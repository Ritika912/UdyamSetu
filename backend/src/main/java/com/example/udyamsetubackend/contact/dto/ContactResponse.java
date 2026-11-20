package com.example.udyamsetubackend.contact.dto;

/**
 * Returned with 202 Accepted once the message has been delivered to the support mailbox.
 *
 * @param message a short confirmation for the visitor
 */
public record ContactResponse(String message) {
}
