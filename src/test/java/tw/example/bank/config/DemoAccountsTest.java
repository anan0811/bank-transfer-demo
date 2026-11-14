package tw.example.bank.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tw.example.bank.account.Account;
import tw.example.bank.account.AccountRepository;
import tw.example.bank.user.DemoUser;
import tw.example.bank.user.DemoUserRepository;

@SpringBootTest
@ActiveProfiles("test")
class DemoAccountsTest {
    @Autowired DemoUserRepository users;
    @Autowired AccountRepository accounts;
    @Autowired PasswordEncoder passwords;
    @Autowired DemoDataInitializer initializer;

    @Test
    void createsOneHundredIdsWithNamesRepeatedAtMostThreeTimes() {
        var all = users.findByRoleOrderByLoginIdAsc(DemoUser.ROLE_USER);
        assertThat(all).hasSize(100);
        assertThat(accounts.count()).isEqualTo(100);
        assertThat(all.get(0).getLoginId()).isEqualTo("001");
        assertThat(all.get(99).getLoginId()).isEqualTo("100");
        Map<String, Long> nameCounts = all.stream()
                .collect(Collectors.groupingBy(DemoUser::getDisplayName, Collectors.counting()));
        assertThat(nameCounts.values()).allSatisfy(count -> assertThat(count).isLessThanOrEqualTo(3));
        assertThat(nameCounts.values()).anySatisfy(count -> assertThat(count).isGreaterThan(1));
        assertThat(passwords.matches(all.get(0).getDisplayName() + "_123", all.get(0).getPasswordHash())).isTrue();
        assertThat(all).allSatisfy(user ->
                assertThat(DemoDataInitializer.isEnglishName(user.getDisplayName())).isTrue());
    }

    @Test
    void namesAreFixedSoEveryCloneGetsTheSameLogins() {
        var names = users.findByRoleOrderByLoginIdAsc(DemoUser.ROLE_USER).stream()
                .map(DemoUser::getDisplayName).toList();
        assertThat(names).isEqualTo(DemoNames.CUSTOMERS);
        assertThat(names.get(0)).isEqualTo("Brian Kuo");
        assertThat(names.get(99)).isEqualTo("Annie Huang");
    }

    @Test
    void credentialsDocumentMatchesFixedNames() throws Exception {
        // "示範帳戶與登入說明.md" must list exactly the names the application creates.
        String doc = Files.readString(Path.of("示範帳戶與登入說明.md"), StandardCharsets.UTF_8);
        Matcher row = Pattern.compile("(?m)^\\| (\\d{3}) \\| ([^|]+?) \\| `([^`]+)` \\|$").matcher(doc);
        List<String> documented = new ArrayList<>();
        while (row.find()) {
            assertThat(row.group(1)).isEqualTo(String.format("%03d", documented.size() + 1));
            assertThat(row.group(3)).isEqualTo(row.group(2) + "_123");
            documented.add(row.group(2));
        }
        assertThat(documented).isEqualTo(DemoNames.CUSTOMERS);
        assertThat(doc).contains("admin_000");
    }

    @Test
    @Transactional // Rolled back so the renamed data does not leak into other tests.
    void outdatedNamesAreResetToFixedNamesWithoutTouchingBalances() {
        DemoUser first = users.findById("001").orElseThrow();
        first.rename("王子晴", passwords.encode("王子晴_123"));
        String secondHashBefore = users.findById("002").orElseThrow().getPasswordHash();
        BigDecimal totalBefore = totalBalance();

        initializer.initialize();

        DemoUser renamed = users.findById("001").orElseThrow();
        assertThat(renamed.getDisplayName()).isEqualTo("Brian Kuo");
        assertThat(passwords.matches("Brian Kuo_123", renamed.getPasswordHash())).isTrue();
        assertThat(passwords.matches("王子晴_123", renamed.getPasswordHash())).isFalse();
        // Customers that already match keep their password hash untouched.
        assertThat(users.findById("002").orElseThrow().getPasswordHash()).isEqualTo(secondHashBefore);
        assertThat(totalBalance()).isEqualByComparingTo(totalBefore);
        assertThat(users.findById("000").orElseThrow().getDisplayName()).isEqualTo("admin");
    }

    private BigDecimal totalBalance() {
        return accounts.findAll().stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void createsAdministratorWithoutBankAccount() {
        DemoUser admin = users.findById("000").orElseThrow();
        assertThat(admin.getDisplayName()).isEqualTo("admin");
        assertThat(admin.getRole()).isEqualTo(DemoUser.ROLE_ADMIN);
        assertThat(passwords.matches("admin_000", admin.getPasswordHash())).isTrue();
        assertThat(users.count()).isEqualTo(101);
        assertThat(accounts.findByOwnerUsernameOrderById("000")).isEmpty();
    }
}
