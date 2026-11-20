package com.example.udyamsetubackend.insights.dto;

import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.insights.enums.InsightPeriod;
import com.example.udyamsetubackend.insights.enums.ServiceSource;
import com.example.udyamsetubackend.insights.enums.TrendDirection;
import com.example.udyamsetubackend.insights.enums.TrendGranularity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Business insights for one reporting period. Every insight carries the counts it is based on and a
 * plain-language reason, so the figures can be checked.
 *
 * @param from                       first day of the window (LAST_30_DAYS: today minus 29 days; ALL_TIME: date of
 *                                   the oldest enquiry)
 * @param to                         last day of the window (inclusive)
 * @param totalEnquiries             enquiries created in the period
 * @param withoutRequestedService    enquiries with no requested-service text (excluded from service insights)
 * @param statusCounts               enquiries per status in the period (every status is present)
 * @param notes                      explanations of suppressed or limited results
 * @param thresholds                 the thresholds applied
 */
public record BusinessInsightsResponse(
        InsightPeriod period,
        LocalDate from,
        LocalDate to,
        long totalEnquiries,
        long withoutRequestedService,
        Map<EnquiryStatus, Long> statusCounts,
        List<DemandInsight> mostRequestedServices,
        List<DemandInsight> repeatedUnfulfilledDemand,
        List<DemandInsight> highLostRateServices,
        TrendInsight demandTrend,
        List<String> notes,
        Thresholds thresholds
) {

    /**
     * Demand for one requested service. Rates are null when the sample is below the minimum.
     */
    public record DemandInsight(
            String service,
            String serviceKey,
            ServiceSource serviceSource,
            long totalEnquiries,
            long unfulfilledCount,
            long lostCount,
            BigDecimal unfulfilledRate,
            BigDecimal lostRate,
            boolean sampleSufficient,
            Map<EnquiryStatus, Long> statusCounts,
            String reason
    ) {
    }

    /**
     * Enquiry volume over time. {@code previousPeriodCount} and {@code recentPeriodCount} are the first and
     * second halves of the window.
     */
    public record TrendInsight(
            TrendGranularity granularity,
            TrendDirection direction,
            long previousPeriodCount,
            long recentPeriodCount,
            List<TrendPoint> points,
            String reason
    ) {
    }

    public record TrendPoint(String bucket, long count) {
    }

    public record Thresholds(
            int lastDaysWindow,
            int topServiceCount,
            int minRepeatedUnfulfilled,
            int minSampleSize,
            BigDecimal highLostRate,
            int minTrendEnquiries,
            int minTrendBuckets,
            BigDecimal trendChangeRatio
    ) {
    }
}
