package com.jose.backend.health;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HealthControllerTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new HealthController(jdbcTemplate)).build();

    @Test
    void returnsUpWhenDatabaseResponds() throws Exception {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"));
    }

    @Test
    void returns503WhenDatabaseFails() throws Exception {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.database").value("DOWN"));
    }
}
