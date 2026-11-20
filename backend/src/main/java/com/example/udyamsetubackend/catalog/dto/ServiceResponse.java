package com.example.udyamsetubackend.catalog.dto;

import com.example.udyamsetubackend.catalog.entity.ServiceOffering;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Outbound representation of a catalogue service.
 */
public record ServiceResponse(
        Long id,
        String name,
        String description,
        String category,
        BigDecimal price,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ServiceResponse from(ServiceOffering service) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getCategory(),
                service.getPrice(),
                service.isActive(),
                service.getCreatedAt(),
                service.getUpdatedAt());
    }
}
