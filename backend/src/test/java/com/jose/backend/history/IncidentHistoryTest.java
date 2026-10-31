package com.jose.backend.history;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.jose.backend.user.Role;
import com.jose.backend.user.User;
import com.jose.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentHistoryTest {

    private static final String PASSWORD = "secret-password";
    private static final String SAME_CONTENT =
            "\"description\":\"Something is broken\",\"category\":\"Network\"";

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

    private long createIncident(String token, String title) throws Exception {
        String response = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"," + SAME_CONTENT + "}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.<Number>read(response, "$.id").longValue();
    }

    private ResultActions update(String token, long id, String json) throws Exception {
        return mockMvc.perform(put("/api/incidents/" + id)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions send(String token, long id, String path, String json) throws Exception {
        return mockMvc.perform(patch("/api/incidents/" + id + "/" + path)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions history(String token, long id) throws Exception {
        return mockMvc.perform(get("/api/incidents/" + id + "/history").header("Authorization", token));
    }

    @Test
    void everyChangeIsRecordedInOrder() throws Exception {
        User agentUser = createUser("agent.history@example.com", Role.AGENT);
        createUser("owner.history@example.com", Role.USER);
        String owner = tokenFor("owner.history@example.com");
        String agent = tokenFor("agent.history@example.com");
        long id = createIncident(owner, "History test");

        update(agent, id, "{\"title\":\"History test\"," + SAME_CONTENT + ",\"priority\":\"HIGH\"}")
                .andExpect(status().isOk());
        send(agent, id, "status", "{\"status\":\"IN_PROGRESS\"}").andExpect(status().isOk());
        send(agent, id, "assignee", "{\"assigneeId\":" + agentUser.getId() + "}")
                .andExpect(status().isOk());
        update(agent, id, "{\"title\":\"Renamed\"," + SAME_CONTENT + "}").andExpect(status().isOk());

        history(owner, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].field").value("CREATED"))
                .andExpect(jsonPath("$[0].actor.name").value("owner.history"))
                .andExpect(jsonPath("$[1].field").value("PRIORITY"))
                .andExpect(jsonPath("$[1].oldValue").value("MEDIUM"))
                .andExpect(jsonPath("$[1].newValue").value("HIGH"))
                .andExpect(jsonPath("$[1].actor.name").value("agent.history"))
                .andExpect(jsonPath("$[2].field").value("STATUS"))
                .andExpect(jsonPath("$[2].oldValue").value("OPEN"))
                .andExpect(jsonPath("$[2].newValue").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[3].field").value("ASSIGNEE"))
                .andExpect(jsonPath("$[3].oldValue").doesNotExist())
                .andExpect(jsonPath("$[3].newValue").value("agent.history"))
                .andExpect(jsonPath("$[4].field").value("TITLE"))
                .andExpect(jsonPath("$[4].oldValue").value("History test"))
                .andExpect(jsonPath("$[4].newValue").value("Renamed"));
    }

    @Test
    void savingWithoutChangesAddsNothing() throws Exception {
        createUser("agent.nochange@example.com", Role.AGENT);
        String agent = tokenFor("agent.nochange@example.com");
        long id = createIncident(agent, "No changes");

        update(agent, id, "{\"title\":\"No changes\"," + SAME_CONTENT + "}").andExpect(status().isOk());

        history(agent, id).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void descriptionChangesAreRecordedWithoutTheText() throws Exception {
        createUser("owner.description@example.com", Role.USER);
        String owner = tokenFor("owner.description@example.com");
        long id = createIncident(owner, "Description");

        update(owner, id, "{\"title\":\"Description\",\"description\":\"A new text\",\"category\":\"Network\"}")
                .andExpect(status().isOk());

        history(owner, id)
                .andExpect(jsonPath("$[1].field").value("DESCRIPTION"))
                .andExpect(jsonPath("$[1].oldValue").doesNotExist())
                .andExpect(jsonPath("$[1].newValue").doesNotExist());
    }

    @Test
    void unassigningIsRecorded() throws Exception {
        User agentUser = createUser("agent.unassign@example.com", Role.AGENT);
        String agent = tokenFor("agent.unassign@example.com");
        long id = createIncident(agent, "Unassign");

        send(agent, id, "assignee", "{\"assigneeId\":" + agentUser.getId() + "}").andExpect(status().isOk());
        send(agent, id, "assignee", "{\"assigneeId\":null}").andExpect(status().isOk());

        history(agent, id)
                .andExpect(jsonPath("$[2].field").value("ASSIGNEE"))
                .andExpect(jsonPath("$[2].oldValue").value("agent.unassign"))
                .andExpect(jsonPath("$[2].newValue").doesNotExist());
    }

    @Test
    void historyIsOnlyVisibleToWhoCanSeeTheIncident() throws Exception {
        createUser("owner.private@example.com", Role.USER);
        createUser("other.private@example.com", Role.USER);
        createUser("agent.private@example.com", Role.AGENT);
        long id = createIncident(tokenFor("owner.private@example.com"), "Private");

        history(tokenFor("other.private@example.com"), id).andExpect(status().isNotFound());
        history(tokenFor("agent.private@example.com"), id).andExpect(status().isOk());
        mockMvc.perform(get("/api/incidents/" + id + "/history")).andExpect(status().isUnauthorized());
    }
}
