package com.example.udyamsetubackend.common.exception;

import com.example.udyamsetubackend.contact.exception.ContactDeliveryException;
import com.example.udyamsetubackend.contact.exception.ContactRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String ENUM_HINT_SOURCE = "source must be one of: WEBSITE, PHONE, WHATSAPP, WALK_IN, SOCIAL_MEDIA, OTHER";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Request validation failed", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("Unreadable request body: {}", ex.getMessage());
        String message = "Malformed request body or a field has an invalid format. "
                + "Dates use yyyy-MM-dd, times use HH:mm:ss, and "
                + ENUM_HINT_SOURCE + ".";
        return buildResponse(HttpStatus.BAD_REQUEST, message, Map.of());
    }

    /**
     * A path variable or query parameter could not be converted to its Java type, for example
     * {@code /api/services/abc} or {@code ?active=maybe}. This is a client error, so it returns 400 in the
     * standard error format. The submitted value is deliberately not echoed back.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String expectedType = ex.getRequiredType() == null ? "a valid value" : ex.getRequiredType().getSimpleName();
        String message = "Invalid value for '" + ex.getName() + "': expected " + expectedType;
        return buildResponse(HttpStatus.BAD_REQUEST, message, Map.of());
    }

    /** A field refers to an invalid reference (for example an unknown or inactive service). */
    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<ErrorResponse> handleFieldValidation(FieldValidationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Request validation failed",
                Map.of(ex.getField(), ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    /**
     * Last line of defence for uniqueness and foreign-key rules enforced by the database, for example two
     * concurrent requests creating the same customer. The DB detail is logged, not returned.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return buildResponse(HttpStatus.CONFLICT,
                "The request conflicts with existing data. Please check for duplicates or linked records.",
                Map.of());
    }

    /** An anti-spam check refused the contact submission. Same generic wording for every reason. */
    @ExceptionHandler(ContactRejectedException.class)
    public ResponseEntity<ErrorResponse> handleContactRejected(ContactRejectedException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of());
    }

    /** The contact email could not be sent. The SMTP detail is logged by the sender, never returned. */
    @ExceptionHandler(ContactDeliveryException.class)
    public ResponseEntity<ErrorResponse> handleContactDelivery(ContactDeliveryException ex) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), Map.of());
    }

    /**
     * Fallback for everything else. Spring MVC's own exceptions (for example 405 or 415) carry their
     * intended status and are passed through; any other exception is logged and returned as a generic 500
     * so that no stack trace or internal detail reaches the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode statusCode = springError.getStatusCode();
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            if (status != null) {
                return buildResponse(status, springError.getBody().getDetail(), Map.of());
            }
        }
        log.error("Unhandled exception while processing request", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", Map.of());
    }

    private static ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message,
                                                               Map<String, String> fieldErrors) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
