# 模擬銀行轉帳系統

以 Java、Spring Boot、Spring Data JPA 和 H2 製作的金融資訊 side project。提供本機操作頁面與 REST API。僅使用模擬帳戶與資料，**不連接真實銀行或執行真實轉帳**。

## 功能

- HTTP Basic 登入；使用者只能查詢自己的帳戶與交易紀錄，也只能從自己的帳戶轉出。
- 同一筆轉帳在資料庫交易中完成扣款、入款及交易紀錄寫入。
- 使用 `BigDecimal` 儲存金額；檢查餘額不足、無效金額、同帳戶轉帳。
- 依固定帳戶 ID 順序取得資料庫寫入鎖；唯一 `requestId` 防止重複扣款。
- 首次啟動建立 `001`～`100` 共 100 個模擬帳戶。姓名為隨機英文姓名（英文名＋台灣常見姓氏拼音，例如 `Emily Chen`），可重複，但同名最多 3 個；建立後持續保存在 H2 檔案資料庫。
- 登入帳號是三位數帳戶 ID，密碼是該帳戶的「姓名_123」，含空格與大小寫，例如 `Emily Chen_123`。密碼以 BCrypt 雜湊儲存。
- 若舊資料庫裡仍是中文姓名，啟動時會自動把 100 位使用者改成英文姓名並重設對應密碼；帳戶 ID、餘額與交易紀錄不變。
- 若從原本兩個帳戶的資料庫升級，保留其餘額與交易紀錄，並補足其他 98 個帳戶。
- 管理者帳戶：ID `000`、名稱 `admin`、密碼 `admin_000`。管理者**只能查看**全部 100 個帳戶的餘額與全部交易紀錄，沒有銀行帳戶、不能轉帳（職責分離）；一般使用者則無法呼叫管理者 API。管理者不會出現在公開的示範帳戶名冊。

## 環境與執行

需要 JDK 17 以上。在專案根目錄執行；Maven Wrapper 會自動下載專案指定的 Maven 版本：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.2' # 本機已安裝的 JDK；其他電腦請改為自己的 JDK 路徑
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

看到終端機顯示 `Started BankTransferApplication` 後，在瀏覽器開啟 **`http://127.0.0.1:8080/`**。網頁只放登入表單；登入規則、管理者帳密，以及 100 個帳戶的 ID、姓名與示範密碼寫在專案根目錄的 [`示範帳戶與登入說明.md`](示範帳戶與登入說明.md)。這份是匯出當下的快照，若重建資料庫，姓名會重新隨機產生，最新名冊可用下方的 `GET /api/demo/accounts` 查詢。服務僅監聽本機。這些是公開的本機示範帳密，請勿用於部署或真實資料。若要重設所有模擬資料，先備份並停止服務，再刪除 `bankdemo.mv.db`。

## 試用 API

未登入也能查詢示範帳戶名冊：

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/demo/accounts
```

以下範例自動取得 `001` 的姓名與密碼，並查詢該帳戶；轉帳也可直接在網站操作：

```powershell
$first = (Invoke-RestMethod http://127.0.0.1:8080/api/demo/accounts)[0]
$credential = "$($first.id):$($first.name)_123"
$encoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($credential))
$headers = @{ Authorization = "Basic $encoded" }
Invoke-RestMethod http://127.0.0.1:8080/api/accounts -Headers $headers
```

使用查到的數字帳戶 ID 轉帳，例如從 `001` 轉至 `002`：

```powershell
$body = @{ fromAccountId = 1; toAccountId = 2; amount = '12.00'; requestId = [guid]::NewGuid().ToString() } | ConvertTo-Json
Invoke-RestMethod http://127.0.0.1:8080/api/transfers -Method Post -Headers $headers -ContentType 'application/json' -Body $body
```

再次送出相同 `requestId`，API 會回傳 `409 DUPLICATE_REQUEST`，不會再次扣款。

以管理者身分查詢全部帳戶與交易紀錄（網頁上用 `000` / `admin_000` 登入會直接顯示管理者檢視）：

```powershell
$admin = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('000:admin_000')) }
Invoke-RestMethod http://127.0.0.1:8080/api/me -Headers $admin
Invoke-RestMethod http://127.0.0.1:8080/api/admin/accounts -Headers $admin
Invoke-RestMethod http://127.0.0.1:8080/api/admin/transfers -Headers $admin
```

| API | 權限 | 說明 |
|---|---|---|
| `GET /api/demo/accounts` | 不需登入 | 100 個示範帳戶名冊（不含管理者） |
| `GET /api/me` | 已登入 | 目前登入者的 ID、名稱與角色 |
| `GET /api/accounts`、`GET /api/accounts/{id}/transfers` | 一般使用者 | 自己的帳戶與交易紀錄 |
| `POST /api/transfers` | 一般使用者 | 從自己的帳戶轉出 |
| `GET /api/admin/accounts`、`GET /api/admin/transfers` | 管理者 | 全部帳戶、全部交易紀錄（唯讀） |

## 設計重點

`demo_users` 儲存三位數登入 ID、姓名、密碼雜湊與角色（`USER` 或 `ADMIN`）；舊資料庫升級時，既有使用者會自動補上 `USER` 角色，並新增管理者 `000`。權限在 `SecurityConfig` 依角色區分：轉帳相關 API 只限 `USER`，`/api/admin/**` 只限 `ADMIN`。首頁、樣式檔與示範名冊 API 使用另一條不做 HTTP Basic 驗證的 filter chain，因此瀏覽器殘留舊帳密時也不會被擋下；網頁送出的請求帶有 `X-Requested-With: XMLHttpRequest`，登入失敗時只回 401、不送 `WWW-Authenticate`，避免瀏覽器跳出內建帳密視窗並記住錯誤帳密。`accounts` 儲存帳戶餘額；`transfer_records` 儲存成功轉帳紀錄。核心邏輯位於 `TransferService`。`@Transactional` 使扣款、入款與紀錄寫入同時成功或一起回復。`AccountRepository.findByIdForUpdate` 使用悲觀寫入鎖，並依 ID 順序鎖定兩筆帳戶，降低同時轉帳造成的競爭問題。`requestId` 有資料庫唯一限制；重複請求會被拒絕。

本專案使用 H2 與示範帳密，適合本機展示。若要作為正式服務，仍須改用正式資料庫、身分管理、憑證保護、資料庫遷移與更完整的稽核設計。

## 可展示的測試

`.\mvnw.cmd test` 共 19 項測試，驗證首頁與樣式檔不需登入即可開啟、首頁不顯示示範帳密、瀏覽器殘留舊密碼時公開頁面仍可開啟、網頁登入失敗不會跳出瀏覽器內建的帳密視窗、所有姓名皆為英文、舊中文姓名資料庫會自動更名且餘額不變、轉帳後總金額不變、餘額不足時資料不變、重複請求不會再次扣款、兩筆同時轉帳不會超扣、未授權使用者無法查看他人紀錄，以及管理者能以 `admin_000` 登入並查看全部資料、但不能轉帳，一般使用者也無法使用管理者 API。
