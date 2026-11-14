package tw.example.bank.user;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoDirectoryController {
    private final DemoUserRepository users;

    public DemoDirectoryController(DemoUserRepository users) {
        this.users = users;
    }

    @GetMapping("/accounts")
    public List<DemoAccountResponse> accounts() {
        return users.findByRoleOrderByLoginIdAsc(DemoUser.ROLE_USER).stream().map(DemoAccountResponse::from).toList();
    }
}
