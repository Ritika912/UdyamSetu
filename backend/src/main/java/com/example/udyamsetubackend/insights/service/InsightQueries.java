package com.example.udyamsetubackend.insights.service;

import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import com.example.udyamsetubackend.enquiry.repository.StatusCountRow;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * Aggregation helpers shared by the insight services.
 */
final class InsightQueries {

    private InsightQueries() {
    }

    /** Enquiry count per status for [from, to), with every status present (zero when absent). */
    static Map<EnquiryStatus, Long> statusCounts(EnquiryRepository repository, LocalDateTime from, LocalDateTime to) {
        Map<EnquiryStatus, Long> counts = new EnumMap<>(EnquiryStatus.class);
        for (StatusCountRow row : repository.countStatusesBetween(from, to)) {
            counts.merge(row.getStatus(), row.getTotal(), Long::sum);
        }
        return BusinessInsightRules.fullStatusMap(counts);
    }
}
