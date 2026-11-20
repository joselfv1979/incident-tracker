package com.jose.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jose.backend.support.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest extends AbstractIntegrationTest {

    private record Operation(String path, String method) {}

    // Every endpoint of the API. A new endpoint without documentation fails the build
    // once it is added here, and one missing from this list is easy to spot in review.
    private static final List<Operation> OPERATIONS = List.of(
            new Operation("/api/health", "get"),
            new Operation("/api/auth/login", "post"),
            new Operation("/api/auth/register", "post"),
            new Operation("/api/auth/verify-email", "post"),
            new Operation("/api/auth/resend-verification", "post"),
            new Operation("/api/auth/me", "get"),
            new Operation("/api/users", "get"),
            new Operation("/api/users/assignable", "get"),
            new Operation("/api/users/{id}/role", "patch"),
            new Operation("/api/incidents", "post"),
            new Operation("/api/incidents", "get"),
            new Operation("/api/incidents/{id}", "get"),
            new Operation("/api/incidents/{id}", "put"),
            new Operation("/api/incidents/{id}/status", "patch"),
            new Operation("/api/incidents/{id}/assignee", "patch"),
            new Operation("/api/incidents/{incidentId}/history", "get"),
            new Operation("/api/incidents/{incidentId}/comments", "get"),
            new Operation("/api/incidents/{incidentId}/comments", "post"),
            new Operation("/api/incidents/{incidentId}/comments/{commentId}", "put"),
            new Operation("/api/incidents/{incidentId}/comments/{commentId}", "delete"),
            new Operation("/api/stats/summary", "get"),
            new Operation("/api/stats/trend", "get"),
            new Operation("/api/stats/resolution-time", "get"));

    @Autowired
    MockMvc mockMvc;

    @Test
    void theSpecificationIsPublicAndDescribesTheApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Incident Tracker API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    void everyEndpointIsDocumentedWithASummary() throws Exception {
        for (Operation operation : OPERATIONS) {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(jsonPath("$.paths['" + operation.path() + "']." + operation.method() + ".summary")
                            .isNotEmpty());
        }
    }

    @Test
    void publicEndpointsDoNotRequireAToken() throws Exception {
        for (String path : List.of(
                "/api/auth/login", "/api/auth/register", "/api/auth/verify-email", "/api/auth/resend-verification")) {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(jsonPath("$.paths['" + path + "'].post.security").isEmpty());
        }
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/health'].get.security").isEmpty());
    }

    @Test
    void theInteractiveInterfaceIsServed() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
