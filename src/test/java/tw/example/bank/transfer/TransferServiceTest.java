package tw.example.bank.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tw.example.bank.account.Account;
import tw.example.bank.account.AccountRepository;
import tw.example.bank.api.ApiException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransferServiceTest {
    @Autowired TransferService service;
    @Autowired AccountRepository accounts;
    @Autowired TransferRepository transfers;

    @Test
    void successfulTransferMovesMoneyAndCreatesRecord() {
        Account alice = account("001");
        Account bob = account("002");
        BigDecimal totalBefore = alice.getBalance().add(bob.getBalance());

        TransferResponse result = service.transfer(
                new TransferRequest(alice.getId(), bob.getId(), new BigDecimal("1200.00"), "success-1"), "001");

        assertThat(result.amount()).isEqualByComparingTo("1200.00");
        assertThat(alice.getBalance()).isEqualByComparingTo("8800.00");
        assertThat(bob.getBalance()).isEqualByComparingTo("11200.00");
        assertThat(alice.getBalance().add(bob.getBalance())).isEqualByComparingTo(totalBefore);
        assertThat(transfers.existsByRequestId("success-1")).isTrue();
    }

    @Test
    void insufficientBalanceChangesNothing() {
        Account alice = account("001");
        Account bob = account("002");
        BigDecimal aliceBefore = alice.getBalance();
        BigDecimal bobBefore = bob.getBalance();
        long countBefore = transfers.count();

        assertThatThrownBy(() -> service.transfer(new TransferRequest(
                alice.getId(), bob.getId(), new BigDecimal("10000.01"), "insufficient-1"), "001"))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getCode()).isEqualTo("INSUFFICIENT_FUNDS"));

        assertThat(alice.getBalance()).isEqualByComparingTo(aliceBefore);
        assertThat(bob.getBalance()).isEqualByComparingTo(bobBefore);
        assertThat(transfers.count()).isEqualTo(countBefore);
    }

    @Test
    void reusedRequestIdCannotChargeTwice() {
        Account alice = account("001");
        Account bob = account("002");
        TransferRequest request = new TransferRequest(alice.getId(), bob.getId(),
                new BigDecimal("100.00"), "duplicate-1");
        service.transfer(request, "001");

        assertThatThrownBy(() -> service.transfer(request, "001"))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getCode()).isEqualTo("DUPLICATE_REQUEST"));
        assertThat(alice.getBalance()).isEqualByComparingTo("9900.00");
        assertThat(bob.getBalance()).isEqualByComparingTo("10100.00");
        assertThat(transfers.findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(
                alice.getId(), alice.getId())).hasSize(1);
    }

    @Test
    void otherUsersAccountCannotBeDebitedOrViewed() {
        Account alice = account("001");
        Account bob = account("002");

        assertThatThrownBy(() -> service.transfer(new TransferRequest(
                bob.getId(), alice.getId(), new BigDecimal("100.00"), "forbidden-1"), "001"))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.historyFor(bob.getId(), "001"))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private Account account(String username) {
        return accounts.findByOwnerUsernameOrderById(username).get(0);
    }
}
