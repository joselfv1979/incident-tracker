package com.jose.backend.comment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class CommentControllerTest {

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
                        .content("{\"title\":\"Comments test\",\"description\":\"Something is broken\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.<Number>read(response, "$.id").longValue();
    }

    private ResultActions addComment(String token, long incidentId, String json) throws Exception {
        return mockMvc.perform(post("/api/incidents/" + incidentId + "/comments")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private long createComment(String token, long incidentId, String body, boolean internal)
            throws Exception {
        String response = addComment(
                        token, incidentId, "{\"body\":\"" + body + "\",\"internal\":" + internal + "}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.<Number>read(response, "$.id").longValue();
    }

    private ResultActions list(String token, long incidentId) throws Exception {
        return mockMvc.perform(
                get("/api/incidents/" + incidentId + "/comments").header("Authorization", token));
    }

    private ResultActions edit(String token, long incidentId, long commentId, String body)
            throws Exception {
        return mockMvc.perform(put("/api/incidents/" + incidentId + "/comments/" + commentId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"" + body + "\"}"));
    }

    private ResultActions remove(String token, long incidentId, long commentId) throws Exception {
        return mockMvc.perform(delete("/api/incidents/" + incidentId + "/comments/" + commentId)
                .header("Authorization", token));
    }

    @Test
    void theReporterCanCommentOnTheirIncident() throws Exception {
        createUser("owner.comment@example.com", Role.USER);
        String owner = tokenFor("owner.comment@example.com");
        long incidentId = createIncident(owner);

        addComment(owner, incidentId, "{\"body\":\"  Still broken  \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Still broken"))
                .andExpect(jsonPath("$.internal").value(false))
                .andExpect(jsonPath("$.author.name").value("owner.comment"))
                .andExpect(jsonPath("$.author.email").doesNotExist())
                .andExpect(jsonPath("$.editedAt").doesNotExist());
    }

    @Test
    void strangersCannotReadOrWriteComments() throws Exception {
        createUser("owner.private@example.com", Role.USER);
        createUser("stranger.private@example.com", Role.USER);
        long incidentId = createIncident(tokenFor("owner.private@example.com"));
        String stranger = tokenFor("stranger.private@example.com");

        list(stranger, incidentId).andExpect(status().isNotFound());
        addComment(stranger, incidentId, "{\"body\":\"Hi\"}").andExpect(status().isNotFound());
    }

    @Test
    void staffAndReporterShareAChronologicalThread() throws Exception {
        createUser("owner.thread@example.com", Role.USER);
        createUser("agent.thread@example.com", Role.AGENT);
        String owner = tokenFor("owner.thread@example.com");
        String agent = tokenFor("agent.thread@example.com");
        long incidentId = createIncident(owner);

        createComment(owner, incidentId, "First", false);
        createComment(agent, incidentId, "Second", false);
        createComment(owner, incidentId, "Third", false);

        list(owner, incidentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].body").value("First"))
                .andExpect(jsonPath("$[1].body").value("Second"))
                .andExpect(jsonPath("$[1].author.role").value("AGENT"))
                .andExpect(jsonPath("$[2].body").value("Third"));
    }

    @Test
    void internalNotesAreHiddenFromTheReporter() throws Exception {
        createUser("owner.internal@example.com", Role.USER);
        createUser("agent.internal@example.com", Role.AGENT);
        String owner = tokenFor("owner.internal@example.com");
        String agent = tokenFor("agent.internal@example.com");
        long incidentId = createIncident(owner);

        long publicId = createComment(agent, incidentId, "Public reply", false);
        long internalId = createComment(agent, incidentId, "Internal note", true);

        list(agent, incidentId).andExpect(jsonPath("$.length()").value(2));
        list(owner, incidentId)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(publicId));

        // for the reporter an internal note does not exist
        edit(owner, incidentId, internalId, "Changed").andExpect(status().isNotFound());
        remove(owner, incidentId, internalId).andExpect(status().isNotFound());
        // and the reporter cannot write one
        addComment(owner, incidentId, "{\"body\":\"Secret\",\"internal\":true}")
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyTheAuthorCanEditAndTheEditIsMarked() throws Exception {
        createUser("owner.edit@example.com", Role.USER);
        createUser("agent.edit@example.com", Role.AGENT);
        String owner = tokenFor("owner.edit@example.com");
        String agent = tokenFor("agent.edit@example.com");
        long incidentId = createIncident(owner);
        long commentId = createComment(owner, incidentId, "Typo", false);

        edit(owner, incidentId, commentId, "Fixed")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Fixed"))
                .andExpect(jsonPath("$.editedAt").isNotEmpty());

        // not even the staff can edit somebody else's comment
        edit(agent, incidentId, commentId, "Rewritten").andExpect(status().isForbidden());
    }

    @Test
    void editingWithTheSameTextIsNotMarkedAsEdited() throws Exception {
        createUser("owner.same@example.com", Role.USER);
        String owner = tokenFor("owner.same@example.com");
        long incidentId = createIncident(owner);
        long commentId = createComment(owner, incidentId, "Same", false);

        edit(owner, incidentId, commentId, "Same")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editedAt").doesNotExist());
    }

    @Test
    void authorsDeleteTheirOwnCommentsAndOnlyAdminsDeleteOthers() throws Exception {
        createUser("owner.delete@example.com", Role.USER);
        createUser("agent.delete@example.com", Role.AGENT);
        createUser("admin.delete@example.com", Role.ADMIN);
        String owner = tokenFor("owner.delete@example.com");
        String agent = tokenFor("agent.delete@example.com");
        String admin = tokenFor("admin.delete@example.com");
        long incidentId = createIncident(owner);

        long ownerComment = createComment(owner, incidentId, "Mine", false);
        long otherComment = createComment(owner, incidentId, "Also mine", false);

        remove(agent, incidentId, ownerComment).andExpect(status().isForbidden());
        remove(owner, incidentId, ownerComment).andExpect(status().isNoContent());
        remove(admin, incidentId, otherComment).andExpect(status().isNoContent());

        list(owner, incidentId).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invalidRequestsAreRejected() throws Exception {
        createUser("owner.invalid@example.com", Role.USER);
        String owner = tokenFor("owner.invalid@example.com");
        long incidentId = createIncident(owner);
        long otherIncidentId = createIncident(owner);
        long commentId = createComment(owner, incidentId, "Belongs to the first", false);

        addComment(owner, incidentId, "{\"body\":\"   \"}").andExpect(status().isBadRequest());
        // a comment is only reachable through its own incident
        edit(owner, otherIncidentId, commentId, "Nope").andExpect(status().isNotFound());
        mockMvc.perform(get("/api/incidents/" + incidentId + "/comments"))
                .andExpect(status().isUnauthorized());
    }
}
