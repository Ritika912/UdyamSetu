package com.example.udyamsetubackend.enquiry.dto;

import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Inbound payload for changing the status of an enquiry.
 */
public record EnquiryStatusRequest(
        @NotNull(message = "status is required")
        EnquiryStatus status
) {
}
