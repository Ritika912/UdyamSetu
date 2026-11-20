package com.example.udyamsetubackend.insights.service;

import com.example.udyamsetubackend.insights.enums.InsightPeriod;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

/**
 * A reporting window in whole calendar days, shared by all insight features.
 *
 * @param fromDate first day included
 * @param toDate   last day included
 */
public record InsightWindow(LocalDate fromDate, LocalDate toDate) {

    /**
     * LAST_30_DAYS covers today and the 29 days before it. ALL_TIME starts at the oldest enquiry, so no stored
     * record is excluded; with no enquiries the window is just today.
     */
    public static InsightWindow of(InsightPeriod period, LocalDate today,
                                   Supplier<LocalDateTime> earliestCreatedAt) {
        if (period == InsightPeriod.ALL_TIME) {
            LocalDateTime earliest = earliestCreatedAt.get();
            return new InsightWindow(earliest == null ? today : earliest.toLocalDate(), today);
        }
        return new InsightWindow(today.minusDays(InsightThresholds.LAST_DAYS_WINDOW - 1L), today);
    }

    /** Inclusive lower bound for {@code created_at} comparisons. */
    public LocalDateTime from() {
        return fromDate.atStartOfDay();
    }

    /** Exclusive upper bound: the day after {@code toDate}, at midnight. */
    public LocalDateTime toExclusive() {
        return toDate.plusDays(1).atStartOfDay();
    }

    /**
     * Start of the recent half of the window. Enquiries at or after this instant are "recent"; the earlier
     * part of the window is "previous". Matches the first-half/second-half split used for trends.
     */
    public LocalDateTime recentHalfStart() {
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        return fromDate.plusDays(days / 2).atStartOfDay();
    }
}
