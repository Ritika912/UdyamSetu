package com.example.udyamsetubackend.catalog.service;

import com.example.udyamsetubackend.catalog.dto.ServiceRequest;
import com.example.udyamsetubackend.catalog.dto.ServiceResponse;
import com.example.udyamsetubackend.catalog.entity.ServiceOffering;
import com.example.udyamsetubackend.catalog.repository.ServiceOfferingRepository;
import com.example.udyamsetubackend.common.exception.ConflictException;
import com.example.udyamsetubackend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOfferingServiceTest {

    @Mock
    private ServiceOfferingRepository repository;

    @InjectMocks
    private ServiceOfferingService service;

    @BeforeEach
    void saveReturnsEntityAsIs() {
        org.mockito.Mockito.lenient().when(repository.save(any(ServiceOffering.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(repository.saveAndFlush(any(ServiceOffering.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static ServiceOffering persisted(long id, String name, boolean active) {
        ServiceOffering entity = new ServiceOffering(name);
        entity.setActive(active);
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static ServiceRequest request(String name, String description, String category,
                                          BigDecimal price, Boolean isActive) {
        return new ServiceRequest(name, description, category, price, isActive);
    }

    // ---------- create ----------

    @Test
    void createTrimsNameAndNormalizesBlankOptionalTextToNull() {
        when(repository.findByNameIgnoreCase("Haircut")).thenReturn(Optional.empty());

        ServiceResponse response = service.createService(
                request("  Haircut  ", "   ", "  Hair  ", new BigDecimal("300.00"), null));

        assertEquals("Haircut", response.name());
        assertNull(response.description());
        assertEquals("Hair", response.category());
        assertEquals(new BigDecimal("300.00"), response.price());
    }

    @Test
    void createDefaultsIsActiveToTrueWhenOmitted() {
        when(repository.findByNameIgnoreCase("Haircut")).thenReturn(Optional.empty());

        assertTrue(service.createService(request("Haircut", null, null, null, null)).isActive());
    }

    @Test
    void createKeepsExplicitInactiveStatus() {
        when(repository.findByNameIgnoreCase("Seasonal Pack")).thenReturn(Optional.empty());

        assertFalse(service.createService(request("Seasonal Pack", null, null, null, false)).isActive());
    }

    @Test
    void createRejectsDuplicateNameIgnoringCase() {
        when(repository.findByNameIgnoreCase("facial"))
                .thenReturn(Optional.of(persisted(7L, "Facial", true)));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.createService(request("facial", null, null, null, null)));

        assertEquals("A service with this name already exists (serviceId=7)", ex.getMessage());
        verify(repository, never()).save(any(ServiceOffering.class));
    }

    // ---------- get ----------

    @Test
    void getReturnsServiceById() {
        when(repository.findById(3L)).thenReturn(Optional.of(persisted(3L, "Facial", true)));

        assertEquals("Facial", service.getService(3L).name());
    }

    @Test
    void getMissingServiceIsNotFound() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getService(404L));
    }

    // ---------- update ----------

    @Test
    void updateToOwnNameIsAllowedBecauseTheServiceIsExcluded() {
        ServiceOffering existing = persisted(3L, "Facial", true);
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.findByNameIgnoreCase("facial")).thenReturn(Optional.of(existing));

        ServiceResponse response = service.updateService(3L,
                request("facial", "Deep cleanse", null, new BigDecimal("800.00"), null));

        assertEquals("facial", response.name());
        assertEquals("Deep cleanse", response.description());
    }

    @Test
    void updateToAnotherServicesNameIsConflict() {
        when(repository.findById(3L)).thenReturn(Optional.of(persisted(3L, "Facial", true)));
        when(repository.findByNameIgnoreCase("Manicure"))
                .thenReturn(Optional.of(persisted(9L, "Manicure", true)));

        assertThrows(ConflictException.class,
                () -> service.updateService(3L, request("Manicure", null, null, null, null)));
        verify(repository, never()).saveAndFlush(any(ServiceOffering.class));
    }

    @Test
    void updateWithNullIsActiveLeavesStatusUnchanged() {
        when(repository.findById(3L)).thenReturn(Optional.of(persisted(3L, "Facial", false)));
        when(repository.findByNameIgnoreCase("Facial")).thenReturn(Optional.empty());

        assertFalse(service.updateService(3L, request("Facial", null, null, null, null)).isActive());
    }

    @Test
    void updateWithIsActiveTrueReactivatesService() {
        when(repository.findById(3L)).thenReturn(Optional.of(persisted(3L, "Facial", false)));
        when(repository.findByNameIgnoreCase("Facial")).thenReturn(Optional.empty());

        assertTrue(service.updateService(3L, request("Facial", null, null, null, true)).isActive());
    }

    @Test
    void updateMissingServiceIsNotFound() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateService(404L, request("Facial", null, null, null, null)));
    }

    // ---------- deactivate ----------

    @Test
    void deactivateSetsIsActiveFalseAndNeverDeletes() {
        ServiceOffering existing = persisted(3L, "Facial", true);
        when(repository.findById(3L)).thenReturn(Optional.of(existing));

        service.deactivateService(3L);

        assertFalse(existing.isActive());
        verify(repository).saveAndFlush(existing);
        verify(repository, never()).delete(any(ServiceOffering.class));
        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    void deactivatingAnInactiveServiceIsANoOp() {
        when(repository.findById(3L)).thenReturn(Optional.of(persisted(3L, "Facial", false)));

        service.deactivateService(3L);

        verify(repository, never()).saveAndFlush(any(ServiceOffering.class));
    }

    @Test
    void deactivateMissingServiceIsNotFound() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deactivateService(404L));
    }

    // ---------- list ----------

    @Test
    void listQueriesRepositoryOrderedByName() {
        when(repository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(persisted(1L, "Facial", true)));

        List<ServiceResponse> results = service.listServices(true, "fac");

        assertEquals(1, results.size());
        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        verify(repository).findAll(any(Specification.class), sort.capture());
        assertEquals(Sort.by("name").ascending(), sort.getValue());
    }
}
