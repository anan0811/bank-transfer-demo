package tw.example.bank.transfer;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(Long id, String requestId, Long fromAccountId, Long toAccountId,
                               BigDecimal amount, String currency, Instant createdAt) {
    public static TransferResponse from(TransferRecord record) {
        return new TransferResponse(record.getId(), record.getRequestId(), record.getSourceAccountId(),
                record.getTargetAccountId(), record.getAmount(), "TWD", record.getCreatedAt());
    }
}
