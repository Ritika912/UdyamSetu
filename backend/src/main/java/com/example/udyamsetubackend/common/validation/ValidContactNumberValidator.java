package com.example.udyamsetubackend.common.validation;

import com.example.udyamsetubackend.common.contact.ContactNumberNormalizer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidContactNumberValidator implements ConstraintValidator<ValidContactNumber, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            ContactNumberNormalizer.normalize(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
