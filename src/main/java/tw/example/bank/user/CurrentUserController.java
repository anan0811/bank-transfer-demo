package tw.example.bank.user;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tw.example.bank.api.ApiException;

@RestController
public class CurrentUserController {
    private final DemoUserRepository users;

    public CurrentUserController(DemoUserRepository users) {
        this.users = users;
    }

    /** Lets the web page decide whether to show the customer or the administrator view. */
    @GetMapping("/api/me")
    public CurrentUserResponse me(Principal principal) {
        return users.findById(principal.getName()).map(CurrentUserResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
    }
}
