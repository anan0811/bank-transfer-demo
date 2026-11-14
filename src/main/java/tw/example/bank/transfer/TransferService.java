package tw.example.bank.transfer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.example.bank.account.Account;
import tw.example.bank.account.AccountRepository;
import tw.example.bank.account.AccountResponse;
import tw.example.bank.api.ApiException;

@Service
public class TransferService {
    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    public TransferService(AccountRepository accountRepository, TransferRepository transferRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> accountsFor(String username) {
        return accountRepository.findByOwnerUsernameOrderById(username).stream()
                .map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> historyFor(Long accountId, String username) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found."));
        if (!account.getOwnerUsername().equals(username)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_ACCOUNT_OWNER", "This account is not yours.");
        }
        return transferRepository.findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(accountId, accountId)
                .stream().map(TransferResponse::from).toList();
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request, String username) {
        Long fromId = request.fromAccountId();
        Long toId = request.toAccountId();
        BigDecimal amount = request.amount();
        if (fromId == null || toId == null || amount == null || request.requestId() == null
                || request.requestId().isBlank() || request.requestId().length() > 80) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Required fields are missing or invalid.");
        }
        if (fromId.equals(toId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SAME_ACCOUNT", "Source and target accounts must differ.");
        }
        if (amount.signum() <= 0 || amount.scale() > 2 || amount.precision() - amount.scale() > 17) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be positive with at most two decimals.");
        }
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        if (transferRepository.existsByRequestId(request.requestId())) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_REQUEST", "Request ID was already used.");
        }

        // Lock in a fixed order to reduce deadlocks when transfers run concurrently.
        Long firstId = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);
        Account first = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found."));
        Account second = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found."));
        Account source = first.getId().equals(fromId) ? first : second;
        Account target = first.getId().equals(toId) ? first : second;

        if (!source.getOwnerUsername().equals(username)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_ACCOUNT_OWNER", "This account is not yours.");
        }
        if (source.getBalance().compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", "Insufficient balance.");
        }

        source.debit(amount);
        target.credit(amount);
        TransferRecord record = transferRepository.saveAndFlush(
                new TransferRecord(request.requestId(), fromId, toId, amount));
        return TransferResponse.from(record);
    }
}
