package com.example.udyamsetubackend.customer.dto;

import com.example.udyamsetubackend.common.validation.ValidContactNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound payload for creating or replacing a customer.
 */
public record CustomerRequest(

        @NotBlank(message = "customerName is required")
        @Size(max = 100, message = "customerName must not exceed 100 characters")
        String customerName,

        @NotBlank(message = "contactNumber is required")
        @Size(max = 20, message = "contactNumber must not exceed 20 characters")
        @ValidContactNumber
        String contactNumber,

        @Size(max = 2000, message = "notes must not exceed 2000 characters")
        String notes
) {
}
