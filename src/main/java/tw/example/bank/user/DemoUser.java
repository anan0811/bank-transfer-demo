package tw.example.bank.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "demo_users")
public class DemoUser {
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    @Id
    @Column(length = 3)
    private String loginId;

    @Column(nullable = false, length = 40)
    private String displayName;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    // Nullable in the schema so an older database can gain this column; DemoDataInitializer fills it in.
    @Column(length = 10)
    private String role;

    protected DemoUser() {}

    public DemoUser(String loginId, String displayName, String passwordHash, String role) {
        this.loginId = loginId;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getLoginId() { return loginId; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role == null ? ROLE_USER : role; }

    public void rename(String newDisplayName, String newPasswordHash) {
        displayName = newDisplayName;
        passwordHash = newPasswordHash;
    }
}
