package tw.example.bank.admin;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tw.example.bank.transfer.TransferResponse;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/accounts")
    public List<AdminAccountResponse> accounts() {
        return service.allAccounts();
    }

    @GetMapping("/transfers")
    public List<TransferResponse> transfers() {
        return service.allTransfers();
    }
}
