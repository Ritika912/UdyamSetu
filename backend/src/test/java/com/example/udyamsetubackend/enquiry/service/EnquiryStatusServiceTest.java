package com.example.udyamsetubackend.enquiry.service;

import com.example.udyamsetubackend.common.exception.ResourceNotFoundException;
import com.example.udyamsetubackend.customer.entity.Customer;
import com.example.udyamsetubackend.enquiry.dto.EnquiryResponse;
import com.example.udyamsetubackend.enquiry.entity.Enquiry;
import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import com.example.udyamsetubackend.catalog.service.ServiceOfferingService;
import com.example.udyamsetubackend.customer.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnquiryStatusServiceTest {

    @Mock
    private EnquiryRepository enquiryRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private ServiceOfferingService serviceOfferingService;

    @InjectMocks
    private EnquiryService enquiryService;

    private Enquiry enquiry() {
        return new Enquiry(new Customer("Priya Sharma", "9876543210", "9876543210", null),
                "Priya Sharma", "9876543210");
    }

    @Test
    void updateStatusChangesStatusAndSaves() {
        Enquiry existing = enquiry();
        when(enquiryRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(enquiryRepository.saveAndFlush(any(Enquiry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnquiryResponse response = enquiryService.updateStatus(5L, EnquiryStatus.CONVERTED);

        assertEquals(EnquiryStatus.CONVERTED, existing.getStatus());
        assertEquals(EnquiryStatus.CONVERTED, response.status());
    }

    @Test
    void updateStatusForMissingEnquiryThrowsNotFoundAndSavesNothing() {
        when(enquiryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> enquiryService.updateStatus(404L, EnquiryStatus.LOST));

        verify(enquiryRepository, never()).saveAndFlush(any(Enquiry.class));
    }

    @Test
    void getEnquiryForMissingIdThrowsNotFound() {
        when(enquiryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> enquiryService.getEnquiry(404L));
    }

    @Test
    void listPassesFiltersAndNewestFirstOrderToRepository() {
        when(enquiryRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(enquiry()));

        List<EnquiryResponse> result = enquiryService.listEnquiries(EnquiryStatus.NEW, 3L);

        assertEquals(1, result.size());
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(enquiryRepository).findAll(any(Specification.class), sortCaptor.capture());
        Sort sort = sortCaptor.getValue();
        assertTrue(sort.getOrderFor("createdAt").isDescending());
        assertTrue(sort.getOrderFor("id").isDescending());
    }
}
