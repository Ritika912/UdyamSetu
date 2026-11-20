package com.example.udyamsetubackend.common.exception;

/**
 * Thrown when an operation conflicts with existing data, such as a duplicate key or deleting a
 * record that still has dependents. Mapped to HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
