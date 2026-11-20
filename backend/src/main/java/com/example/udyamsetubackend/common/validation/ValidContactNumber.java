package com.example.udyamsetubackend.common.validation;

import com.example.udyamsetubackend.common.contact.ContactNumberNormalizer;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a contact number can be normalized unambiguously. See {@link ContactNumberNormalizer}.
 * Null is accepted here; combine with {@code @NotBlank} when the field is mandatory.
 */
@Documented
@Constraint(validatedBy = ValidContactNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidContactNumber {

    String message() default ContactNumberNormalizer.INVALID_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
