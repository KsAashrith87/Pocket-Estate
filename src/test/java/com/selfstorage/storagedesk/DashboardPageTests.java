package com.selfstorage.storagedesk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardPageTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository users;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        users.save(new AppUser("demo", "demo@example.com", "Demo", "User", null, "demo12345", "MANAGER"));
    }

    @Test
    @WithMockUser(username = "demo")
    void dashboardShouldNotExposeInlineAssignmentControls() throws Exception {
        String html = mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(html).doesNotContain("inline-status-form");
        assertThat(html).doesNotContain("this.form.requestSubmit()");
    }

    @Test
    @WithMockUser(username = "demo")
    void clientsPageShouldRenderWithoutServerError() throws Exception {
        mockMvc.perform(get("/clients"))
            .andExpect(status().isOk());
    }
}
