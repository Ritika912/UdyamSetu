package com.example.udyamsetubackend.catalog.service;

import com.example.udyamsetubackend.catalog.entity.ServiceOffering;
import com.example.udyamsetubackend.catalog.repository.ServiceOfferingRepository;
import com.example.udyamsetubackend.common.exception.FieldValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Rules used when an enquiry selects a catalogue service: it must exist and be active.
 */
@ExtendWith(MockitoExtension.class)
class ServiceOfferingEnquiryLookupTest {

    @Mock
    private ServiceOfferingRepository repository;

    @InjectMocks
    private ServiceOfferingService service;

    @Test
    void activeServiceIsReturnedForEnquiries() {
        ServiceOffering nails = new ServiceOffering("Nail Extensions");
        when(repository.findById(5L)).thenReturn(Optional.of(nails));

        assertSame(nails, service.getActiveServiceForEnquiry(5L));
    }

    @Test
    void unknownServiceIdIsRejectedWithFieldErrorOnServiceId() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        FieldValidationException ex = assertThrows(FieldValidationException.class,
                () -> service.getActiveServiceForEnquiry(404L));

        assertEquals("serviceId", ex.getField());
        assertEquals("serviceId does not refer to an existing service", ex.getMessage());
    }

    @Test
    void inactiveServiceIsRejectedWithFieldErrorOnServiceId() {
        ServiceOffering inactive = new ServiceOffering("Seasonal Pack");
        inactive.setActive(false);
        ReflectionTestUtils.setField(inactive, "id", 7L);
        when(repository.findById(7L)).thenReturn(Optional.of(inactive));

        FieldValidationException ex = assertThrows(FieldValidationException.class,
                () -> service.getActiveServiceForEnquiry(7L));

        assertEquals("serviceId", ex.getField());
        assertEquals("serviceId refers to an inactive service", ex.getMessage());
    }
}
