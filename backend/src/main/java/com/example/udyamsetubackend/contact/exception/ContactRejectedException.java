package com.example.udyamsetubackend.contact.exception;

/**
 * The submission was refused by an anti-spam check (for example a filled honeypot field). It is reported with the
 * same generic wording for every reason, and no email is sent.
 */
public class ContactRejectedException extends RuntimeException {

    public static final String PUBLIC_MESSAGE = "Your message could not be accepted.";

    public ContactRejectedException() {
        super(PUBLIC_MESSAGE);
    }
}
