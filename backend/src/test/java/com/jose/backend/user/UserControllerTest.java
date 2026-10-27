package com.jose.backend.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    private static final String PASSWORD = "secret-password";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    private User createUser(String email, Role role) {
        User user = new User();
        user.setName(email.substring(0, email.indexOf('@')));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setEmailVerified(true);
        return users.save(user);
    }

    private String tokenFor(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return "Bearer " + JsonPath.<String>read(response, "$.accessToken");
    }

    @Test
    void adminCanListUsers() throws Exception {
        createUser("admin.list@example.com", Role.ADMIN);

        mockMvc.perform(get("/api/users").header("Authorization", tokenFor("admin.list@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.content[?(@.email=='admin.list@example.com')]").exists());
    }

    @Test
    void regularUsersCannotListUsers() throws Exception {
        createUser("plain.user@example.com", Role.USER);

        mockMvc.perform(get("/api/users").header("Authorization", tokenFor("plain.user@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listingRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void agentsSeeOnlyAssignableUsers() throws Exception {
        createUser("agent.one@example.com", Role.AGENT);
        createUser("regular.one@example.com", Role.USER);

        mockMvc.perform(get("/api/users/assignable")
                        .header("Authorization", tokenFor("agent.one@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='agent.one')]").exists())
                .andExpect(jsonPath("$[?(@.name=='regular.one')]").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    void regularUsersCannotSeeAssignableUsers() throws Exception {
        createUser("regular.two@example.com", Role.USER);

        mockMvc.perform(get("/api/users/assignable")
                        .header("Authorization", tokenFor("regular.two@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanChangeARole() throws Exception {
        createUser("admin.roles@example.com", Role.ADMIN);
        User target = createUser("target.user@example.com", Role.USER);

        mockMvc.perform(patch("/api/users/" + target.getId() + "/role")
                        .header("Authorization", tokenFor("admin.roles@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"AGENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("AGENT"));
    }

    @Test
    void aRoleChangeAppliesToTokensAlreadyIssued() throws Exception {
        createUser("admin.promote@example.com", Role.ADMIN);
        User target = createUser("soon.agent@example.com", Role.USER);
        String targetToken = tokenFor("soon.agent@example.com");

        mockMvc.perform(get("/api/users/assignable").header("Authorization", targetToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/users/" + target.getId() + "/role")
                        .header("Authorization", tokenFor("admin.promote@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"AGENT\"}"))
                .andExpect(status().isOk());

        // same token, new role
        mockMvc.perform(get("/api/users/assignable").header("Authorization", targetToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotChangeTheirOwnRole() throws Exception {
        User admin = createUser("admin.self@example.com", Role.ADMIN);

        mockMvc.perform(patch("/api/users/" + admin.getId() + "/role")
                        .header("Authorization", tokenFor("admin.self@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUsersCannotChangeRoles() throws Exception {
        User attacker = createUser("attacker@example.com", Role.USER);
        User other = createUser("other.user@example.com", Role.USER);

        mockMvc.perform(patch("/api/users/" + other.getId() + "/role")
                        .header("Authorization", tokenFor("attacker@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        // nobody can promote themselves either
        mockMvc.perform(patch("/api/users/" + attacker.getId() + "/role")
                        .header("Authorization", tokenFor("attacker@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void changingTheRoleOfAnUnknownUserReturns404() throws Exception {
        createUser("admin.missing@example.com", Role.ADMIN);

        mockMvc.perform(patch("/api/users/999999999/role")
                        .header("Authorization", tokenFor("admin.missing@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"AGENT\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidRolesAreRejected() throws Exception {
        User admin = createUser("admin.invalid@example.com", Role.ADMIN);
        User target = createUser("target.invalid@example.com", Role.USER);

        mockMvc.perform(patch("/api/users/" + target.getId() + "/role")
                        .header("Authorization", tokenFor("admin.invalid@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPERUSER\"}"))
                .andExpect(status().isBadRequest());
        // keeps the compiler quiet about the unused variable
        org.assertj.core.api.Assertions.assertThat(admin.getId()).isNotNull();
    }
}
