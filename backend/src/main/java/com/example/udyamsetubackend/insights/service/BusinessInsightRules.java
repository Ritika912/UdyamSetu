package com.example.udyamsetubackend.insights.service;

import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.insights.dto.BusinessInsightsResponse.DemandInsight;
import com.example.udyamsetubackend.insights.dto.BusinessInsightsResponse.TrendInsight;
import com.example.udyamsetubackend.insights.dto.BusinessInsightsResponse.TrendPoint;
import com.example.udyamsetubackend.insights.enums.TrendDirection;
import com.example.udyamsetubackend.insights.enums.TrendGranularity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.example.udyamsetubackend.insights.service.InsightThresholds.HIGH_LOST_RATE;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_REPEATED_UNFULFILLED;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_SAMPLE_SIZE;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_TREND_BUCKETS;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_TREND_ENQUIRIES;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.TREND_CHANGE_RATIO;

/**
 * Business rules that turn aggregated counts into insights. Pure functions with no framework or database
 * access, so every threshold can be unit-tested.
 *
 * <p>UNFULFILLED (the business could not provide the service) and LOST (the customer did not proceed for
 * another reason) are kept apart throughout. No rule adds them together.
 */
public final class BusinessInsightRules {

    private static final int RATE_SCALE = 3;

    private static final Comparator<ServiceDemand> BY_TOTAL_DESC_THEN_NAME =
            Comparator.comparingLong(ServiceDemand::total).reversed()
                    .thenComparing(ServiceDemand::serviceName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(ServiceDemand::serviceKey);

    private BusinessInsightRules() {
    }

    /** part / total, rounded to three decimal places. Returns zero when total is zero. */
    public static BigDecimal rate(long part, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(part).divide(BigDecimal.valueOf(total), RATE_SCALE, RoundingMode.HALF_UP);
    }

    /** The {@code topCount} services with the most enquiries, most requested first. */
    public static List<DemandInsight> mostRequested(List<ServiceDemand> demand, long totalEnquiries, int topCount) {
        return demand.stream()
                .sorted(BY_TOTAL_DESC_THEN_NAME)
                .limit(topCount)
                .map(s -> build(s, "Requested in " + s.total() + " of " + totalEnquiries + " enquiries in this period."))
                .toList();
    }

    /**
     * Services that the business could not provide at least {@link InsightThresholds#MIN_REPEATED_UNFULFILLED}
     * times. The count is reported even for small samples; the rate is suppressed unless the sample is large
     * enough.
     */
    public static List<DemandInsight> repeatedUnfulfilled(List<ServiceDemand> demand) {
        Comparator<ServiceDemand> byUnfulfilledDesc =
                Comparator.comparingLong((ServiceDemand s) -> s.count(EnquiryStatus.UNFULFILLED)).reversed();
        return demand.stream()
                .filter(s -> s.count(EnquiryStatus.UNFULFILLED) >= MIN_REPEATED_UNFULFILLED)
                .sorted(byUnfulfilledDesc.thenComparing(BY_TOTAL_DESC_THEN_NAME))
                .map(s -> build(s, "UNFULFILLED " + s.count(EnquiryStatus.UNFULFILLED)
                        + " times: the business could not provide this service ("
                        + s.total() + " enquiries in total)."))
                .toList();
    }

    /**
     * Services whose LOST share is at or above {@link InsightThresholds#HIGH_LOST_RATE}, considering only
     * services with at least {@link InsightThresholds#MIN_SAMPLE_SIZE} enquiries.
     */
    public static List<DemandInsight> highLostRate(List<ServiceDemand> demand) {
        Comparator<ServiceDemand> byLostRateDesc =
                Comparator.comparing((ServiceDemand s) -> rate(s.count(EnquiryStatus.LOST), s.total())).reversed();
        return demand.stream()
                .filter(s -> s.total() >= MIN_SAMPLE_SIZE)
                .filter(BusinessInsightRules::isHighLost)
                .sorted(byLostRateDesc.thenComparing(BY_TOTAL_DESC_THEN_NAME))
                .map(s -> build(s, "LOST in " + s.count(EnquiryStatus.LOST) + " of " + s.total()
                        + " enquiries (" + percent(rate(s.count(EnquiryStatus.LOST), s.total()))
                        + "): the customer did not proceed for another reason. Threshold "
                        + percent(HIGH_LOST_RATE) + "."))
                .toList();
    }

    /**
     * Number of services with a high LOST share that were left out because they have fewer than
     * {@link InsightThresholds#MIN_SAMPLE_SIZE} enquiries. Used for a transparency note.
     */
    public static long smallSampleHighLostCount(List<ServiceDemand> demand) {
        return demand.stream()
                .filter(s -> s.total() > 0 && s.total() < MIN_SAMPLE_SIZE)
                .filter(BusinessInsightRules::isHighLost)
                .count();
    }

    /**
     * Compares the first and second halves of the period. {@code orderedBuckets} must be in chronological
     * order and include empty buckets.
     */
    public static TrendInsight trend(TrendGranularity granularity, Map<String, Long> orderedBuckets) {
        List<TrendPoint> points = orderedBuckets.entrySet().stream()
                .map(e -> new TrendPoint(e.getKey(), e.getValue()))
                .toList();
        int bucketCount = points.size();
        long total = points.stream().mapToLong(TrendPoint::count).sum();
        String unit = granularity == TrendGranularity.DAY ? "days" : "months";

        if (total < MIN_TREND_ENQUIRIES || bucketCount < MIN_TREND_BUCKETS) {
            return new TrendInsight(granularity, TrendDirection.INSUFFICIENT_DATA, 0, 0, points,
                    "Needs at least " + MIN_TREND_ENQUIRIES + " enquiries across " + MIN_TREND_BUCKETS
                            + " " + unit + " to judge a trend; found " + total + " across "
                            + bucketCount + " " + unit + ".");
        }

        int splitIndex = bucketCount / 2;
        long previous = points.subList(0, splitIndex).stream().mapToLong(TrendPoint::count).sum();
        long recent = total - previous;

        BigDecimal multiplierUp = BigDecimal.ONE.add(TREND_CHANGE_RATIO);
        BigDecimal multiplierDown = BigDecimal.ONE.subtract(TREND_CHANGE_RATIO);
        BigDecimal previousValue = BigDecimal.valueOf(previous);
        BigDecimal recentValue = BigDecimal.valueOf(recent);

        TrendDirection direction;
        if (recentValue.compareTo(previousValue.multiply(multiplierUp)) > 0) {
            direction = TrendDirection.RISING;
        } else if (recentValue.compareTo(previousValue.multiply(multiplierDown)) < 0) {
            direction = TrendDirection.FALLING;
        } else {
            direction = TrendDirection.STABLE;
        }

        String reason = "First half of the period: " + previous + " enquiries; second half: " + recent
                + " enquiries. A change of more than " + percent(TREND_CHANGE_RATIO) + " is treated as "
                + "rising or falling.";
        return new TrendInsight(granularity, direction, previous, recent, points, reason);
    }

    private static boolean isHighLost(ServiceDemand s) {
        return s.total() > 0 && rate(s.count(EnquiryStatus.LOST), s.total()).compareTo(HIGH_LOST_RATE) >= 0;
    }

    private static DemandInsight build(ServiceDemand s, String reason) {
        long total = s.total();
        boolean sufficient = total >= MIN_SAMPLE_SIZE;
        long unfulfilled = s.count(EnquiryStatus.UNFULFILLED);
        long lost = s.count(EnquiryStatus.LOST);
        return new DemandInsight(
                s.serviceName(),
                s.serviceKey(),
                s.source(),
                total,
                unfulfilled,
                lost,
                sufficient ? rate(unfulfilled, total) : null,
                sufficient ? rate(lost, total) : null,
                sufficient,
                fullStatusMap(s.countsByStatus()),
                reason);
    }

    /** Copies the counts into an EnumMap that includes every status, with zero for missing ones. */
    public static Map<EnquiryStatus, Long> fullStatusMap(Map<EnquiryStatus, Long> counts) {
        Map<EnquiryStatus, Long> full = new EnumMap<>(EnquiryStatus.class);
        for (EnquiryStatus status : EnquiryStatus.values()) {
            full.put(status, counts.getOrDefault(status, 0L));
        }
        return full;
    }

    /** Formats a ratio as a percentage with one decimal place, for example {@code 40.0%}. */
    static String percent(BigDecimal ratio) {
        return ratio.movePointRight(2).setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
