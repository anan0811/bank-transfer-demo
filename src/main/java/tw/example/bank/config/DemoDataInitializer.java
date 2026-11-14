package tw.example.bank.config;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.example.bank.account.Account;
import tw.example.bank.account.AccountRepository;
import tw.example.bank.user.DemoUser;
import tw.example.bank.user.DemoUserRepository;

@Service
public class DemoDataInitializer {
    public static final String ADMIN_LOGIN_ID = "000";
    static final String ADMIN_NAME = "admin";
    static final String ADMIN_PASSWORD = "admin_000"; // Public local-demo credential, like the customer ones.

    // Common Taiwanese surnames in romanized form, paired with English given names, e.g. "Emily Chen".
    private static final String[] SURNAMES = {
            "Chen", "Lin", "Huang", "Chang", "Lee", "Wang", "Wu", "Liu", "Tsai", "Yang",
            "Hsu", "Cheng", "Hsieh", "Kuo", "Hung", "Tseng", "Chiu", "Liao", "Lai", "Chou"
    };
    private static final String[] GIVEN_NAMES = {
            "Emily", "Grace", "Kevin", "Jason", "Amy", "Eric", "Vivian", "Brian", "Joyce", "Ryan",
            "Cindy", "Daniel", "Irene", "Leo", "Sophia", "Andy", "Tina", "Jerry", "Annie", "Henry",
            "Iris", "Alex", "Wendy", "Ethan", "Peggy", "Steven", "Claire", "Ivan", "Sharon", "Oscar"
    };
    private static final Pattern ENGLISH_NAME = Pattern.compile("[A-Za-z]+( [A-Za-z]+)*");

    private final AccountRepository accounts;
    private final DemoUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public DemoDataInitializer(AccountRepository accounts, DemoUserRepository users,
                               PasswordEncoder passwordEncoder, EntityManager entityManager) {
        this.accounts = accounts;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    @Transactional
    public void initialize() {
        users.assignDefaultRole();
        seedCustomers();
        renameNonEnglishCustomers();
        ensureAdmin();
    }

    /**
     * Older databases were seeded with Chinese names. Give every customer a new English name and
     * matching password ("name_123"); IDs, balances and transfer history stay unchanged.
     */
    private void renameNonEnglishCustomers() {
        List<DemoUser> customers = users.findByRoleOrderByLoginIdAsc(DemoUser.ROLE_USER);
        if (customers.stream().allMatch(user -> isEnglishName(user.getDisplayName()))) return;
        List<String> names = generateNames();
        for (int i = 0; i < customers.size(); i++) {
            String name = names.get(i);
            customers.get(i).rename(name, passwordEncoder.encode(name + "_123"));
        }
    }

    public static boolean isEnglishName(String name) {
        return ENGLISH_NAME.matcher(name).matches();
    }

    /** The administrator only reviews data: it owns no bank account and cannot transfer money. */
    private void ensureAdmin() {
        users.findById(ADMIN_LOGIN_ID).ifPresentOrElse(existing -> {
            if (!DemoUser.ROLE_ADMIN.equals(existing.getRole())) {
                throw new IllegalStateException("Login ID 000 is reserved for the administrator.");
            }
        }, () -> users.save(new DemoUser(ADMIN_LOGIN_ID, ADMIN_NAME,
                passwordEncoder.encode(ADMIN_PASSWORD), DemoUser.ROLE_ADMIN)));
    }

    private void seedCustomers() {
        long userCount = users.countByRole(DemoUser.ROLE_USER);
        if (userCount == 100) {
            if (accounts.count() != 100) throw new IllegalStateException("Expected 100 demo accounts.");
            return;
        }
        if (userCount != 0) throw new IllegalStateException("Demo user setup is incomplete.");

        List<Account> existing = accounts.findAll(Sort.by("id"));
        if (existing.size() > 100) throw new IllegalStateException("More than 100 accounts already exist.");
        List<String> names = generateNames();
        for (int number = 1; number <= 100; number++) {
            String loginId = String.format(Locale.ROOT, "%03d", number);
            if (number <= existing.size()) {
                Account account = existing.get(number - 1);
                if (!account.getId().equals((long) number)) {
                    throw new IllegalStateException("Existing account IDs must start at 1 without gaps.");
                }
                account.assignOwner(loginId); // Preserve existing balances and transfer history.
            } else {
                // Insert explicit IDs because an older database may have skipped identity values.
                entityManager.createNativeQuery(
                        "insert into accounts (id, owner_username, balance) values (:id, :owner, :balance)")
                        .setParameter("id", number)
                        .setParameter("owner", loginId)
                        .setParameter("balance", new BigDecimal("10000.00"))
                        .executeUpdate();
            }
            String name = names.get(number - 1);
            users.save(new DemoUser(loginId, name, passwordEncoder.encode(name + "_123"), DemoUser.ROLE_USER));
        }
    }

    private List<String> generateNames() {
        SecureRandom random = new SecureRandom();
        List<String> uniqueNames = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        while (uniqueNames.size() < 70) {
            String name = GIVEN_NAMES[random.nextInt(GIVEN_NAMES.length)]
                    + " " + SURNAMES[random.nextInt(SURNAMES.length)];
            if (seen.add(name)) uniqueNames.add(name);
        }
        List<String> names = new ArrayList<>(uniqueNames);
        Map<String, Integer> counts = new HashMap<>();
        uniqueNames.forEach(name -> counts.put(name, 1));
        while (names.size() < 100) {
            String name = uniqueNames.get(random.nextInt(uniqueNames.size()));
            if (counts.get(name) < 3) {
                names.add(name);
                counts.put(name, counts.get(name) + 1);
            }
        }
        Collections.shuffle(names, random);
        return names;
    }
}
