package com.example.udyamsetubackend.enquiry.dto;

import com.example.udyamsetubackend.common.validation.ValidContactNumber;
import com.example.udyamsetubackend.enquiry.enums.EnquirySource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Inbound payload for creating an enquiry. Only customerName and contactNumber are mandatory.
 */
public record EnquiryRequest(

        @NotBlank(message = "customerName is required")
        @Size(max = 100, message = "customerName must not exceed 100 characters")
        String customerName,

        @NotBlank(message = "contactNumber is required")
        @Size(max = 20, message = "contactNumber must not exceed 20 characters")
        @ValidContactNumber
        String contactNumber,

        EnquirySource source,

        @Size(max = 150, message = "requestedService must not exceed 150 characters")
        String requestedService,

        @Size(max = 2000, message = "requirements must not exceed 2000 characters")
        String requirements,

        @Size(max = 100, message = "occasion must not exceed 100 characters")
        String occasion,

        @DecimalMin(value = "0.00", message = "budget must not be negative")
        @Digits(integer = 10, fraction = 2, message = "budget must have at most 10 integer digits and 2 decimal places")
        BigDecimal budget,

        LocalDate expectedDate,

        LocalDate followUpDate,

        LocalTime followUpTime,

        @Size(max = 2000, message = "followUpNotes must not exceed 2000 characters")
        String followUpNotes,

        /** Optional catalogue service. Must refer to an existing, active service (checked in the service layer). */
        @Positive(message = "serviceId must be a positive number")
        Long serviceId
) {
}
