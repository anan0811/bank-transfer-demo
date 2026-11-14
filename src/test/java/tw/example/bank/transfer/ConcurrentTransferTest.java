package tw.example.bank.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import tw.example.bank.account.Account;
import tw.example.bank.account.AccountRepository;
import tw.example.bank.api.ApiException;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:bankconcurrency;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ConcurrentTransferTest {
    @Autowired TransferService service;
    @Autowired AccountRepository accounts;
    @Autowired TransferRepository transfers;

    @Test
    void simultaneousTransfersCannotOverdrawAccount() throws Exception {
        Long aliceId = account("001").getId();
        Long bobId = account("002").getId();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> first = () -> attempt(start, aliceId, bobId, "concurrent-1");
            Callable<Boolean> second = () -> attempt(start, aliceId, bobId, "concurrent-2");
            Future<Boolean> firstResult = executor.submit(first);
            Future<Boolean> secondResult = executor.submit(second);
            start.countDown();

            assertThat((firstResult.get() ? 1 : 0) + (secondResult.get() ? 1 : 0)).isEqualTo(1);
            assertThat(account("001").getBalance()).isEqualByComparingTo("3000.00");
            assertThat(account("002").getBalance()).isEqualByComparingTo("17000.00");
            assertThat(transfers.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean attempt(CountDownLatch start, Long fromId, Long toId, String requestId)
            throws InterruptedException {
        start.await();
        try {
            service.transfer(new TransferRequest(fromId, toId, new BigDecimal("7000.00"), requestId), "001");
            return true;
        } catch (ApiException error) {
            if (!"INSUFFICIENT_FUNDS".equals(error.getCode())) {
                throw error;
            }
            return false;
        }
    }

    private Account account(String username) {
        return accounts.findByOwnerUsernameOrderById(username).get(0);
    }
}
