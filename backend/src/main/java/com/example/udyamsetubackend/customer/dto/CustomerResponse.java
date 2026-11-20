package com.example.udyamsetubackend.customer.dto;

import com.example.udyamsetubackend.customer.entity.Customer;

import java.time.LocalDateTime;

/**
 * Outbound representation of a customer.
 */
public record CustomerResponse(
        Long id,
        String customerName,
        String contactNumber,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCustomerName(),
                customer.getContactNumber(),
                customer.getNotes(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }
}
