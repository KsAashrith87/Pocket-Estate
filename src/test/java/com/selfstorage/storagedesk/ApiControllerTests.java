package com.selfstorage.storagedesk;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import jakarta.servlet.http.Cookie;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private ClientRepository clients;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        users.save(new AppUser("demo", "demo@example.com", "Demo", "User", null, "encoded", "MANAGER"));
    }

    @AfterEach
    void cleanUp() {
        clients.deleteAll();
        users.deleteAll();
    }

    @Test
    @WithMockUser(username = "demo")
    void dashboardApiReturnsTheSignedInUsersPortfolio() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.displayName").value("Demo User"))
                .andExpect(jsonPath("$.clients").isArray())
                .andExpect(jsonPath("$.notifications").isArray());
    }

    @Test
    void csrfEndpointSetsAngularCompatibleCookieAndUnauthenticatedApiUses401() throws Exception {
        var csrfResponse = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn();
        Cookie csrfCookie = csrfResponse.getResponse().getCookie("XSRF-TOKEN");
        mockMvc.perform(post("/api/auth/signup")
                        .cookie(csrfCookie)
                        .header("X-CSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Asha","lastName":"Ray","username":"xsrf","email":"xsrf@example.com","password":"safe-password-123"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "demo")
    void clientsApiCreatesOwnerScopedClient() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .with(csrfCookieAndHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Maya Chen","email":"maya@example.com","phone":"555-0100","notes":"New tenant"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maya Chen"))
                .andExpect(jsonPath("$.email").value("maya@example.com"));

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Maya Chen"));
    }

    @Test
    void accountCanRegisterSignInAndReadSession() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrfCookieAndHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Asha","lastName":"Ray","username":"asha","email":"asha@example.com","password":"safe-password-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Asha Ray"));

        var login = mockMvc.perform(post("/api/auth/login")
                        .with(csrfCookieAndHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"asha","password":"safe-password-123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("asha"))
                .andExpect(jsonPath("$.displayName").value("Asha Ray"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor csrfCookieAndHeader()
            throws Exception {
        Cookie csrfCookie = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");
        if (csrfCookie == null) {
            throw new IllegalStateException("CSRF endpoint did not return the XSRF-TOKEN cookie.");
        }
        return request -> {
            request.setCookies(csrfCookie);
            request.addHeader("X-CSRF-TOKEN", csrfCookie.getValue());
            return request;
        };
    }
}
