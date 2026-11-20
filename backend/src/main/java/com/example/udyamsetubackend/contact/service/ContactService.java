package com.example.udyamsetubackend.contact.service;

import com.example.udyamsetubackend.contact.dto.ContactRequest;
import com.example.udyamsetubackend.contact.dto.ContactResponse;
import com.example.udyamsetubackend.contact.exception.ContactRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Applies the anti-spam honeypot check, then delivers the message. The contact form has no persistence.
 */
@Service
public class ContactService {

    static final String ACCEPTED_MESSAGE = "Thank you. Your message has been sent to our support team.";

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private final ContactEmailSender emailSender;

    public ContactService(ContactEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    /**
     * @throws ContactRejectedException if the honeypot field is populated. No email is sent and no success is reported.
     * @throws com.example.udyamsetubackend.contact.exception.ContactDeliveryException if delivery fails
     */
    public ContactResponse submit(ContactRequest request) {
        if (isPopulated(request.website())) {
            // Logged without any submitted content.
            log.info("Contact submission rejected by honeypot.");
            throw new ContactRejectedException();
        }
        emailSender.send(request);
        log.info("Contact message delivered to support mailbox.");
        return new ContactResponse(ACCEPTED_MESSAGE);
    }

    private static boolean isPopulated(String honeypot) {
        return honeypot != null && !honeypot.isBlank();
    }
}
