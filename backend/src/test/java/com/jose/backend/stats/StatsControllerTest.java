package com.jose.backend.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.jose.backend.user.Role;
import com.jose.backend.user.User;
import com.jose.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatsControllerTest {

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

    private void changeStatus(String token, long id, String status) throws Exception {
        mockMvc.perform(patch("/api/incidents/" + id + "/status")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    private String body(String path, String token) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static long number(String json, String path) {
        return JsonPath.<Number>read(json, path).longValue();
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
    void summaryCountsByStatusPriorityAndAssignee() throws Exception {
        User agentUser = createUser("agent.stats@example.com", Role.AGENT);
        createUser("owner.stats@example.com", Role.USER);
        String agent = tokenFor("agent.stats@example.com");
        String owner = tokenFor("owner.stats@example.com");

        String before = body("/api/stats/summary", agent);

        long first = createIncident(owner);
        long second = createIncident(owner);
        changeStatus(agent, first, "IN_PROGRESS");
        mockMvc.perform(patch("/api/incidents/" + first + "/assignee")
                        .header("Authorization", agent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":" + agentUser.getId() + "}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/incidents/" + second)
                        .header("Authorization", agent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Stats test\",\"description\":\"Something is broken\","
                                + "\"priority\":\"HIGH\"}"))
                .andExpect(status().isOk());

        String after = body("/api/stats/summary", agent);

        assertThat(number(after, "$.total")).isEqualTo(number(before, "$.total") + 2);
        assertThat(number(after, "$.active")).isEqualTo(number(before, "$.active") + 2);
        assertThat(number(after, "$.unassigned")).isEqualTo(number(before, "$.unassigned") + 1);
        assertThat(number(after, "$.byStatus.OPEN")).isEqualTo(number(before, "$.byStatus.OPEN") + 1);
        assertThat(number(after, "$.byStatus.IN_PROGRESS"))
                .isEqualTo(number(before, "$.byStatus.IN_PROGRESS") + 1);
        assertThat(number(after, "$.byPriority.HIGH")).isEqualTo(number(before, "$.byPriority.HIGH") + 1);
        assertThat(number(after, "$.byPriority.MEDIUM"))
                .isEqualTo(number(before, "$.byPriority.MEDIUM") + 1);

        mockMvc.perform(get("/api/stats/summary").header("Authorization", agent))
                .andExpect(jsonPath("$.byAssignee[?(@.user.name=='agent.stats')].active").value(1));
    }

    @Test
    void everyStatusAndPriorityIsPresentEvenWithoutIncidents() throws Exception {
        createUser("agent.zeros@example.com", Role.AGENT);

        mockMvc.perform(get("/api/stats/summary").header("Authorization", tokenFor("agent.zeros@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byStatus.OPEN").isNumber())
                .andExpect(jsonPath("$.byStatus.CLOSED").isNumber())
                .andExpect(jsonPath("$.byPriority.LOW").isNumber())
                .andExpect(jsonPath("$.byPriority.CRITICAL").isNumber());
    }

    @Test
    void trendReturnsOnePointPerDayEndingToday() throws Exception {
        createUser("agent.trend@example.com", Role.AGENT);
        String agent = tokenFor("agent.trend@example.com");

        String before = body("/api/stats/trend?days=7", agent);
        createIncident(agent);
        createIncident(agent);
        String after = body("/api/stats/trend?days=7", agent);

        assertThat(JsonPath.<Integer>read(after, "$.points.length()")).isEqualTo(7);
        assertThat(JsonPath.<String>read(after, "$.points[6].date"))
                .isEqualTo(LocalDate.now(ZoneOffset.UTC).toString());
        assertThat(number(after, "$.points[6].created")).isEqualTo(number(before, "$.points[6].created") + 2);
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
    void resolutionTimeIncludesResolvedIncidents() throws Exception {
        createUser("agent.resolution@example.com", Role.AGENT);
        String agent = tokenFor("agent.resolution@example.com");

        String before = body("/api/stats/resolution-time?days=30", agent);

        long id = createIncident(agent);
        changeStatus(agent, id, "IN_PROGRESS");
        changeStatus(agent, id, "IN_REVIEW");
        changeStatus(agent, id, "RESOLVED");

        String after = body("/api/stats/resolution-time?days=30", agent);

        assertThat(number(after, "$.resolvedCount")).isEqualTo(number(before, "$.resolvedCount") + 1);
        assertThat(JsonPath.<Number>read(after, "$.averageHours").doubleValue()).isGreaterThanOrEqualTo(0);
        mockMvc.perform(get("/api/stats/resolution-time?days=30").header("Authorization", agent))
                .andExpect(jsonPath("$.byPriority[?(@.priority=='MEDIUM')]").exists());
    }
}
