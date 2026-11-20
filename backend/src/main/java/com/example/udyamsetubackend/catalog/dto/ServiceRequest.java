package com.example.udyamsetubackend.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Inbound payload for creating or replacing a catalogue service.
 *
 * <p>On create, a null {@code isActive} means active. On update, a null {@code isActive} means
 * unchanged. Name trimming and blank-to-null handling happen in the service layer.
 */
public record ServiceRequest(

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must not exceed 150 characters")
        String name,

        @Size(max = 2000, message = "description must not exceed 2000 characters")
        String description,

        @Size(max = 100, message = "category must not exceed 100 characters")
        String category,

        @DecimalMin(value = "0.00", message = "price must not be negative")
        @Digits(integer = 10, fraction = 2, message = "price must have at most 10 integer digits and 2 decimal places")
        BigDecimal price,

        Boolean isActive
) {
}
