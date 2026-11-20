package com.example.udyamsetubackend.dashboard.dto;

/**
 * Dashboard metrics snapshot returned by {@code GET /api/dashboard}.
 */
public record DashboardResponse(
    long totalEnquiries,
    long newEnquiries,
    long followUpsDueToday,
    long overdueFollowUps
) {
}
