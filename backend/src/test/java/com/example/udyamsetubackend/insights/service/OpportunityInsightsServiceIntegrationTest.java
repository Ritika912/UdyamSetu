package com.example.udyamsetubackend.insights.service;

import com.example.udyamsetubackend.catalog.entity.ServiceOffering;
import com.example.udyamsetubackend.catalog.repository.ServiceOfferingRepository;
import com.example.udyamsetubackend.customer.entity.Customer;
import com.example.udyamsetubackend.customer.repository.CustomerRepository;
import com.example.udyamsetubackend.enquiry.entity.Enquiry;
import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import com.example.udyamsetubackend.insights.dto.OpportunityInsightsResponse;
import com.example.udyamsetubackend.insights.dto.OpportunityInsightsResponse.Opportunity;
import com.example.udyamsetubackend.insights.enums.InsightPeriod;
import com.example.udyamsetubackend.insights.enums.OpportunityType;
import com.example.udyamsetubackend.insights.enums.ServiceSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opportunity insights against real SQL on udyamsetu_test (enforced by the test guard). Each test is rolled back
 * by @DataJpaTest. A precondition fails the run if the test database already holds enquiries or services.
 *
 * <p>Seeded window (30 days, all created today unless noted). Total enquiries in the window: 35.
 * <ul>
 *   <li>"ZZOPP Bridal" (catalogue): UNFULFILLED x3, NEW x2. Expect REPEATED_UNFULFILLED.</li>
 *   <li>"ZZOPP Facial" (catalogue): LOST x3, NEW x2. Expect HIGH_LOST_RATE, and no REPEATED_UNFULFILLED.</li>
 *   <li>"ZZOPP Growth" (catalogue): NEW x4 today, NEW x1 25 days ago. Expect INCREASING_DEMAND.</li>
 *   <li>"ZZOPP Old Pack" (catalogue, deactivated): UNFULFILLED x2, NEW x3. Expect a deactivated flag.</li>
 *   <li>Free text "ZZOPP  Mehendi" (double space), "zzopp mehendi" and " ZZOPP Mehendi ": UNFULFILLED x4, NEW x1.
 *       One normalised key, 5 enquiries. Expect REPEATED_UNFULFILLED with FREE_TEXT source.</li>
 *   <li>Free text "ZZOPP Cafe" x3 and "ZZOPP Café" x3: must stay separate. Neither reaches the frequent rule.</li>
 *   <li>Free text "ZZOPP Saree": UNFULFILLED x2, NEW x1. Repeated, but below the sample minimum.</li>
 *   <li>One enquiry with no service and no requested-service text.</li>
 * </ul>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OpportunityInsightsService.class)
class OpportunityInsightsServiceIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private OpportunityInsightsService service;

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
        assertEquals(0, enquiryRepository.count(),
                "udyamsetu_test must hold no enquiries before these tests seed their data");
        assertEquals(0, serviceOfferingRepository.count(),
                "udyamsetu_test must hold no catalogue services before these tests seed their data");

        customer = customerRepository.saveAndFlush(new Customer("ZZOPP Customer", "ZZOPP-KEY", "ZZOPP-KEY", null));

        ServiceOffering bridal = service("ZZOPP Bridal", true);
        ServiceOffering facial = service("ZZOPP Facial", true);
        ServiceOffering growth = service("ZZOPP Growth", true);
        ServiceOffering oldPack = service("ZZOPP Old Pack", false);

        for (int i = 0; i < 3; i++) linked(bridal, EnquiryStatus.UNFULFILLED);
        for (int i = 0; i < 2; i++) linked(bridal, EnquiryStatus.NEW);

        for (int i = 0; i < 3; i++) linked(facial, EnquiryStatus.LOST);
        for (int i = 0; i < 2; i++) linked(facial, EnquiryStatus.NEW);

        for (int i = 0; i < 4; i++) linked(growth, EnquiryStatus.NEW);
        backdate(linked(growth, EnquiryStatus.NEW), TODAY.minusDays(25).atTime(10, 0));

        for (int i = 0; i < 2; i++) linked(oldPack, EnquiryStatus.UNFULFILLED);
        for (int i = 0; i < 3; i++) linked(oldPack, EnquiryStatus.NEW);

        freeText("ZZOPP  Mehendi", EnquiryStatus.UNFULFILLED);
        freeText("ZZOPP  Mehendi", EnquiryStatus.UNFULFILLED);
        freeText("zzopp mehendi", EnquiryStatus.UNFULFILLED);
        freeText("zzopp mehendi", EnquiryStatus.UNFULFILLED);
        freeText(" ZZOPP Mehendi ", EnquiryStatus.NEW);

        for (int i = 0; i < 3; i++) freeText("ZZOPP Cafe", EnquiryStatus.NEW);
        for (int i = 0; i < 3; i++) freeText("ZZOPP Café", EnquiryStatus.NEW);

        freeText("ZZOPP Saree", EnquiryStatus.UNFULFILLED);
        freeText("ZZOPP Saree", EnquiryStatus.UNFULFILLED);
        freeText("ZZOPP Saree", EnquiryStatus.NEW);

        freeText(null, EnquiryStatus.NEW);
    }

    private ServiceOffering service(String name, boolean active) {
        ServiceOffering entity = new ServiceOffering(name);
        entity.setActive(active);
        return serviceOfferingRepository.saveAndFlush(entity);
    }

    private Enquiry linked(ServiceOffering service, EnquiryStatus status) {
        Enquiry enquiry = new Enquiry(customer, "ZZOPP Customer", "ZZOPP-KEY");
        enquiry.setService(service);
        enquiry.setRequestedService(service.getName());
        enquiry.setStatus(status);
        return enquiryRepository.saveAndFlush(enquiry);
    }

    private void freeText(String requestedService, EnquiryStatus status) {
        Enquiry enquiry = new Enquiry(customer, "ZZOPP Customer", "ZZOPP-KEY");
        enquiry.setRequestedService(requestedService);
        enquiry.setStatus(status);
        enquiryRepository.saveAndFlush(enquiry);
    }

    private Enquiry backdate(Enquiry enquiry, LocalDateTime createdAt) {
        jdbcTemplate.update("update enquiries set created_at = ? where id = ?", createdAt, enquiry.getId());
        return enquiry;
    }

    private OpportunityInsightsResponse run() {
        return service.getOpportunities(InsightPeriod.LAST_30_DAYS, TODAY);
    }

    private static Opportunity find(OpportunityInsightsResponse response, String serviceName, OpportunityType type) {
        return response.opportunities().stream()
                .filter(o -> o.serviceName().equals(serviceName) && o.type() == type)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + type + " for " + serviceName + " in " + response.opportunities()));
    }

    private static boolean has(OpportunityInsightsResponse response, String serviceName, OpportunityType type) {
        return response.opportunities().stream()
                .anyMatch(o -> o.serviceName().equals(serviceName) && o.type() == type);
    }

    @Test
    void repeatedUnfulfilledCatalogueServiceIsRecommendedWithCatalogueKey() {
        OpportunityInsightsResponse response = run();

        Opportunity op = find(response, "ZZOPP Bridal", OpportunityType.REPEATED_UNFULFILLED);
        assertEquals(ServiceSource.CATALOGUE, op.serviceSource());
        assertTrue(op.serviceKey().startsWith("SERVICE:"), op.serviceKey());
        assertEquals(3, op.supportingEnquiryCount());
        assertEquals(3L, op.statusCounts().get(EnquiryStatus.UNFULFILLED));
        assertEquals(TODAY.minusDays(29), op.periodFrom());
        assertEquals(TODAY, op.periodTo());
    }

    @Test
    void lostEnquiriesProduceConversionRiskNotUnmetDemand() {
        OpportunityInsightsResponse response = run();

        Opportunity lost = find(response, "ZZOPP Facial", OpportunityType.HIGH_LOST_RATE);
        assertEquals(3, lost.supportingEnquiryCount());
        assertFalse(has(response, "ZZOPP Facial", OpportunityType.REPEATED_UNFULFILLED),
                "LOST enquiries must not create unmet-demand recommendations");
    }

    @Test
    void increasingDemandIsDetectedFromTheRecentHalfOfTheWindow() {
        OpportunityInsightsResponse response = run();

        Opportunity growth = find(response, "ZZOPP Growth", OpportunityType.INCREASING_DEMAND);
        assertEquals(4, growth.recentCount());
        assertEquals(1, growth.previousCount());
    }

    @Test
    void deactivatedCatalogueServiceIsFlaggedAndSaysSo() {
        Opportunity op = find(run(), "ZZOPP Old Pack", OpportunityType.REPEATED_UNFULFILLED);

        assertEquals(false, op.serviceActive());
        assertTrue(op.explanation().contains("deactivated"), op.explanation());
    }

    @Test
    void freeTextSpellingsWithDifferentSpacingAndCaseAreGroupedUnderOneKey() {
        // Three spellings, one key. The display name is the smallest original spelling ("ZZOPP  Mehendi" with the
        // double space), which is deterministic regardless of row order.
        Opportunity op = find(run(), "ZZOPP  Mehendi", OpportunityType.REPEATED_UNFULFILLED);

        assertEquals("TEXT:zzopp mehendi", op.serviceKey());
        assertEquals(ServiceSource.FREE_TEXT, op.serviceSource());
        assertEquals(null, op.serviceActive());
        assertEquals(4, op.supportingEnquiryCount(), "UNFULFILLED from all three spellings");
        assertEquals(5L, op.statusCounts().values().stream().mapToLong(Long::longValue).sum());
    }

    @Test
    void cafeAndCafeWithAccentAreNeverMerged() {
        OpportunityInsightsResponse response = run();

        // Merged, the two spellings would be 6 enquiries and pass the frequent rule. Kept apart, neither does.
        boolean anyCafeRecommendation = response.opportunities().stream()
                .anyMatch(o -> o.serviceKey().startsWith("TEXT:zzopp caf"));
        assertFalse(anyCafeRecommendation, response.opportunities().toString());
    }

    @Test
    void smallSampleRepeatsAreSuppressedWithANote() {
        OpportunityInsightsResponse response = run();

        assertFalse(response.opportunities().stream().anyMatch(o -> o.serviceName().equals("ZZOPP Saree")));
        assertTrue(response.notes().stream().anyMatch(n -> n.contains("fewer than 5 enquiries")),
                response.notes().toString());
    }

    @Test
    void unattributableEnquiriesAreCountedInANote() {
        OpportunityInsightsResponse response = run();

        assertEquals(35, response.totalEnquiries());
        assertTrue(response.notes().stream().anyMatch(n -> n.startsWith("1 enquiries have no catalogue service")),
                response.notes().toString());
    }

    @Test
    void resultIsCappedAndMostSignificantFirst() {
        OpportunityInsightsResponse response = run();

        assertTrue(response.opportunities().size() <= response.thresholds().maxOpportunities());
        for (int i = 1; i < response.opportunities().size(); i++) {
            assertTrue(response.opportunities().get(i - 1).supportingEnquiryCount()
                    >= response.opportunities().get(i).supportingEnquiryCount());
        }
    }

    @Test
    void everyRecommendationCarriesItsCategoryAndAnAction() {
        List<Opportunity> all = run().opportunities();

        assertFalse(all.isEmpty());
        for (Opportunity op : all) {
            assertEquals(op.type().category(), op.category());
            assertFalse(op.suggestedAction().isBlank());
            assertFalse(op.explanation().isBlank());
        }
    }
}
