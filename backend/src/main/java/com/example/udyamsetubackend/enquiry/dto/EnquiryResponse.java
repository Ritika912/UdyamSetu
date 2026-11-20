package com.example.udyamsetubackend.enquiry.dto;

import com.example.udyamsetubackend.enquiry.entity.Enquiry;
import com.example.udyamsetubackend.enquiry.enums.EnquirySource;
import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Outbound representation of a saved enquiry.
 */
public record EnquiryResponse(
        Long id,
        Long customerId,
        Long serviceId,
        String customerName,
        String contactNumber,
        EnquirySource source,
        String requestedService,
        String requirements,
        String occasion,
        BigDecimal budget,
        LocalDate expectedDate,
        LocalDate followUpDate,
        LocalTime followUpTime,
        String followUpNotes,
        EnquiryStatus status,
        LocalDateTime createdAt
) {

    public static EnquiryResponse from(Enquiry enquiry) {
        return new EnquiryResponse(
                enquiry.getId(),
                enquiry.getCustomer() == null ? null : enquiry.getCustomer().getId(),
                enquiry.getService() == null ? null : enquiry.getService().getId(),
                enquiry.getCustomerName(),
                enquiry.getContactNumber(),
                enquiry.getSource(),
                enquiry.getRequestedService(),
                enquiry.getRequirements(),
                enquiry.getOccasion(),
                enquiry.getBudget(),
                enquiry.getExpectedDate(),
                enquiry.getFollowUpDate(),
                enquiry.getFollowUpTime(),
                enquiry.getFollowUpNotes(),
                enquiry.getStatus(),
                enquiry.getCreatedAt());
    }
}
