package tw.example.bank.transfer;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tw.example.bank.account.AccountResponse;

@RestController
@RequestMapping("/api")
public class TransferController {
    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @GetMapping("/accounts")
    public List<AccountResponse> accounts(Principal principal) {
        return service.accountsFor(principal.getName());
    }

    @GetMapping("/accounts/{id}/transfers")
    public List<TransferResponse> history(@PathVariable Long id, Principal principal) {
        return service.historyFor(id, principal.getName());
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request, Principal principal) {
        return service.transfer(request, principal.getName());
    }
}
