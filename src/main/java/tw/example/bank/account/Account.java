package tw.example.bank.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String ownerUsername;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    protected Account() {}

    public Account(String ownerUsername, BigDecimal balance) {
        this.ownerUsername = ownerUsername;
        this.balance = balance;
    }

    public Long getId() { return id; }
    public String getOwnerUsername() { return ownerUsername; }
    public BigDecimal getBalance() { return balance; }

    public void assignOwner(String loginId) { ownerUsername = loginId; }
    public void debit(BigDecimal amount) { balance = balance.subtract(amount); }
    public void credit(BigDecimal amount) { balance = balance.add(amount); }
}
