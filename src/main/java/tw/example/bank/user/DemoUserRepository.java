package tw.example.bank.user;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DemoUserRepository extends JpaRepository<DemoUser, String> {
    List<DemoUser> findByRoleOrderByLoginIdAsc(String role);

    long countByRole(String role);

    /** Users created before roles existed are ordinary customers. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update DemoUser u set u.role = 'USER' where u.role is null")
    int assignDefaultRole();
}
