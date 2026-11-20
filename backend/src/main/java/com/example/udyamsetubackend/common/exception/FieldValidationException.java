package com.example.udyamsetubackend.common.exception;

/**
 * A request is well formed but a field refers to something invalid, such as a service that does not
 * exist or is inactive. Mapped to HTTP 400 with the same {@code fieldErrors} shape as bean validation.
 */
public class FieldValidationException extends RuntimeException {

    private final String field;

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
