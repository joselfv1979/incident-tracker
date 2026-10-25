package com.jose.backend.auth;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jose.backend.user.UserRepository;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RegistrationFlowTest {

    private static final String EMAIL = "new.user@example.com";
    private static final String PASSWORD = "secret-password";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @MockitoBean JavaMailSender mailSender;

    // Tokens are removed by ON DELETE CASCADE
    @AfterEach
    void cleanUp() {
        users.findByEmail(EMAIL).ifPresent(users::delete);
    }

    private void perform(String path, String json, int expectedStatus) throws Exception {
        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(expectedStatus));
    }

    private String registerJson() {
        return """
                {"name":"New User","email":"%s","password":"%s"}""".formatted(EMAIL, PASSWORD);
    }

    private String loginJson() {
        return """
                {"email":"%s","password":"%s"}""".formatted(EMAIL, PASSWORD);
    }

    @Test
    void registerVerifyAndLogin() throws Exception {
        perform("/api/auth/register", registerJson(), 202);

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        Matcher matcher = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(mail.getValue().getText());
        assertThat(matcher.find()).isTrue();
        String token = matcher.group(1);

        perform("/api/auth/login", loginJson(), 403);
        perform("/api/auth/verify-email", "{\"token\":\"" + token + "\"}", 200);
        perform("/api/auth/login", loginJson(), 200);
        // opening the link twice is harmless
        perform("/api/auth/verify-email", "{\"token\":\"" + token + "\"}", 200);
    }

    @Test
    void registeringAnAlreadyVerifiedEmailSendsNothing() throws Exception {
        perform("/api/auth/register", registerJson(), 202);
        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        Matcher matcher = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(mail.getValue().getText());
        matcher.find();
        perform("/api/auth/verify-email", "{\"token\":\"" + matcher.group(1) + "\"}", 200);

        org.mockito.Mockito.clearInvocations(mailSender);
        perform("/api/auth/register", registerJson(), 202);

        verifyNoInteractions(mailSender);
    }

    @Test
    void verifyRejectsUnknownToken() throws Exception {
        perform("/api/auth/verify-email", "{\"token\":\"does-not-exist\"}", 400);
    }

    @Test
    void registerRejectsShortPassword() throws Exception {
        perform("/api/auth/register",
                """
                {"name":"New User","email":"%s","password":"short"}""".formatted(EMAIL), 400);
    }
}
