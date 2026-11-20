package com.example.udyamsetubackend.contact.exception;

/**
 * The contact message could not be delivered. The message is fixed and the cause is kept only for the server
 * side, so SMTP host names, usernames and server replies never reach the client or the logs.
 */
public class ContactDeliveryException extends RuntimeException {

    public static final String PUBLIC_MESSAGE = "We could not send your message right now. Please try again later.";

    public ContactDeliveryException() {
        super(PUBLIC_MESSAGE);
    }

    public ContactDeliveryException(Throwable cause) {
        super(PUBLIC_MESSAGE, cause);
    }
}
