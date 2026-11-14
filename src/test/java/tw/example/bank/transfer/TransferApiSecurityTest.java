package tw.example.bank.transfer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tw.example.bank.account.AccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferApiSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;

    @Test
    void staleBrowserCredentialsDoNotBlockPublicPages() throws Exception {
        // A browser may keep sending an old password; public pages must still load.
        mvc.perform(get("/api/demo/accounts").with(httpBasic("001", "old-password_123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(100)));
        mvc.perform(get("/index.html").with(httpBasic("001", "old-password_123")))
                .andExpect(status().isOk());
    }

    @Test
    void failedLoginFromWebPageDoesNotTriggerBrowserPasswordDialog() throws Exception {
        mvc.perform(get("/api/me").with(httpBasic("001", "wrong_123"))
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
        // Command-line clients still receive the standard Basic challenge.
        mvc.perform(get("/api/me").with(httpBasic("001", "wrong_123")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
    }

    @Test
    void homePageOpensWithoutLogin() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/app.css")))
                // Demo credentials live in the project's Markdown file, not on the page.
                .andExpect(content().string(not(containsString("accounts.html"))))
                .andExpect(content().string(not(containsString("admin_000"))));
        mvc.perform(get("/app.css")).andExpect(status().isOk());
    }

    @Test
    void removedDemoAccountPageIsNotServed() throws Exception {
        mvc.perform(get("/accounts.html")).andExpect(status().is4xxClientError());
    }

    @Test
    void anonymousUserCannotSeeAccounts() throws Exception {
        mvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotSeeAnotherUsersHistory() throws Exception {
        Long bobsAccountId = accounts.findByOwnerUsernameOrderById("002").get(0).getId();
        mvc.perform(get("/api/accounts/{id}/transfers", bobsAccountId).with(user("001")))
                .andExpect(status().isForbidden());
    }
}
