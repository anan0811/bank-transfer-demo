package tw.example.bank.admin;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminApiTest {
    @Autowired MockMvc mvc;

    @Test
    void adminLogsInWithSeededPasswordAndSeesAllAccounts() throws Exception {
        mvc.perform(get("/api/me").with(httpBasic("000", "admin_000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(get("/api/admin/accounts").with(httpBasic("000", "admin_000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(100)))
                .andExpect(jsonPath("$[0].ownerId").value("001"));
        mvc.perform(get("/api/admin/transfers").with(httpBasic("000", "admin_000")))
                .andExpect(status().isOk());
    }

    @Test
    void wrongAdminPasswordIsRejected() throws Exception {
        mvc.perform(get("/api/admin/accounts").with(httpBasic("000", "admin_123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotUseAdminApi() throws Exception {
        mvc.perform(get("/api/admin/accounts").with(user("001").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/transfers").with(user("001").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminIsReadOnlyAndCannotTransfer() throws Exception {
        String body = """
                {"fromAccountId": 1, "toAccountId": 2, "amount": "10.00", "requestId": "admin-attempt-1"}
                """;
        mvc.perform(post("/api/transfers").with(httpBasic("000", "admin_000"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/accounts").with(httpBasic("000", "admin_000")))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicDirectoryListsCustomersOnly() throws Exception {
        mvc.perform(get("/api/demo/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(100)))
                .andExpect(jsonPath("$[*].id", not(hasItem("000"))));
    }
}
