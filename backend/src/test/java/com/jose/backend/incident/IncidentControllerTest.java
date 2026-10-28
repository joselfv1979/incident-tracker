package com.jose.backend.incident;

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
class IncidentControllerTest {

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

    private long createIncident(String token, String title) throws Exception {
        String response = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"Something is broken","category":"Network"}"""
                                .formatted(title)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.<Number>read(response, "$.id").longValue();
    }

    private ResultActions changeStatus(String token, long id, String newStatus) throws Exception {
        return mockMvc.perform(patch("/api/incidents/" + id + "/status")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + newStatus + "\"}"));
    }

    private ResultActions assign(String token, long id, String assigneeJson) throws Exception {
        return mockMvc.perform(patch("/api/incidents/" + id + "/assignee")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assigneeId\":" + assigneeJson + "}"));
    }

    private ResultActions update(String token, long id, String json) throws Exception {
        return mockMvc.perform(put("/api/incidents/" + id)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void anAuthenticatedUserCanReportAnIncident() throws Exception {
        User reporter = createUser("reporter.create@example.com", Role.USER);

        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", tokenFor("reporter.create@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  Printer on fire  \",\"description\":\"Smoke\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Printer on fire"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.reporter.id").value(reporter.getId()))
                .andExpect(jsonPath("$.reporter.email").doesNotExist())
                .andExpect(jsonPath("$.assignee").doesNotExist());
    }

    @Test
    void creationRequiresAuthenticationAndValidData() throws Exception {
        createUser("reporter.invalid@example.com", Role.USER);

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"description\":\"y\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", tokenFor("reporter.invalid@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"description\":\"y\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usersOnlySeeTheirOwnIncidents() throws Exception {
        createUser("owner.one@example.com", Role.USER);
        createUser("owner.two@example.com", Role.USER);
        String tokenOne = tokenFor("owner.one@example.com");
        String tokenTwo = tokenFor("owner.two@example.com");
        createIncident(tokenOne, "First user problem");
        long otherId = createIncident(tokenTwo, "Second user problem");

        mockMvc.perform(get("/api/incidents").header("Authorization", tokenOne))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("First user problem"));

        // somebody else's incident looks like it does not exist
        mockMvc.perform(get("/api/incidents/" + otherId).header("Authorization", tokenOne))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffSeeEveryIncident() throws Exception {
        createUser("owner.staff@example.com", Role.USER);
        createUser("agent.staff@example.com", Role.AGENT);
        long id = createIncident(tokenFor("owner.staff@example.com"), "Visible for staff zq9visible");

        mockMvc.perform(get("/api/incidents/" + id)
                        .header("Authorization", tokenFor("agent.staff@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Visible for staff zq9visible"));

        mockMvc.perform(get("/api/incidents")
                        .param("q", "zq9visible")
                        .header("Authorization", tokenFor("agent.staff@example.com")))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listingSupportsFiltersTextSearchAndSorting() throws Exception {
        createUser("agent.filters@example.com", Role.AGENT);
        createUser("owner.filters@example.com", Role.USER);
        String agent = tokenFor("agent.filters@example.com");
        String owner = tokenFor("owner.filters@example.com");
        long first = createIncident(owner, "Alpha zq9filters");
        createIncident(owner, "Beta zq9filters");

        update(agent, first, """
                {"title":"Alpha zq9filters","description":"Something is broken","category":"Network","priority":"HIGH"}""")
                .andExpect(status().isOk());
        changeStatus(agent, first, "IN_PROGRESS").andExpect(status().isOk());

        mockMvc.perform(get("/api/incidents").param("q", "zq9filters").header("Authorization", agent))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/incidents")
                        .param("q", "zq9filters")
                        .param("status", "IN_PROGRESS")
                        .header("Authorization", agent))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Alpha zq9filters"));
        mockMvc.perform(get("/api/incidents")
                        .param("q", "zq9filters")
                        .param("priority", "HIGH")
                        .header("Authorization", agent))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/incidents")
                        .param("q", "zq9filters")
                        .param("category", "network")
                        .param("sortBy", "title")
                        .param("direction", "asc")
                        .header("Authorization", agent))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Alpha zq9filters"));
        // wildcard characters in the text are not wildcards
        mockMvc.perform(get("/api/incidents").param("q", "%").header("Authorization", agent))
                .andExpect(status().isOk());
    }

    @Test
    void unsupportedSortingIsRejected() throws Exception {
        createUser("agent.sort@example.com", Role.AGENT);
        String agent = tokenFor("agent.sort@example.com");

        mockMvc.perform(get("/api/incidents").param("sortBy", "password").header("Authorization", agent))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/incidents").param("direction", "sideways").header("Authorization", agent))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportersCanEditWhileOpenButNeverThePriority() throws Exception {
        createUser("owner.edit@example.com", Role.USER);
        createUser("agent.edit@example.com", Role.AGENT);
        String owner = tokenFor("owner.edit@example.com");
        String agent = tokenFor("agent.edit@example.com");
        long id = createIncident(owner, "Original title");

        update(owner, id, "{\"title\":\"Better title\",\"description\":\"More details\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Better title"));

        update(owner, id, "{\"title\":\"Urgent\",\"description\":\"x\",\"priority\":\"CRITICAL\"}")
                .andExpect(status().isForbidden());

        changeStatus(agent, id, "IN_PROGRESS").andExpect(status().isOk());

        update(owner, id, "{\"title\":\"Too late\",\"description\":\"x\"}")
                .andExpect(status().isForbidden());

        update(agent, id, "{\"title\":\"Triaged\",\"description\":\"x\",\"priority\":\"CRITICAL\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("CRITICAL"));
    }

    @Test
    void statusFollowsTheWorkflowAndTracksResolution() throws Exception {
        createUser("owner.flow@example.com", Role.USER);
        createUser("agent.flow@example.com", Role.AGENT);
        String owner = tokenFor("owner.flow@example.com");
        String agent = tokenFor("agent.flow@example.com");
        long id = createIncident(owner, "Workflow");

        changeStatus(agent, id, "RESOLVED").andExpect(status().isConflict());
        changeStatus(agent, id, "IN_PROGRESS").andExpect(status().isOk());
        changeStatus(agent, id, "IN_REVIEW").andExpect(status().isOk());
        changeStatus(agent, id, "RESOLVED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        // the reporter can reopen a resolved incident, which clears the resolution date
        changeStatus(owner, id, "OPEN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.resolvedAt").doesNotExist());
    }

    @Test
    void reportersCannotChangeTheStatusOtherThanReopening() throws Exception {
        createUser("owner.status@example.com", Role.USER);
        String owner = tokenFor("owner.status@example.com");
        long id = createIncident(owner, "Status rules");

        changeStatus(owner, id, "IN_PROGRESS").andExpect(status().isForbidden());
        changeStatus(owner, id, "CLOSED").andExpect(status().isForbidden());
    }

    @Test
    void onlyStaffCanAssignAndOnlyToStaff() throws Exception {
        createUser("owner.assign@example.com", Role.USER);
        User agentUser = createUser("agent.assign@example.com", Role.AGENT);
        User regular = createUser("regular.assign@example.com", Role.USER);
        String owner = tokenFor("owner.assign@example.com");
        String agent = tokenFor("agent.assign@example.com");
        long id = createIncident(owner, "Assignments");

        assign(owner, id, String.valueOf(agentUser.getId())).andExpect(status().isForbidden());
        assign(agent, id, String.valueOf(regular.getId())).andExpect(status().isBadRequest());

        assign(agent, id, String.valueOf(agentUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee.id").value(agentUser.getId()));

        assign(agent, id, "null")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").doesNotExist());
    }

    @Test
    void unknownIncidentsReturn404() throws Exception {
        createUser("agent.missing@example.com", Role.AGENT);

        mockMvc.perform(get("/api/incidents/999999999")
                        .header("Authorization", tokenFor("agent.missing@example.com")))
                .andExpect(status().isNotFound());
    }
}
