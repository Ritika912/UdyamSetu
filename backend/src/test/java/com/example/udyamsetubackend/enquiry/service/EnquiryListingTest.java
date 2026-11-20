package com.example.udyamsetubackend.enquiry.service;

import com.example.udyamsetubackend.customer.entity.Customer;
import com.example.udyamsetubackend.customer.repository.CustomerRepository;
import com.example.udyamsetubackend.enquiry.dto.EnquiryResponse;
import com.example.udyamsetubackend.enquiry.entity.Enquiry;
import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import com.example.udyamsetubackend.catalog.service.ServiceOfferingService;
import com.example.udyamsetubackend.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Real SQL for the enquiry list filters, run against udyamsetu_test (enforced by the test guard).
 * Each test is rolled back by @DataJpaTest. Seeded contact keys use a unique prefix.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({EnquiryService.class, CustomerService.class, ServiceOfferingService.class})
class EnquiryListingTest {

    @Autowired
    private EnquiryService enquiryService;

    @Autowired
    private EnquiryRepository enquiryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer alpha;
    private Customer beta;

    @BeforeEach
    void seed() {
        alpha = customerRepository.saveAndFlush(new Customer("ZZTEST Alpha", "ZZTEST-A", "ZZTEST-A", null));
        beta = customerRepository.saveAndFlush(new Customer("ZZTEST Beta", "ZZTEST-B", "ZZTEST-B", null));

        enquiryRepository.saveAndFlush(enquiry(alpha, EnquiryStatus.NEW));
        enquiryRepository.saveAndFlush(enquiry(alpha, EnquiryStatus.LOST));
        enquiryRepository.saveAndFlush(enquiry(beta, EnquiryStatus.NEW));
    }

    private static Enquiry enquiry(Customer customer, EnquiryStatus status) {
        Enquiry enquiry = new Enquiry(customer, customer.getCustomerName(), customer.getContactNumber());
        enquiry.setStatus(status);
        return enquiry;
    }

    /**
     * Keeps only the enquiries this test seeded. The table is shared with rows committed outside these tests,
     * so unfiltered queries are checked against the seeded customers only.
     */
    private List<EnquiryResponse> seededOnly(List<EnquiryResponse> enquiries) {
        Set<Long> seededCustomers = Set.of(alpha.getId(), beta.getId());
        return enquiries.stream()
                .filter(e -> seededCustomers.contains(e.customerId()))
                .toList();
    }

    @Test
    void noFiltersReturnsAllEnquiriesNewestFirst() {
        List<EnquiryResponse> all = seededOnly(enquiryService.listEnquiries(null, null));

        assertEquals(3, all.size());
        // Newest first: the last saved enquiry (Beta, NEW) must come first.
        assertEquals(beta.getId(), all.get(0).customerId());
    }

    @Test
    void statusFilterReturnsOnlyMatchingEnquiries() {
        List<EnquiryResponse> newOnes = seededOnly(enquiryService.listEnquiries(EnquiryStatus.NEW, null));

        assertEquals(2, newOnes.size());
        assertEquals(List.of(EnquiryStatus.NEW, EnquiryStatus.NEW),
                newOnes.stream().map(EnquiryResponse::status).toList());
    }

    @Test
    void customerFilterReturnsOnlyThatCustomersEnquiries() {
        assertEquals(2, enquiryService.listEnquiries(null, alpha.getId()).size());
        assertEquals(1, enquiryService.listEnquiries(null, beta.getId()).size());
    }

    @Test
    void filtersCombine() {
        List<EnquiryResponse> result = enquiryService.listEnquiries(EnquiryStatus.LOST, alpha.getId());

        assertEquals(1, result.size());
        assertEquals(EnquiryStatus.LOST, result.get(0).status());
    }

    @Test
    void noMatchReturnsEmptyList() {
        assertEquals(List.of(), seededOnly(enquiryService.listEnquiries(EnquiryStatus.CONVERTED, null)));
    }
}
