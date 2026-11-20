package com.example.udyamsetubackend.contact.service;

import com.example.udyamsetubackend.contact.dto.ContactRequest;
import com.example.udyamsetubackend.contact.dto.ContactResponse;
import com.example.udyamsetubackend.contact.exception.ContactDeliveryException;
import com.example.udyamsetubackend.contact.exception.ContactRejectedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Database-free tests for the honeypot decision and delivery hand-off. The email sender is a mock. */
class ContactServiceTest {

    private final ContactEmailSender emailSender = mock(ContactEmailSender.class);
    private final ContactService service = new ContactService(emailSender);

    @Test
    void validSubmissionIsSentOnceAndReportedAccepted() {
        ContactResponse response = service.submit(request(null));

        verify(emailSender, times(1)).send(any(ContactRequest.class));
        assertEquals(ContactService.ACCEPTED_MESSAGE, response.message());
    }

    @Test
    void populatedHoneypotIsRejectedWithoutSendingOrReportingSuccess() {
        assertThrows(ContactRejectedException.class, () -> service.submit(request("http://spam.example")));

        verifyNoInteractions(emailSender);
    }

    @Test
    void whitespaceOnlyHoneypotCountsAsEmpty() {
        ContactResponse response = service.submit(request("   "));

        verify(emailSender, times(1)).send(any(ContactRequest.class));
        assertEquals(ContactService.ACCEPTED_MESSAGE, response.message());
    }

    @Test
    void deliveryFailurePropagatesAndIsNotReportedAsSuccess() {
        doThrow(new ContactDeliveryException()).when(emailSender).send(any(ContactRequest.class));

        assertThrows(ContactDeliveryException.class, () -> service.submit(request(null)));
    }

    @Test
    void rejectedHoneypotExceptionUsesTheGenericMessage() {
        ContactRejectedException ex = assertThrows(ContactRejectedException.class,
                () -> service.submit(request("bot")));

        assertEquals(ContactRejectedException.PUBLIC_MESSAGE, ex.getMessage());
        verify(emailSender, never()).send(any());
    }

    private static ContactRequest request(String website) {
        return new ContactRequest("Asha Verma", "visitor@example.com", "Quote request",
                "Hello, I would like a quote for printing services.", website);
    }
}
