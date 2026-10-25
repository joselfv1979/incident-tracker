package com.jose.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class VerificationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(VerificationEmailSender.class);

    private final JavaMailSender mailSender;
    private final AppMailProperties mailProperties;
    private final VerificationProperties verificationProperties;

    public VerificationEmailSender(
            JavaMailSender mailSender,
            AppMailProperties mailProperties,
            VerificationProperties verificationProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.verificationProperties = verificationProperties;
    }

    // Only after the transaction commits: no emails for users that were never saved
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(VerificationEmailRequested event) {
        String link = mailProperties.verificationUrl() + "?token=" + event.token();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(event.email());
        message.setSubject("Confirm your email address");
        message.setText("""
                Hi %s,

                Confirm your email address to activate your Incident Tracker account:

                %s

                The link expires in %d hours. If you did not create this account, ignore this message.
                """.formatted(event.name(), link, verificationProperties.tokenTtl().toHours()));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            // The user can request a new email with the resend endpoint
            log.error("Could not send the verification email", e);
        }
    }
}
