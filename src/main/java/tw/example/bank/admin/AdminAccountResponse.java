package tw.example.bank.admin;

import java.math.BigDecimal;

public record AdminAccountResponse(Long id, String ownerId, String ownerName, BigDecimal balance, String currency) {}
