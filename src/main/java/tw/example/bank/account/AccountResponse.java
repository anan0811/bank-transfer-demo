package tw.example.bank.account;

import java.math.BigDecimal;

public record AccountResponse(Long id, BigDecimal balance, String currency) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getBalance(), "TWD");
    }
}
