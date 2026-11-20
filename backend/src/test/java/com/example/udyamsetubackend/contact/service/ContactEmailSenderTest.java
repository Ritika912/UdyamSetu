package com.example.udyamsetubackend.contact.service;

import com.example.udyamsetubackend.contact.dto.ContactRequest;
import com.example.udyamsetubackend.contact.exception.ContactDeliveryException;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks the outgoing email without any SMTP connection. {@link JavaMailSender} is a mock, so {@code send} only
 * records the message it was given.
 */
class ContactEmailSenderTest {

    /** A secret-looking SMTP detail. It must never appear in the exception the client receives. */
    private static final String SMTP_DETAIL = "535 authentication failed for support@getudyamsetu.com pw=hunter2";

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);

    private final ContactEmailSender sender = new ContactEmailSender(provider);

    @Test
    void messageIsAddressedFromAndToTheSupportMailboxWithVisitorInReplyTo() throws Exception {
        when(provider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(newMessage());

        sender.send(request());

        MimeMessage sent = captureSent();
        assertEquals(addresses(sent.getFrom()), "support@getudyamsetu.com");
        assertEquals(addresses(sent.getRecipients(Message.RecipientType.TO)), "support@getudyamsetu.com");
        assertEquals(addresses(sent.getReplyTo()), "visitor@example.com");
        assertFalse(addresses(sent.getFrom()).contains("visitor@example.com"),
                "the visitor address must never be the sender");
    }

    @Test
    void subjectIsPrefixedAndBodyContainsTheSubmittedFieldsAsPlainText() throws Exception {
        when(provider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(newMessage());

        sender.send(request());

        MimeMessage sent = captureSent();
        assertEquals("[UdyamSetu website contact] Quote request", sent.getSubject());
        String body = sent.getContent().toString();
        assertTrue(body.contains("Name: Asha Verma"));
        assertTrue(body.contains("Email: visitor@example.com"));
        assertTrue(body.contains("Subject: Quote request"));
        assertTrue(body.contains("Hello, I would like a quote for printing services."));
        assertTrue(sent.getContentType().startsWith("text/plain"), "the body must be plain text");
    }

    @Test
    void sendIsCalledExactlyOnce() throws Exception {
        when(provider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(newMessage());

        sender.send(request());

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void smtpFailureBecomesAGenericDeliveryExceptionWithoutSmtpDetail() throws Exception {
        when(provider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(newMessage());
        org.mockito.Mockito.doThrow(new MailSendException(SMTP_DETAIL)).when(mailSender).send(any(MimeMessage.class));

        ContactDeliveryException ex = assertThrows(ContactDeliveryException.class, () -> sender.send(request()));

        assertEquals(ContactDeliveryException.PUBLIC_MESSAGE, ex.getMessage());
        assertFalse(ex.getMessage().contains("hunter2"));
        assertFalse(ex.getMessage().contains("535"));
    }

    @Test
    void missingMailConfigurationIsReportedAsDeliveryFailureAndSendsNothing() {
        when(provider.getIfAvailable()).thenReturn(null);

        assertThrows(ContactDeliveryException.class, () -> sender.send(request()));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private static ContactRequest request() {
        return new ContactRequest("Asha Verma", "visitor@example.com", "Quote request",
                "Hello, I would like a quote for printing services.", null);
    }

    private static MimeMessage newMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private MimeMessage captureSent() {
        org.mockito.ArgumentCaptor<MimeMessage> captor = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private static String addresses(Address[] addresses) {
        StringBuilder out = new StringBuilder();
        for (Address address : addresses) {
            if (!out.isEmpty()) {
                out.append(',');
            }
            out.append(((InternetAddress) address).getAddress());
        }
        return out.toString();
    }
}
