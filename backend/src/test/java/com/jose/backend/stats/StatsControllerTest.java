package com.jose.backend.stats;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.jose.backend.support.AbstractIntegrationTest;
import com.jose.backend.user.Role;
import com.jose.backend.user.User;
import com.jose.backend.user.UserRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatsControllerTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "secret-password";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;

    // The test states its starting point instead of trusting the others to clean up.
    // It runs in the test transaction, so it is rolled back like everything else.
    @BeforeEach
    void startWithoutIncidents() {
        jdbc.update("DELETE FROM incidents");
    }

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

    private long createIncident(String token) throws Exception {
        String response = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Stats test\",\"description\":\"Something is broken\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.<Number>read(response, "$.id").longValue();
    }

    private void changeStatus(String token, long id, String newStatus) throws Exception {
        mockMvc.perform(patch("/api/incidents/" + id + "/status")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + newStatus + "\"}"))
                .andExpect(status().isOk());
    }

    private void resolve(String token, long id) throws Exception {
        changeStatus(token, id, "IN_PROGRESS");
        changeStatus(token, id, "IN_REVIEW");
        changeStatus(token, id, "RESOLVED");
    }

    private void assign(String token, long id, long assigneeId) throws Exception {
        mockMvc.perform(patch("/api/incidents/" + id + "/assignee")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":" + assigneeId + "}"))
                .andExpect(status().isOk());
    }

    private void setPriority(String token, long id, String priority) throws Exception {
        mockMvc.perform(put("/api/incidents/" + id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Stats test\",\"description\":\"Something is broken\","
                                + "\"priority\":\"" + priority + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void regularUsersCannotSeeStatistics() throws Exception {
        createUser("user.stats@example.com", Role.USER);
        String user = tokenFor("user.stats@example.com");

        for (String path : new String[] {"/api/stats/summary", "/api/stats/trend", "/api/stats/resolution-time"}) {
            mockMvc.perform(get(path).header("Authorization", user)).andExpect(status().isForbidden());
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void withoutIncidentsEverythingIsZeroAndEmpty() throws Exception {
        createUser("agent.empty@example.com", Role.AGENT);
        String agent = tokenFor("agent.empty@example.com");

        mockMvc.perform(get("/api/stats/summary").header("Authorization", agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.active").value(0))
                .andExpect(jsonPath("$.unassigned").value(0))
                .andExpect(jsonPath("$.byStatus.OPEN").value(0))
                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(0))
                .andExpect(jsonPath("$.byStatus.IN_REVIEW").value(0))
                .andExpect(jsonPath("$.byStatus.RESOLVED").value(0))
                .andExpect(jsonPath("$.byStatus.CLOSED").value(0))
                .andExpect(jsonPath("$.byPriority.LOW").value(0))
                .andExpect(jsonPath("$.byPriority.MEDIUM").value(0))
                .andExpect(jsonPath("$.byPriority.HIGH").value(0))
                .andExpect(jsonPath("$.byPriority.CRITICAL").value(0))
                .andExpect(jsonPath("$.byAssignee").isEmpty());

        mockMvc.perform(get("/api/stats/resolution-time").header("Authorization", agent))
                .andExpect(jsonPath("$.resolvedCount").value(0))
                .andExpect(jsonPath("$.averageHours").doesNotExist())
                .andExpect(jsonPath("$.byPriority").isEmpty());
    }

    @Test
    void summaryCountsByStatusPriorityAndAssignee() throws Exception {
        User agentUser = createUser("agent.stats@example.com", Role.AGENT);
        createUser("owner.stats@example.com", Role.USER);
        String agent = tokenFor("agent.stats@example.com");
        String owner = tokenFor("owner.stats@example.com");

        long inProgress = createIncident(owner); // active, assigned, HIGH
        createIncident(owner); // active, unassigned, MEDIUM, stays OPEN
        long resolved = createIncident(owner); // RESOLVED
        long closed = createIncident(owner); // CLOSED

        changeStatus(agent, inProgress, "IN_PROGRESS");
        assign(agent, inProgress, agentUser.getId());
        setPriority(agent, inProgress, "HIGH");

        resolve(agent, resolved);

        resolve(agent, closed);
        changeStatus(agent, closed, "CLOSED");

        mockMvc.perform(get("/api/stats/summary").header("Authorization", agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.active").value(2))
                .andExpect(jsonPath("$.unassigned").value(1))
                .andExpect(jsonPath("$.byStatus.OPEN").value(1))
                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(1))
                .andExpect(jsonPath("$.byStatus.IN_REVIEW").value(0))
                .andExpect(jsonPath("$.byStatus.RESOLVED").value(1))
                .andExpect(jsonPath("$.byStatus.CLOSED").value(1))
                .andExpect(jsonPath("$.byPriority.LOW").value(0))
                .andExpect(jsonPath("$.byPriority.MEDIUM").value(3))
                .andExpect(jsonPath("$.byPriority.HIGH").value(1))
                .andExpect(jsonPath("$.byPriority.CRITICAL").value(0))
                .andExpect(jsonPath("$.byAssignee.length()").value(1))
                .andExpect(jsonPath("$.byAssignee[0].user.name").value("agent.stats"))
                .andExpect(jsonPath("$.byAssignee[0].active").value(1));
    }

    @Test
    void trendHasOnePointPerDayAndCountsToday() throws Exception {
        createUser("agent.trend@example.com", Role.AGENT);
        String agent = tokenFor("agent.trend@example.com");

        // Days are UTC: this would only be wrong if the test ran across midnight UTC
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        long first = createIncident(agent);
        createIncident(agent);
        resolve(agent, first);

        mockMvc.perform(get("/api/stats/trend?days=7").header("Authorization", agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(7))
                .andExpect(jsonPath("$.points.length()").value(7))
                .andExpect(jsonPath("$.points[0].date").value(today.minusDays(6).toString()))
                .andExpect(jsonPath("$.points[6].date").value(today.toString()))
                .andExpect(jsonPath("$.points[6].created").value(2))
                .andExpect(jsonPath("$.points[6].resolved").value(1))
                .andExpect(jsonPath("$.points[0].created").value(0))
                .andExpect(jsonPath("$.points[0].resolved").value(0));
    }

    @Test
    void trendRejectsInvalidPeriods() throws Exception {
        createUser("agent.days@example.com", Role.AGENT);
        String agent = tokenFor("agent.days@example.com");

        for (String days : new String[] {"0", "-3", "366"}) {
            mockMvc.perform(get("/api/stats/trend?days=" + days).header("Authorization", agent))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(get("/api/stats/resolution-time?days=" + days).header("Authorization", agent))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void resolutionTimeIsAveragedOverallAndByPriority() throws Exception {
        createUser("agent.resolution@example.com", Role.AGENT);
        String agent = tokenFor("agent.resolution@example.com");

        long medium = createIncident(agent);
        long high = createIncident(agent);
        setPriority(agent, high, "HIGH");

        // Known durations: move the creation date back, then resolve
        jdbc.update("UPDATE incidents SET created_at = now() - interval '4 hours' WHERE id = ?", medium);
        jdbc.update("UPDATE incidents SET created_at = now() - interval '8 hours' WHERE id = ?", high);
        resolve(agent, medium);
        resolve(agent, high);

        mockMvc.perform(get("/api/stats/resolution-time?days=30").header("Authorization", agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedCount").value(2))
                .andExpect(jsonPath("$.averageHours").value(6.0))
                .andExpect(jsonPath("$.byPriority.length()").value(2))
                .andExpect(jsonPath("$.byPriority[?(@.priority=='MEDIUM')].averageHours").value(4.0))
                .andExpect(jsonPath("$.byPriority[?(@.priority=='MEDIUM')].resolvedCount").value(1))
                .andExpect(jsonPath("$.byPriority[?(@.priority=='HIGH')].averageHours").value(8.0))
                .andExpect(jsonPath("$.byPriority[?(@.priority=='HIGH')].resolvedCount").value(1));
    }

    @Test
    void incidentsResolvedBeforeThePeriodAreLeftOut() throws Exception {
        createUser("agent.period@example.com", Role.AGENT);
        String agent = tokenFor("agent.period@example.com");

        long old = createIncident(agent);
        resolve(agent, old);
        jdbc.update("UPDATE incidents SET resolved_at = now() - interval '40 days' WHERE id = ?", old);

        mockMvc.perform(get("/api/stats/resolution-time?days=30").header("Authorization", agent))
                .andExpect(jsonPath("$.resolvedCount").value(0))
                .andExpect(jsonPath("$.averageHours").doesNotExist());
        mockMvc.perform(get("/api/stats/resolution-time?days=90").header("Authorization", agent))
                .andExpect(jsonPath("$.resolvedCount").value(1));
    }
}
