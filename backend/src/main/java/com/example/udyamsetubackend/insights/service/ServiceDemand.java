package com.example.udyamsetubackend.insights.service;

import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.insights.enums.ServiceSource;

import java.util.Map;

/**
 * Demand for one service identity (see {@link ServiceKeys}) within a period, as input to the opportunity rules.
 *
 * @param serviceKey   {@code SERVICE:<id>} or {@code TEXT:<normalised>}
 * @param source       catalogue or free text
 * @param serviceName  display name. For catalogue services this is the current catalogue name. For free text it
 *                     is an original spelling, preserved as typed.
 * @param active       catalogue active flag; null for free text
 * @param countsByStatus enquiries per status (may omit statuses that did not occur)
 * @param recentCount  enquiries in the recent half of the period
 */
public record ServiceDemand(
        String serviceKey,
        ServiceSource source,
        String serviceName,
        Boolean active,
        Map<EnquiryStatus, Long> countsByStatus,
        long recentCount
) {

    public long total() {
        return countsByStatus.values().stream().mapToLong(Long::longValue).sum();
    }

    public long count(EnquiryStatus status) {
        return countsByStatus.getOrDefault(status, 0L);
    }

    /** Enquiries in the earlier half of the period. */
    public long previousCount() {
        return total() - recentCount;
    }
}
