package com.example.udyamsetubackend.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Inbound payload for the public contact form. All four visible fields are required.
 *
 * <p>{@code website} is a hidden honeypot. People never see or fill it, but simple bots often do. A populated
 * value is refused by the service without sending email.
 *
 * <p>Single-line fields reject every control character, which includes CR and LF. This stops header injection
 * in the outgoing email.
 */
public record ContactRequest(

        @NotBlank(message = "fullName is required")
        @Size(max = 100, message = "fullName must not exceed 100 characters")
        @Pattern(regexp = SINGLE_LINE, message = "fullName must be a single line without control characters")
        String fullName,

        @NotBlank(message = "email is required")
        @Size(max = 254, message = "email must not exceed 254 characters")
        @Pattern(regexp = SINGLE_LINE, message = "email must be a single line without control characters")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "subject is required")
        @Size(max = 150, message = "subject must not exceed 150 characters")
        @Pattern(regexp = SINGLE_LINE, message = "subject must be a single line without control characters")
        String subject,

        @NotBlank(message = "message is required")
        @Size(min = 10, max = 2000, message = "message must be between 10 and 2000 characters")
        @Pattern(regexp = MULTI_LINE, message = "message must not contain control characters other than line breaks")
        String message,

        /** Honeypot. Must stay empty. Not part of the visible form. */
        @Size(max = 500, message = "website must not exceed 500 characters")
        String website
) {

    /** No control characters at all, which includes CR, LF and TAB. */
    static final String SINGLE_LINE = "^[^\\p{Cc}]*$";

    /**
     * Control characters are refused except line breaks and tabs, which a message body may contain. A single character
     * class is used rather than an alternation inside a repeated group, because the latter overflows the regex stack
     * on long input.
     */
    static final String MULTI_LINE = "^[\\P{Cc}\\r\\n\\t]*$";
}
