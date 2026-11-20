package com.example.udyamsetubackend.contact.service;

import com.example.udyamsetubackend.contact.dto.ContactRequest;
import com.example.udyamsetubackend.contact.exception.ContactDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Sends a contact message to the fixed support mailbox over SMTP.
 *
 * <p>The sender and the recipient are both {@value #SUPPORT_MAILBOX}, which is also the authenticated GoDaddy
 * mailbox. The visitor's address goes only into {@code Reply-To}, so it can never be spoofed as the sender.
 * The body is plain text, which prevents HTML injection.
 *
 * <p>Failures are logged by exception class only. Server replies, host names and credentials never reach the log
 * or the client.
 */
@Component
public class ContactEmailSender {

    /** Fixed destination and sending identity. It is not configurable, so a request can never choose a recipient. */
    static final String SUPPORT_MAILBOX = "support@getudyamsetu.com";

    private static final String SUBJECT_PREFIX = "[UdyamSetu website contact] ";

    private static final Logger log = LoggerFactory.getLogger(ContactEmailSender.class);

    /** Optional: absent when no SMTP settings exist, for example in local development without mail configuration. */
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public ContactEmailSender(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    /**
     * Delivers the message synchronously.
     *
     * @throws ContactDeliveryException if no mail sender is configured or SMTP delivery fails
     */
    public void send(ContactRequest request) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("Contact message not sent: mail is not configured.");
            throw new ContactDeliveryException();
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(SUPPORT_MAILBOX);
            helper.setTo(SUPPORT_MAILBOX);
            helper.setReplyTo(request.email().strip());
            helper.setSubject(SUBJECT_PREFIX + request.subject().strip());
            helper.setText(body(request), false);
            mailSender.send(message);
        } catch (MailException | MessagingException ex) {
            log.error("Contact message not sent: SMTP delivery failed ({}).", ex.getClass().getSimpleName());
            throw new ContactDeliveryException(ex);
        }
    }

    private static String body(ContactRequest request) {
        return """
                New message from the UdyamSetu website contact form.

                Name: %s
                Email: %s
                Subject: %s

                Message:
                %s
                """.formatted(
                request.fullName().strip(),
                request.email().strip(),
                request.subject().strip(),
                request.message().strip());
    }
}
