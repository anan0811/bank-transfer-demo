package tw.example.bank.transfer;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<TransferRecord, Long> {
    boolean existsByRequestId(String requestId);
    List<TransferRecord> findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
            Long sourceAccountId, Long targetAccountId);
    List<TransferRecord> findAllByOrderByCreatedAtDescIdDesc();
}
