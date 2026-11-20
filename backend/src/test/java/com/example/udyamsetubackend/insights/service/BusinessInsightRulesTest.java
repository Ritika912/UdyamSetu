package com.example.udyamsetubackend.insights.service;

import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.insights.dto.BusinessInsightsResponse.DemandInsight;
import com.example.udyamsetubackend.insights.dto.BusinessInsightsResponse.TrendInsight;
import com.example.udyamsetubackend.insights.enums.TrendDirection;
import com.example.udyamsetubackend.insights.enums.TrendGranularity;
import com.example.udyamsetubackend.insights.enums.ServiceSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure rule tests. No Spring, no database.
 */
class BusinessInsightRulesTest {

    private static ServiceDemand stats(String name, int unfulfilled, int lost, int other) {
        Map<EnquiryStatus, Long> counts = new EnumMap<>(EnquiryStatus.class);
        if (unfulfilled > 0) counts.put(EnquiryStatus.UNFULFILLED, (long) unfulfilled);
        if (lost > 0) counts.put(EnquiryStatus.LOST, (long) lost);
        if (other > 0) counts.put(EnquiryStatus.NEW, (long) other);
        return new ServiceDemand(ServiceKeys.forFreeText(name), ServiceSource.FREE_TEXT, name, null, counts, 0);
    }

    private static Map<String, Long> buckets(long... counts) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (int i = 0; i < counts.length; i++) {
            map.put("b" + i, counts[i]);
        }
        return map;
    }

    @Test
    void rateIsZeroWhenTotalIsZeroAndRoundsToThreeDecimals() {
        assertEquals(new BigDecimal("0.000"), BusinessInsightRules.rate(0, 0));
        assertEquals(new BigDecimal("0.333"), BusinessInsightRules.rate(1, 3));
        assertEquals(new BigDecimal("0.400"), BusinessInsightRules.rate(2, 5));
    }

    @Test
    void mostRequestedRanksByTotalThenNameAndLimitsToTopCount() {
        List<ServiceDemand>stats = List.of(
                stats("Bridal Makeup", 0, 0, 4),
                stats("Haircut", 0, 0, 6),
                stats("Manicure", 0, 0, 4));

        List<DemandInsight> top = BusinessInsightRules.mostRequested(stats, 14, 2);

        assertEquals(2, top.size());
        assertEquals("Haircut", top.get(0).service());
        assertEquals("Bridal Makeup", top.get(1).service(), "ties are ordered by name");
    }

    @Test
    void mostRequestedReasonShowsShareOfAllEnquiries() {
        List<DemandInsight> top = BusinessInsightRules.mostRequested(List.of(stats("Haircut", 0, 0, 6)), 14, 5);

        assertTrue(top.get(0).reason().contains("6 of 14"), top.get(0).reason());
    }

    @Test
    void repeatedUnfulfilledIncludesOnlyServicesAtOrAboveTheRepeatThreshold() {
        List<ServiceDemand>stats = List.of(
                stats("Saree Draping", 2, 0, 3),   // repeated: included
                stats("Tattoo", 1, 0, 9),          // once only: excluded
                stats("Facial", 0, 5, 0));         // LOST only: excluded (LOST is not UNFULFILLED)

        List<DemandInsight> result = BusinessInsightRules.repeatedUnfulfilled(stats);

        assertEquals(1, result.size());
        assertEquals("Saree Draping", result.get(0).service());
        assertEquals(2, result.get(0).unfulfilledCount());
    }

    @Test
    void repeatedUnfulfilledSuppressesRateForSmallSampleButKeepsTheCount() {
        DemandInsight insight = BusinessInsightRules.repeatedUnfulfilled(List.of(stats("Saree", 2, 0, 0))).get(0);

        assertEquals(2, insight.unfulfilledCount());
        assertEquals(2, insight.totalEnquiries());
        assertFalse(insight.sampleSufficient());
        assertNull(insight.unfulfilledRate(), "a rate from 2 enquiries would be misleading");
    }

    @Test
    void repeatedUnfulfilledReportsRateWhenSampleIsSufficient() {
        DemandInsight insight = BusinessInsightRules.repeatedUnfulfilled(List.of(stats("Saree", 2, 0, 3))).get(0);

        assertTrue(insight.sampleSufficient());
        assertEquals(new BigDecimal("0.400"), insight.unfulfilledRate());
    }

    @Test
    void highLostRateIncludesServiceExactlyAtThreshold() {
        // 2 of 5 = 0.400, which equals HIGH_LOST_RATE.
        List<DemandInsight> result = BusinessInsightRules.highLostRate(List.of(stats("Facial", 0, 2, 3)));

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("0.400"), result.get(0).lostRate());
    }

    @Test
    void highLostRateDoesNotFlagServicesWithHighUnfulfilledButLowLost() {
        // UNFULFILLED is a supply problem, not a lost customer. It must not appear as a high-LOST service.
        List<ServiceDemand>stats = List.of(stats("Bridal Makeup", 5, 0, 0));

        assertTrue(BusinessInsightRules.highLostRate(stats).isEmpty());
        assertEquals(1, BusinessInsightRules.repeatedUnfulfilled(stats).size());
    }

    @Test
    void highLostRateRequiresMinimumSampleAndCountsSuppressedServices() {
        List<ServiceDemand>stats = List.of(stats("Small Pack", 0, 4, 0));

        assertTrue(BusinessInsightRules.highLostRate(stats).isEmpty());
        assertEquals(1, BusinessInsightRules.smallSampleHighLostCount(stats));
    }

    @Test
    void highLostRateSortsByLostShareDescending() {
        List<DemandInsight> result = BusinessInsightRules.highLostRate(List.of(
                stats("Moderate", 0, 2, 3),   // 0.400
                stats("Severe", 0, 4, 1)));   // 0.800

        assertEquals("Severe", result.get(0).service());
        assertEquals("Moderate", result.get(1).service());
    }

    @Test
    void trendIsInsufficientWhenTooFewEnquiries() {
        TrendInsight trend = BusinessInsightRules.trend(TrendGranularity.DAY,
                buckets(0, 0, 0, 0, 0, 0, 0, 0, 3, 6));

        assertEquals(TrendDirection.INSUFFICIENT_DATA, trend.direction());
    }

    @Test
    void trendIsInsufficientWhenTooFewBuckets() {
        TrendInsight trend = BusinessInsightRules.trend(TrendGranularity.MONTH, buckets(10, 40, 30));

        assertEquals(TrendDirection.INSUFFICIENT_DATA, trend.direction());
        assertTrue(trend.reason().contains("months"), trend.reason());
    }

    @Test
    void trendIsRisingWhenSecondHalfExceedsFirstByMoreThanTheRatio() {
        // First half 4, second half 12: 12 > 4 * 1.25.
        TrendInsight trend = BusinessInsightRules.trend(TrendGranularity.DAY,
                buckets(1, 1, 1, 1, 3, 3, 3, 3));

        assertEquals(TrendDirection.RISING, trend.direction());
        assertEquals(4, trend.previousPeriodCount());
        assertEquals(12, trend.recentPeriodCount());
    }

    @Test
    void trendIsFallingWhenSecondHalfIsBelowFirstByMoreThanTheRatio() {
        TrendInsight trend = BusinessInsightRules.trend(TrendGranularity.DAY,
                buckets(3, 3, 3, 3, 1, 1, 1, 1));

        assertEquals(TrendDirection.FALLING, trend.direction());
    }

    @Test
    void trendIsStableWithinTheRatio() {
        // First half 20, second half 21: within 25% either way.
        TrendInsight trend = BusinessInsightRules.trend(TrendGranularity.DAY,
                buckets(5, 5, 5, 5, 5, 5, 5, 6));

        assertEquals(TrendDirection.STABLE, trend.direction());
    }

    @Test
    void trendReturnsEveryBucketInOrderIncludingEmptyOnes() {
        List<String> keys = BusinessInsightRules.trend(TrendGranularity.DAY, buckets(1, 0, 2, 0))
                .points().stream().map(p -> p.bucket()).toList();

        assertEquals(List.of("b0", "b1", "b2", "b3"), keys);
    }
}
