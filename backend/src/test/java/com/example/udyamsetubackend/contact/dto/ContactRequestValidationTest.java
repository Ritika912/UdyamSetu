package com.example.udyamsetubackend.contact.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bean Validation rules for the public contact form. No Spring context is needed. */
class ContactRequestValidationTest {

    private static final String VALID_EMAIL = "visitor@example.com";
    private static final String VALID_MESSAGE = "Hello, I would like a quote for printing services.";

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void completeSubmissionPasses() {
        assertNoViolations(request("Asha Verma", VALID_EMAIL, "Quote request", VALID_MESSAGE, null));
    }

    @Test
    void blankOrMissingRequiredFieldsAreReportedPerField() {
        Set<ConstraintViolation<ContactRequest>> violations = validator.validate(
                request(" ", null, "", "   ", null));

        assertFieldReported(violations, "fullName", "fullName is required");
        assertFieldReported(violations, "email", "email is required");
        assertFieldReported(violations, "subject", "subject is required");
        assertFieldReported(violations, "message", "message is required");
    }

    @Test
    void malformedEmailIsRejected() {
        assertFieldReported(validator.validate(request("A", "not-an-email", "S", VALID_MESSAGE, null)),
                "email", "email must be a valid email address");
    }

    @Test
    void emailWithLineBreakIsRejected() {
        Set<ConstraintViolation<ContactRequest>> violations = validator.validate(
                request("A", "visitor@example.com\r\nBcc: someone@example.com", "S", VALID_MESSAGE, null));

        assertTrue(hasViolationOn(violations, "email"));
    }

    @Test
    void fullNameAndSubjectRejectLineBreaksAndControlCharacters() {
        assertTrue(hasViolationOn(validator.validate(request("Asha\nBcc: x@example.com", VALID_EMAIL, "S",
                VALID_MESSAGE, null)), "fullName"));
        assertTrue(hasViolationOn(validator.validate(request("Asha", VALID_EMAIL, "Subject\rInjected",
                VALID_MESSAGE, null)), "subject"));
        assertTrue(hasViolationOn(validator.validate(request("Asha\tB", VALID_EMAIL, "S", VALID_MESSAGE, null)),
                "fullName"));
    }

    @Test
    void messageMayContainLineBreaksButNoOtherControlCharacters() {
        assertNoViolations(request("Asha", VALID_EMAIL, "S", "First line.\nSecond line.\r\nThird line.", null));
        assertTrue(hasViolationOn(validator.validate(request("Asha", VALID_EMAIL, "S",
                "Contains a bell \u0007 character.", null)), "message"));
    }

    @Test
    void messageLengthBoundsAreInclusive() {
        assertNoViolations(request("Asha", VALID_EMAIL, "S", "a".repeat(10), null));
        assertNoViolations(request("Asha", VALID_EMAIL, "S", "a".repeat(2000), null));
        assertTrue(hasViolationOn(validator.validate(request("Asha", VALID_EMAIL, "S", "a".repeat(9), null)),
                "message"));
        assertTrue(hasViolationOn(validator.validate(request("Asha", VALID_EMAIL, "S", "a".repeat(2001), null)),
                "message"));
    }

    @Test
    void fieldLengthLimitsAreEnforced() {
        assertTrue(hasViolationOn(validator.validate(request("n".repeat(101), VALID_EMAIL, "S",
                VALID_MESSAGE, null)), "fullName"));
        assertTrue(hasViolationOn(validator.validate(request("Asha", VALID_EMAIL, "s".repeat(151),
                VALID_MESSAGE, null)), "subject"));
        assertTrue(hasViolationOn(validator.validate(request("Asha", "a".repeat(246) + "@example.com", "S",
                VALID_MESSAGE, null)), "email"));
    }

    @Test
    void honeypotIsOptionalAndBounded() {
        assertNoViolations(request("Asha", VALID_EMAIL, "S", VALID_MESSAGE, null));
        assertNoViolations(request("Asha", VALID_EMAIL, "S", VALID_MESSAGE, "http://spam.example"));
        assertTrue(hasViolationOn(validator.validate(request("Asha", VALID_EMAIL, "S", VALID_MESSAGE,
                "x".repeat(501))), "website"));
    }

    private static ContactRequest request(String fullName, String email, String subject, String message,
                                          String website) {
        return new ContactRequest(fullName, email, subject, message, website);
    }

    private static void assertNoViolations(ContactRequest request) {
        Set<ConstraintViolation<ContactRequest>> violations = validator.validate(request);
        assertEquals(Set.of(), violations.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(java.util.stream.Collectors.toSet()));
    }

    private static boolean hasViolationOn(Set<ConstraintViolation<ContactRequest>> violations, String field) {
        return violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    private static void assertFieldReported(Set<ConstraintViolation<ContactRequest>> violations, String field,
                                            String message) {
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(field)
                        && v.getMessage().equals(message)),
                "expected '" + message + "' on " + field + " but was " + violations);
    }
}
