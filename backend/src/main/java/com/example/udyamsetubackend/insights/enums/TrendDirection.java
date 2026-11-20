package com.example.udyamsetubackend.insights.enums;

/**
 * Direction of demand over the reporting period. INSUFFICIENT_DATA is returned when the sample is too small
 * to judge a trend.
 */
public enum TrendDirection {
    RISING,
    FALLING,
    STABLE,
    INSUFFICIENT_DATA
}
