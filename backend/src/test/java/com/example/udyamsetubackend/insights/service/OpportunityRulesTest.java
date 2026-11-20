package com.example.udyamsetubackend.insights.service;

import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.insights.dto.OpportunityInsightsResponse.Opportunity;
import com.example.udyamsetubackend.insights.enums.OpportunityCategory;
import com.example.udyamsetubackend.insights.enums.OpportunityType;
import com.example.udyamsetubackend.insights.enums.ServiceSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.example.udyamsetubackend.insights.service.InsightThresholds.MAX_OPPORTUNITIES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure rule tests for opportunities. No Spring, no database.
 */
class OpportunityRulesTest {

    private static final LocalDate FROM = LocalDate.of(2026, 10, 21);
    private static final LocalDate TO = LocalDate.of(2026, 11, 19);

    /** Builds demand from counts. Recent count is given explicitly so the half-period split is clear. */
    private static ServiceDemand demand(String name, ServiceSource source, Boolean active,
                                        int unfulfilled, int lost, int other, long recent) {
        Map<EnquiryStatus, Long> counts = new EnumMap<>(EnquiryStatus.class);
        if (unfulfilled > 0) counts.put(EnquiryStatus.UNFULFILLED, (long) unfulfilled);
        if (lost > 0) counts.put(EnquiryStatus.LOST, (long) lost);
        if (other > 0) counts.put(EnquiryStatus.NEW, (long) other);
        String key = source == ServiceSource.CATALOGUE
                ? ServiceKeys.forCatalogueService(name.hashCode() & 0xffff)
                : ServiceKeys.forFreeText(name);
        return new ServiceDemand(key, source, name, active, counts, recent);
    }

    private static ServiceDemand catalogue(String name, int unfulfilled, int lost, int other) {
        return demand(name, ServiceSource.CATALOGUE, true, unfulfilled, lost, other, 0);
    }

    private static Optional<Opportunity> only(List<Opportunity> list, OpportunityType type) {
        return list.stream().filter(o -> o.type() == type).findFirst();
    }

    private static List<Opportunity> evaluate(long totalEnquiries, ServiceDemand... services) {
        return OpportunityRules.evaluate(List.of(services), totalEnquiries, FROM, TO);
    }

    // --- Repeated UNFULFILLED -------------------------------------------------------------------------------

    @Test
    void repeatedUnfulfilledIsRecommendedWhenCountAndSampleAreSufficient() {
        List<Opportunity> result = evaluate(20, catalogue("Bridal Makeup", 3, 0, 2));

        Opportunity op = only(result, OpportunityType.REPEATED_UNFULFILLED).orElseThrow();
        assertEquals(OpportunityCategory.SUPPLY_GAP, op.category());
        assertEquals(3, op.supportingEnquiryCount());
        assertEquals("Repeated unmet demand: Bridal Makeup", op.title());
        assertEquals(FROM, op.periodFrom());
        assertEquals(TO, op.periodTo());
        assertTrue(op.explanation().contains("3 of 5"), op.explanation());
    }

    @Test
    void repeatedUnfulfilledNeedsAtLeastTheRepeatThreshold() {
        // 1 UNFULFILLED is a single event, not a pattern.
        assertFalse(only(evaluate(20, catalogue("Saree Draping", 1, 0, 9)), OpportunityType.REPEATED_UNFULFILLED)
                .isPresent());
    }

    @Test
    void repeatedUnfulfilledNeedsMinimumSampleEvenWhenRepeated() {
        // 2 of 4 is a repeat, but 4 enquiries is below the 5-enquiry sample minimum.
        assertFalse(only(evaluate(20, catalogue("Saree Draping", 2, 0, 2)), OpportunityType.REPEATED_UNFULFILLED)
                .isPresent());
    }

    @Test
    void lostEnquiriesAreNeverCountedAsUnfulfilledDemand() {
        List<Opportunity> result = evaluate(20, catalogue("Facial", 0, 6, 0));

        assertFalse(only(result, OpportunityType.REPEATED_UNFULFILLED).isPresent());
        assertTrue(only(result, OpportunityType.HIGH_LOST_RATE).isPresent());
    }

    @Test
    void freeTextUnfulfilledActionNeverPromisesAutomaticCatalogueCreation() {
        Opportunity op = only(evaluate(20, demand("Mehendi", ServiceSource.FREE_TEXT, null, 3, 0, 2, 0)),
                OpportunityType.REPEATED_UNFULFILLED).orElseThrow();

        assertEquals(ServiceSource.FREE_TEXT, op.serviceSource());
        assertEquals(null, op.serviceActive());
        assertTrue(op.suggestedAction().contains("does not create catalogue entries"), op.suggestedAction());
    }

    @Test
    void deactivatedCatalogueServiceIsFlaggedInTheExplanation() {
        Opportunity op = only(evaluate(20, demand("Old Pack", ServiceSource.CATALOGUE, false, 3, 0, 2, 0)),
                OpportunityType.REPEATED_UNFULFILLED).orElseThrow();

        assertEquals(false, op.serviceActive());
        assertTrue(op.explanation().contains("deactivated"), op.explanation());
    }

    // --- Frequently requested -------------------------------------------------------------------------------

    @Test
    void frequentlyRequestedNeedsCountAndShareOfThePeriod() {
        // 5 of 20 = 25%, above the 10% share threshold.
        Opportunity op = only(evaluate(20, catalogue("Haircut", 0, 0, 5)), OpportunityType.FREQUENTLY_REQUESTED)
                .orElseThrow();

        assertEquals(OpportunityCategory.DEMAND_PROMOTION, op.category());
        assertEquals(5, op.supportingEnquiryCount());
    }

    @Test
    void frequentlyRequestedIsNotReportedWhenShareIsLow() {
        // 5 of 100 = 5%, below the 10% threshold.
        assertFalse(only(evaluate(100, catalogue("Haircut", 0, 0, 5)), OpportunityType.FREQUENTLY_REQUESTED)
                .isPresent());
    }

    @Test
    void frequentlyRequestedNeedsMinimumCount() {
        // 4 of 8 = 50% share, but 4 requests is below the minimum of 5.
        assertFalse(only(evaluate(8, catalogue("Haircut", 0, 0, 4)), OpportunityType.FREQUENTLY_REQUESTED)
                .isPresent());
    }

    @Test
    void frequentlyRequestedHandlesAnEmptyPeriod() {
        assertFalse(only(evaluate(0, catalogue("Haircut", 0, 0, 5)), OpportunityType.FREQUENTLY_REQUESTED)
                .isPresent());
    }

    // --- High LOST rate -------------------------------------------------------------------------------------

    @Test
    void highLostRateIsReportedAtExactlyTheThreshold() {
        // 2 of 5 = 0.400, equal to HIGH_LOST_RATE.
        Opportunity op = only(evaluate(20, catalogue("Facial", 0, 2, 3)), OpportunityType.HIGH_LOST_RATE).orElseThrow();

        assertEquals(OpportunityCategory.CONVERSION_RISK, op.category());
        assertEquals(2, op.supportingEnquiryCount());
        assertTrue(op.explanation().contains("not unmet service demand"), op.explanation());
    }

    @Test
    void highLostRateNeedsMinimumSample() {
        // 4 of 4 LOST, but 4 enquiries is below the sample minimum.
        assertFalse(only(evaluate(20, catalogue("Facial", 0, 4, 0)), OpportunityType.HIGH_LOST_RATE).isPresent());
    }

    @Test
    void highUnfulfilledWithLowLostIsNotAHighLostOpportunity() {
        List<Opportunity> result = evaluate(20, catalogue("Bridal Makeup", 5, 0, 0));

        assertFalse(only(result, OpportunityType.HIGH_LOST_RATE).isPresent());
        assertTrue(only(result, OpportunityType.REPEATED_UNFULFILLED).isPresent());
    }

    // --- Increasing demand ----------------------------------------------------------------------------------

    @Test
    void increasingDemandIsReportedWhenRecentHalfExceedsEarlierByMoreThanRatio() {
        // total 5, recent 4, previous 1: 4 > 1 * 1.25.
        Opportunity op = only(evaluate(20, demand("Nail Art", ServiceSource.CATALOGUE, true, 0, 0, 5, 4)),
                OpportunityType.INCREASING_DEMAND).orElseThrow();

        assertEquals(OpportunityCategory.GROWTH, op.category());
        assertEquals(4, op.recentCount());
        assertEquals(1, op.previousCount());
    }

    @Test
    void increasingDemandNotReportedWhenChangeIsWithinRatio() {
        // total 6, recent 3, previous 3: 3 is not above 3 * 1.25.
        assertFalse(only(evaluate(20, demand("Nail Art", ServiceSource.CATALOGUE, true, 0, 0, 6, 3)),
                OpportunityType.INCREASING_DEMAND).isPresent());
    }

    @Test
    void demandThatAppearsFromNothingIsNotCalledAnIncreasingTrend() {
        // total 5, all recent, none before: new demand, not a trend. Requires a non-zero baseline.
        assertFalse(only(evaluate(20, demand("Nail Art", ServiceSource.CATALOGUE, true, 0, 0, 5, 5)),
                OpportunityType.INCREASING_DEMAND).isPresent());
    }

    @Test
    void increasingDemandNeedsMinimumRecentCount() {
        // total 5, recent 2, previous 3: a rise, but only 2 recent enquiries.
        assertFalse(only(evaluate(20, demand("Nail Art", ServiceSource.CATALOGUE, true, 0, 0, 5, 2)),
                OpportunityType.INCREASING_DEMAND).isPresent());
    }

    // --- Ordering, cap and small-sample notes ---------------------------------------------------------------

    @Test
    void resultsAreMostSignificantFirstAndCappedAtTheMaximum() {
        List<ServiceDemand> services = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            services.add(demand("Service " + i, ServiceSource.FREE_TEXT, null, 5, 0, 0, 0));
        }

        List<Opportunity> result = OpportunityRules.evaluate(services, 60, FROM, TO);

        assertEquals(MAX_OPPORTUNITIES, result.size());
        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i - 1).supportingEnquiryCount() >= result.get(i).supportingEnquiryCount());
        }
    }

    @Test
    void mostSignificantOpportunityComesFirst() {
        // Total 100 keeps the 6-enquiry service below the 10% frequent-share threshold, so only the repeat rule fires.
        List<Opportunity> result = evaluate(100,
                demand("Small Repeat", ServiceSource.FREE_TEXT, null, 2, 0, 2, 0),   // 2 unfulfilled, total 4: suppressed
                demand("Big Repeat", ServiceSource.FREE_TEXT, null, 6, 0, 0, 0));   // 6 unfulfilled, total 6

        assertEquals(1, result.size(), "the small-sample service must not be recommended");
        assertEquals("Repeated unmet demand: Big Repeat", result.get(0).title());
    }

    @Test
    void smallSampleUnfulfilledCountReportsRepeatsThatWereSuppressed() {
        List<ServiceDemand> services = List.of(
                catalogue("Saree", 2, 0, 1),        // repeated, total 3: suppressed
                catalogue("Bridal", 3, 0, 4),       // repeated, total 7: reported
                catalogue("Facial", 1, 0, 0));      // once only: not counted

        assertEquals(1, OpportunityRules.smallSampleUnfulfilledCount(services));
    }

    @Test
    void optionalMapsAreAlwaysFullyPopulated() {
        Opportunity op = only(evaluate(20, catalogue("Bridal", 3, 0, 2)), OpportunityType.REPEATED_UNFULFILLED)
                .orElseThrow();

        assertEquals(EnquiryStatus.values().length, op.statusCounts().size());
        assertEquals(3L, op.statusCounts().get(EnquiryStatus.UNFULFILLED));
        assertEquals(0L, op.statusCounts().get(EnquiryStatus.CONVERTED));
    }
}
