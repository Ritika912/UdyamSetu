package com.example.udyamsetubackend.catalog.repository;

import com.example.udyamsetubackend.catalog.entity.ServiceOffering;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs against the configured datasource (udyamsetu_test, enforced by the test guard), not an
 * in-memory database. Each test is rolled back by @DataJpaTest, so no rows persist.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ServiceOfferingRepositoryTest {

    @Autowired
    private ServiceOfferingRepository repository;

    @Test
    void nameOnlyServiceGetsDefaultsAndTimestamps() {
        ServiceOffering saved = repository.saveAndFlush(new ServiceOffering("Haircut"));

        assertNotNull(saved.getId());
        assertEquals("Haircut", saved.getName());
        assertNull(saved.getDescription());
        assertNull(saved.getCategory());
        assertNull(saved.getPrice());
        assertTrue(saved.isActive(), "a new service is active by default");
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void allFieldsArePersisted() {
        ServiceOffering service = new ServiceOffering("Bridal Makeup");
        service.setDescription("Full bridal look with trial session");
        service.setCategory("Makeup");
        service.setPrice(new BigDecimal("4500.00"));

        ServiceOffering saved = repository.saveAndFlush(service);
        ServiceOffering reloaded = repository.findById(saved.getId()).orElseThrow();

        assertEquals("Full bridal look with trial session", reloaded.getDescription());
        assertEquals("Makeup", reloaded.getCategory());
        assertEquals(0, new BigDecimal("4500.00").compareTo(reloaded.getPrice()));
    }

    @Test
    void zeroPriceIsAllowed() {
        ServiceOffering service = new ServiceOffering("Free Consultation");
        service.setPrice(BigDecimal.ZERO);

        assertNotNull(repository.saveAndFlush(service).getId());
    }

    @Test
    void negativePriceIsRejectedByTheDatabase() {
        ServiceOffering service = new ServiceOffering("Bad Price");
        service.setPrice(new BigDecimal("-1.00"));

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(service));
    }

    @Test
    void duplicateNameIsRejectedCaseInsensitivelyByTheDatabase() {
        repository.saveAndFlush(new ServiceOffering("Facial"));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(new ServiceOffering("facial")));
    }

    @Test
    void existsByNameIgnoreCaseDetectsExistingNames() {
        repository.saveAndFlush(new ServiceOffering("Manicure"));

        assertTrue(repository.existsByNameIgnoreCase("MANICURE"));
        assertFalse(repository.existsByNameIgnoreCase("Pedicure"));
    }

    @Test
    void existsByNameIgnoreCaseAndIdNotIgnoresTheServiceItself() {
        ServiceOffering saved = repository.saveAndFlush(new ServiceOffering("Haircut"));

        assertFalse(repository.existsByNameIgnoreCaseAndIdNot("haircut", saved.getId()),
                "a service must not clash with its own name");
        assertTrue(repository.existsByNameIgnoreCaseAndIdNot("haircut", saved.getId() + 1000),
                "another service with the same name is a clash");
    }

    @Test
    void findByNameIgnoreCaseReturnsTheService() {
        repository.saveAndFlush(new ServiceOffering("Nail Art"));

        assertEquals("Nail Art", repository.findByNameIgnoreCase("nail art").orElseThrow().getName());
    }
}
