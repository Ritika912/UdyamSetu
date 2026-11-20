package com.example.udyamsetubackend.enquiry.service;

import com.example.udyamsetubackend.catalog.entity.ServiceOffering;
import com.example.udyamsetubackend.catalog.service.ServiceOfferingService;
import com.example.udyamsetubackend.common.exception.FieldValidationException;
import com.example.udyamsetubackend.customer.entity.Customer;
import com.example.udyamsetubackend.customer.service.CustomerService;
import com.example.udyamsetubackend.enquiry.dto.EnquiryRequest;
import com.example.udyamsetubackend.enquiry.dto.EnquiryResponse;
import com.example.udyamsetubackend.enquiry.entity.Enquiry;
import com.example.udyamsetubackend.enquiry.enums.EnquirySource;
import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnquiryServiceTest {

    @Mock
    private EnquiryRepository enquiryRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private ServiceOfferingService serviceOfferingService;

    @InjectMocks
    private EnquiryService enquiryService;

    @BeforeEach
    void stubCollaborators() {
        // Lenient: not every test reaches each collaborator (for example, rejected service ids stop early).
        Mockito.lenient().when(enquiryRepository.save(any(Enquiry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.lenient().when(customerService.resolveCustomer(anyString(), anyString()))
                .thenAnswer(invocation -> new Customer(
                        invocation.getArgument(0), invocation.getArgument(1), "KEY-" + invocation.getArgument(1), null));
    }

    /** Request with no catalogue service, so the 12th component (serviceId) is null. */
    private static EnquiryRequest request(String customerName, String contactNumber, String requestedService) {
        return new EnquiryRequest(customerName, contactNumber,
                null, requestedService, null, null, null, null, null, null, null, null);
    }

    private Enquiry captureSavedEnquiry() {
        ArgumentCaptor<Enquiry> captor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void enquiryIsLinkedToTheResolvedCustomer() {
        enquiryService.createEnquiry(request("Priya Sharma", "9876543210", null));

        Customer linked = captureSavedEnquiry().getCustomer();

        assertEquals("Priya Sharma", linked.getCustomerName());
        assertEquals("KEY-9876543210", linked.getContactKey());
    }

    @Test
    void customerIsResolvedWithTrimmedNameAndNumber() {
        enquiryService.createEnquiry(request("  Priya Sharma  ", "  9876543210 ", null));

        verify(customerService).resolveCustomer("Priya Sharma", "9876543210");
    }

    @Test
    void minimalRequestIsSavedWithNewStatusAndNullOptionalFields() {
        EnquiryRequest request = new EnquiryRequest(
                "Priya Sharma", "9876543210",
                null, null, null, null, null, null, null, null, null, null);

        EnquiryResponse response = enquiryService.createEnquiry(request);

        assertEquals("Priya Sharma", response.customerName());
        assertEquals("9876543210", response.contactNumber());
        assertEquals(EnquiryStatus.NEW, response.status());
        assertNull(response.source());
        assertNull(response.requestedService());
        assertNull(response.serviceId());
        assertNull(response.budget());
        assertNull(response.followUpTime());
    }

    @Test
    void allFieldsAreMappedToEntityAndResponse() {
        EnquiryRequest request = new EnquiryRequest(
                "Priya Sharma", "+919876543210",
                EnquirySource.WHATSAPP, "Nail Extensions", "Interested in gel nail extensions",
                "Wedding", new BigDecimal("2500.00"),
                LocalDate.of(2026, 12, 15), LocalDate.of(2026, 11, 20), LocalTime.of(15, 30),
                "Call after 3 PM", null);

        EnquiryResponse response = enquiryService.createEnquiry(request);

        assertEquals(EnquirySource.WHATSAPP, response.source());
        assertEquals("Nail Extensions", response.requestedService());
        assertEquals("Interested in gel nail extensions", response.requirements());
        assertEquals("Wedding", response.occasion());
        assertEquals(new BigDecimal("2500.00"), response.budget());
        assertEquals(LocalDate.of(2026, 12, 15), response.expectedDate());
        assertEquals(LocalDate.of(2026, 11, 20), response.followUpDate());
        assertEquals(LocalTime.of(15, 30), response.followUpTime());
        assertEquals("Call after 3 PM", response.followUpNotes());
        assertEquals(EnquiryStatus.NEW, response.status());
    }

    @Test
    void blankOptionalTextIsStoredAsNullAndNonBlankTextIsTrimmed() {
        EnquiryRequest request = new EnquiryRequest(
                "  Priya Sharma  ", "  9876543210 ",
                null, "   ", "", "  Wedding  ", null, null, null, null, "  Call after 3 PM ", null);

        enquiryService.createEnquiry(request);

        Enquiry saved = captureSavedEnquiry();
        assertEquals("Priya Sharma", saved.getCustomerName());
        assertEquals("9876543210", saved.getContactNumber());
        assertNull(saved.getRequestedService());
        assertNull(saved.getRequirements());
        assertEquals("Wedding", saved.getOccasion());
        assertEquals("Call after 3 PM", saved.getFollowUpNotes());
    }

    // ---------- catalogue link ----------

    @Test
    void validServiceIdLinksTheServiceAndUsesItsNameWhenTextIsOmitted() {
        ServiceOffering nails = new ServiceOffering("Nail Extensions");
        when(serviceOfferingService.getActiveServiceForEnquiry(5L)).thenReturn(nails);

        EnquiryResponse response = enquiryService.createEnquiry(new EnquiryRequest(
                "Priya Sharma", "9876543210", null, null, null, null, null, null, null, null, null, 5L));

        Enquiry saved = captureSavedEnquiry();
        assertSame(nails, saved.getService());
        assertEquals("Nail Extensions", saved.getRequestedService());
        assertEquals("Nail Extensions", response.requestedService());
    }

    @Test
    void customRequestedTextIsPreservedOverTheServiceName() {
        when(serviceOfferingService.getActiveServiceForEnquiry(5L))
                .thenReturn(new ServiceOffering("Haircut"));

        enquiryService.createEnquiry(new EnquiryRequest(
                "Priya Sharma", "9876543210", null, "  Haircut with colour  ",
                null, null, null, null, null, null, null, 5L));

        assertEquals("Haircut with colour", captureSavedEnquiry().getRequestedService());
    }

    @Test
    void freeTextWithoutServiceIdIsStoredWithNoCatalogueLink() {
        enquiryService.createEnquiry(request("Priya Sharma", "9876543210", "Nail Extensions"));

        Enquiry saved = captureSavedEnquiry();
        assertNull(saved.getService());
        assertEquals("Nail Extensions", saved.getRequestedService());
        verify(serviceOfferingService, never()).getActiveServiceForEnquiry(anyLong());
    }

    @Test
    void invalidServiceIdIsRejectedBeforeAnyCustomerOrEnquiryIsSaved() {
        when(serviceOfferingService.getActiveServiceForEnquiry(404L))
                .thenThrow(new FieldValidationException("serviceId", "serviceId does not refer to an existing service"));

        assertThrows(FieldValidationException.class, () -> enquiryService.createEnquiry(new EnquiryRequest(
                "Priya Sharma", "9876543210", null, null, null, null, null, null, null, null, null, 404L)));

        verify(customerService, never()).resolveCustomer(anyString(), anyString());
        verify(enquiryRepository, never()).save(any(Enquiry.class));
    }
}
