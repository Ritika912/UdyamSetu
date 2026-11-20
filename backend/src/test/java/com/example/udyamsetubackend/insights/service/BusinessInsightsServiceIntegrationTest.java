package com.example.udyamsetubackend.insights.service;

import com.example.udyamsetubackend.catalog.entity.ServiceOffering;
import com.example.udyamsetubackend.catalog.repository.ServiceOfferingRepository;
import com.example.udyamsetubackend.customer.entity.Customer;
import com.example.udyamsetubackend.customer.repository.CustomerRepository;
import com.example.udyamsetubackend.enquiry.entity.Enquiry;
import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import com.example.udyamsetubackend.insights.dto.BusinessInsightsResponse;
import com.example.udyamsetubackend.insights.dto.BusinessInsightsResponse.DemandInsight;
import com.example.udyamsetubackend.insights.enums.InsightPeriod;
import com.example.udyamsetubackend.insights.enums.ServiceSource;
import com.example.udyamsetubackend.insights.enums.TrendDirection;
import com.example.udyamsetubackend.insights.enums.TrendGranularity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Business insights against real SQL on udyamsetu_test (enforced by the test guard). Each test is rolled
 * back by @DataJpaTest. The test database holds no committed enquiries, so window totals are exact.
 *
 * <p>Seeded data (all created today unless noted):
 * <ul>
 *   <li>"ZZINS Nail": NEW, UNFULFILLED, UNFULFILLED, LOST, plus "  zzins nail  " NEW (5 enquiries, same key)</li>
 *   <li>"ZZINS Lost Heavy": LOST x3, NEW x2 (5 enquiries, LOST 60%)</li>
 *   <li>"ZZINS Small": LOST x2 (2 enquiries, below the sample minimum)</li>
 *   <li>one enquiry with no requested service</li>
 *   <li>"ZZINS Edge In": created 29 days ago at noon, the first day inside the 30-day window</li>
 *   <li>"ZZINS Edge Out": created 30 days ago at 23:59, the last moment outside the 30-day window</li>
 *   <li>"ZZINS Old": created 45 days ago, so it is outside the 30-day window but inside ALL_TIME</li>
 * </ul>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BusinessInsightsService.class)
class BusinessInsightsServiceIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private BusinessInsightsService service;

    @Autowired
    private EnquiryRepository enquiryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ServiceOfferingRepository serviceOfferingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Customer customer;

    @BeforeEach
    void seed() {
        // Exact totals below assume no other enquiries exist. Fail loudly rather than miscount.
        assertEquals(0, enquiryRepository.count(),
                "udyamsetu_test must hold no enquiries before these tests seed their data");
        assertEquals(0, serviceOfferingRepository.count(),
                "udyamsetu_test must hold no catalogue services before these tests seed their data");

        customer = customerRepository.saveAndFlush(new Customer("ZZINS Customer", "ZZINS-KEY", "ZZINS-KEY", null));

        save("ZZINS Nail", EnquiryStatus.NEW);
        save("ZZINS Nail", EnquiryStatus.UNFULFILLED);
        save("ZZINS Nail", EnquiryStatus.UNFULFILLED);
        save("ZZINS Nail", EnquiryStatus.LOST);
        save("  zzins nail  ", EnquiryStatus.NEW);

        for (int i = 0; i < 3; i++) save("ZZINS Lost Heavy", EnquiryStatus.LOST);
        for (int i = 0; i < 2; i++) save("ZZINS Lost Heavy", EnquiryStatus.NEW);

        save("ZZINS Small", EnquiryStatus.LOST);
        save("ZZINS Small", EnquiryStatus.LOST);

        save(null, EnquiryStatus.NEW);

        backdate(save("ZZINS Edge In", EnquiryStatus.NEW), TODAY.minusDays(29).atTime(12, 0));
        backdate(save("ZZINS Edge Out", EnquiryStatus.NEW), TODAY.minusDays(30).atTime(23, 59));
        backdate(save("ZZINS Old", EnquiryStatus.NEW), TODAY.minusDays(45).atTime(9, 0));
    }

    private void backdate(Enquiry enquiry, LocalDateTime createdAt) {
        jdbcTemplate.update("update enquiries set created_at = ? where id = ?", createdAt, enquiry.getId());
    }

    private Enquiry save(String requestedService, EnquiryStatus status) {
        Enquiry enquiry = new Enquiry(customer, "ZZINS Customer", "ZZINS-KEY");
        enquiry.setRequestedService(requestedService);
        enquiry.setStatus(status);
        return enquiryRepository.saveAndFlush(enquiry);
    }

    private static Optional<DemandInsight> find(List<DemandInsight> insights, String service) {
        return insights.stream().filter(i -> i.service().trim().equalsIgnoreCase(service.trim())).findFirst();
    }

    @Test
    void last30DaysWindowExcludesOldEnquiriesAndCountsStatuses() {
        BusinessInsightsResponse response = service.getInsights(InsightPeriod.LAST_30_DAYS, TODAY);

        assertEquals(TODAY.minusDays(29), response.from());
        assertEquals(TODAY, response.to());
        // 13 seeded for today, plus Edge In (29 days ago). Edge Out and Old are excluded.
        assertEquals(14, response.totalEnquiries());
        assertEquals(1, response.withoutRequestedService());
        // NEW: Nail, Nail variant, Lost Heavy x2, no-service enquiry, Edge In = 6
        // UNFULFILLED: Nail x2 = 2. LOST: Nail, Lost Heavy x3, Small x2 = 6.
        assertEquals(6L, response.statusCounts().get(EnquiryStatus.NEW));
        assertEquals(2L, response.statusCounts().get(EnquiryStatus.UNFULFILLED));
        assertEquals(6L, response.statusCounts().get(EnquiryStatus.LOST));
        assertEquals(0L, response.statusCounts().get(EnquiryStatus.CONVERTED));
        assertEquals(EnquiryStatus.values().length, response.statusCounts().size());
    }

    @Test
    void allTimeIncludesEveryStoredEnquiryAndStartsAtTheOldestOne() {
        BusinessInsightsResponse response = service.getInsights(InsightPeriod.ALL_TIME, TODAY);

        // 13 seeded for today, plus Edge In, Edge Out and Old.
        assertEquals(16, response.totalEnquiries());
        assertEquals(TODAY.minusDays(45), response.from(), "from is the date of the oldest enquiry");
        assertEquals(TrendGranularity.MONTH, response.demandTrend().granularity());
    }

    @Test
    void mostRequestedGroupsVariantSpellingsAndCountsStatuses() {
        BusinessInsightsResponse response = service.getInsights(InsightPeriod.LAST_30_DAYS, TODAY);

        DemandInsight nail = find(response.mostRequestedServices(), "ZZINS Nail").orElseThrow();
        assertEquals(5, nail.totalEnquiries(), "the spelling variant with different case and spaces is the same service");
        assertEquals(2, nail.unfulfilledCount());
        assertEquals(1, nail.lostCount());
        assertTrue(nail.sampleSufficient());
        assertEquals(new BigDecimal("0.400"), nail.unfulfilledRate());
        assertEquals(2L, nail.statusCounts().get(EnquiryStatus.UNFULFILLED));
        // Free-text demand: the key is the normalised text, and the source says so.
        assertEquals("TEXT:zzins nail", nail.serviceKey());
        assertEquals(ServiceSource.FREE_TEXT, nail.serviceSource());
    }

    @Test
    void catalogueLinkedEnquiriesAndFreeTextWithTheSameNameStayInSeparateGroups() {
        ServiceOffering catalogue = serviceOfferingRepository.saveAndFlush(new ServiceOffering("ZZBI Catalogue Pack"));
        for (int i = 0; i < 3; i++) {
            Enquiry linked = new Enquiry(customer, "ZZINS Customer", "ZZINS-KEY");
            linked.setService(catalogue);
            linked.setRequestedService(catalogue.getName());
            linked.setStatus(EnquiryStatus.NEW);
            enquiryRepository.saveAndFlush(linked);
        }
        // Same name typed as free text, with no catalogue link (a historical-style enquiry).
        save("ZZBI Catalogue Pack", EnquiryStatus.NEW);
        save("ZZBI Catalogue Pack", EnquiryStatus.NEW);

        BusinessInsightsResponse response = service.getInsights(InsightPeriod.LAST_30_DAYS, TODAY);

        DemandInsight linkedInsight = response.mostRequestedServices().stream()
                .filter(i -> i.serviceKey().equals("SERVICE:" + catalogue.getId()))
                .findFirst().orElseThrow(() -> new AssertionError(response.mostRequestedServices().toString()));
        assertEquals(ServiceSource.CATALOGUE, linkedInsight.serviceSource());
        assertEquals(3, linkedInsight.totalEnquiries());
        assertEquals("ZZBI Catalogue Pack", linkedInsight.service());

        DemandInsight textInsight = response.mostRequestedServices().stream()
                .filter(i -> i.serviceKey().equals("TEXT:zzbi catalogue pack"))
                .findFirst().orElseThrow(() -> new AssertionError(response.mostRequestedServices().toString()));
        assertEquals(ServiceSource.FREE_TEXT, textInsight.serviceSource());
        assertEquals(2, textInsight.totalEnquiries(), "free text must not be merged into the catalogue group");
    }

    @Test
    void repeatedUnfulfilledDemandReportsOnlyServicesUnfulfilledTwiceOrMore() {
        BusinessInsightsResponse response = service.getInsights(InsightPeriod.LAST_30_DAYS, TODAY);

        assertTrue(find(response.repeatedUnfulfilledDemand(), "ZZINS Nail").isPresent());
        assertFalse(find(response.repeatedUnfulfilledDemand(), "ZZINS Lost Heavy").isPresent(),
                "LOST enquiries must not count as unfulfilled demand");
    }

    @Test
    void highLostRateReportsOnlySufficientSamplesAndExplainsSuppression() {
        BusinessInsightsResponse response = service.getInsights(InsightPeriod.LAST_30_DAYS, TODAY);

        DemandInsight heavy = find(response.highLostRateServices(), "ZZINS Lost Heavy").orElseThrow();
        assertEquals(3, heavy.lostCount());
        assertEquals(new BigDecimal("0.600"), heavy.lostRate());

        assertFalse(find(response.highLostRateServices(), "ZZINS Small").isPresent(),
                "2 enquiries is below the minimum sample size");
        assertTrue(response.notes().stream().anyMatch(n -> n.contains("fewer than 5 enquiries")),
                response.notes().toString());
        assertTrue(response.notes().stream().anyMatch(n -> n.contains("no requested service")),
                response.notes().toString());
    }

    @Test
    void dailyTrendHasOneBucketPerDayWithZerosForGaps() {
        BusinessInsightsResponse response = service.getInsights(InsightPeriod.LAST_30_DAYS, TODAY);

        var trend = response.demandTrend();
        assertEquals(TrendGranularity.DAY, trend.granularity());
        assertEquals(30, trend.points().size());
        assertEquals(TODAY.minusDays(29).toString(), trend.points().get(0).bucket());
        assertEquals(TODAY.toString(), trend.points().get(29).bucket());
        assertEquals(1, trend.points().get(0).count(), "Edge In is on the first day of the window");
        assertEquals(13, trend.points().get(29).count());
        assertEquals(1, trend.previousPeriodCount(), "first half holds only Edge In");
        assertEquals(13, trend.recentPeriodCount());
        assertEquals(TrendDirection.RISING, trend.direction());
    }

    @Test
    void allTimeTrendIsNotJudgedForAShortHistory() {
        // Enquiries span at most three calendar months (45 days back to today), below the minimum of four buckets.
        BusinessInsightsResponse response = service.getInsights(InsightPeriod.ALL_TIME, TODAY);

        assertTrue(response.demandTrend().points().size() < InsightThresholds.MIN_TREND_BUCKETS);
        assertEquals(TrendDirection.INSUFFICIENT_DATA, response.demandTrend().direction());
    }

    @Test
    void thresholdsUsedAreReportedInTheResponse() {
        BusinessInsightsResponse response = service.getInsights(InsightPeriod.LAST_30_DAYS, TODAY);

        assertEquals(30, response.thresholds().lastDaysWindow());
        assertEquals(5, response.thresholds().minSampleSize());
        assertEquals(new BigDecimal("0.40"), response.thresholds().highLostRate());
    }
}
