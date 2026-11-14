package tw.example.bank.admin;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.example.bank.account.AccountRepository;
import tw.example.bank.transfer.TransferRepository;
import tw.example.bank.transfer.TransferResponse;
import tw.example.bank.user.DemoUser;
import tw.example.bank.user.DemoUserRepository;

/** Read-only views for the administrator. Nothing here changes balances or records. */
@Service
public class AdminService {
    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final DemoUserRepository users;

    public AdminService(AccountRepository accounts, TransferRepository transfers, DemoUserRepository users) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<AdminAccountResponse> allAccounts() {
        Map<String, String> names = users.findAll().stream()
                .collect(Collectors.toMap(DemoUser::getLoginId, DemoUser::getDisplayName, (a, b) -> a));
        return accounts.findAll(Sort.by("id")).stream()
                .map(account -> new AdminAccountResponse(account.getId(), account.getOwnerUsername(),
                        names.getOrDefault(account.getOwnerUsername(), ""), account.getBalance(), "TWD"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> allTransfers() {
        return transfers.findAllByOrderByCreatedAtDescIdDesc().stream().map(TransferResponse::from).toList();
    }
}
