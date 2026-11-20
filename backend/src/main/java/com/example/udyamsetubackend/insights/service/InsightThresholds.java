package com.example.udyamsetubackend.insights.service;

import java.math.BigDecimal;

/**
 * Tunable constants for business insights. Change values here; every rule reads them from this class.
 *
 * <p>Sample-size guards exist so that a percentage is never shown for a handful of enquiries.
 */
public final class InsightThresholds {

    /** Number of calendar days in the default reporting window, including today. */
    public static final int LAST_DAYS_WINDOW = 30;

    /** How many services to list in the most-requested section. */
    public static final int TOP_SERVICE_COUNT = 5;

    /** Minimum number of UNFULFILLED enquiries for a service to count as repeated unfulfilled demand. */
    public static final int MIN_REPEATED_UNFULFILLED = 2;

    /** Minimum enquiries for a service before any rate (UNFULFILLED or LOST) is reported. */
    public static final int MIN_SAMPLE_SIZE = 5;

    /** LOST share at or above which a service is flagged. */
    public static final BigDecimal HIGH_LOST_RATE = new BigDecimal("0.40");

    /** Minimum enquiries in the period before a demand trend is judged. */
    public static final int MIN_TREND_ENQUIRIES = 10;

    /** Minimum number of buckets (days or months) before a demand trend is judged. */
    public static final int MIN_TREND_BUCKETS = 4;

    /** Relative change between the two halves of the period needed to call a trend rising or falling. */
    public static final BigDecimal TREND_CHANGE_RATIO = new BigDecimal("0.25");

    /** Opportunity: minimum enquiries for a service to count as frequently requested. */
    public static final int MIN_FREQUENT_REQUESTS = 5;

    /** Opportunity: minimum share of the period's enquiries for a service to count as frequently requested. */
    public static final BigDecimal MIN_FREQUENT_SHARE = new BigDecimal("0.10");

    /** Opportunity: minimum enquiries in the recent half before demand is called increasing. */
    public static final int MIN_RECENT_FOR_GROWTH = 3;

    /**
     * Opportunity: minimum enquiries in the earlier half. A rise from zero is new demand, not a trend, so a
     * growth claim needs a baseline.
     */
    public static final int MIN_PREVIOUS_FOR_GROWTH = 1;

    /** Opportunity: maximum recommendations returned, most significant first. */
    public static final int MAX_OPPORTUNITIES = 10;

    private InsightThresholds() {
    }
}
