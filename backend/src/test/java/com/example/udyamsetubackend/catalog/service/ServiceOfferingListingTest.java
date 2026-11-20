package com.example.udyamsetubackend.catalog.service;

import com.example.udyamsetubackend.catalog.entity.ServiceOffering;
import com.example.udyamsetubackend.catalog.repository.ServiceOfferingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real SQL for the list filters, run against udyamsetu_test (enforced by the test guard).
 * Seeded names use a unique prefix and each test is rolled back by @DataJpaTest.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ServiceOfferingListingTest {

    @Autowired
    private ServiceOfferingRepository repository;

    @BeforeEach
    void seedCatalogue() {
        repository.saveAndFlush(service("ZZTEST Nail Extensions", true));
        repository.saveAndFlush(service("ZZTEST Manicure", true));
        repository.saveAndFlush(service("ZZTEST Hair Spa", false));
        repository.saveAndFlush(service("ZZTEST 50% Off Bridal", true));
        repository.saveAndFlush(service("ZZTEST 500 Off Bridal", true));
        repository.saveAndFlush(service("ZZTEST a_b Special", true));
        repository.saveAndFlush(service("ZZTEST axb Special", true));
    }

    private static ServiceOffering service(String name, boolean active) {
        ServiceOffering entity = new ServiceOffering(name);
        entity.setActive(active);
        return entity;
    }

    private List<String> names(Boolean active, String nameFragment) {
        return repository.findAll(ServiceOfferingSpecifications.matching(active, nameFragment),
                        Sort.by("name").ascending())
                .stream()
                .map(ServiceOffering::getName)
                .toList();
    }

    @Test
    void noFiltersReturnsEverythingOrderedByName() {
        assertEquals(List.of("ZZTEST 50% Off Bridal", "ZZTEST 500 Off Bridal", "ZZTEST a_b Special",
                "ZZTEST axb Special", "ZZTEST Hair Spa", "ZZTEST Manicure", "ZZTEST Nail Extensions"),
                names(null, "ZZTEST"));
    }

    @Test
    void activeTrueReturnsOnlyActiveServices() {
        assertEquals(List.of("ZZTEST 50% Off Bridal", "ZZTEST 500 Off Bridal", "ZZTEST a_b Special",
                "ZZTEST axb Special", "ZZTEST Manicure", "ZZTEST Nail Extensions"),
                names(true, "ZZTEST"));
    }

    @Test
    void activeFalseReturnsOnlyInactiveServices() {
        assertEquals(List.of("ZZTEST Hair Spa"), names(false, "ZZTEST"));
    }

    @Test
    void partialNameSearchIsCaseInsensitive() {
        // The fragment carries the ZZTEST prefix so rows committed outside these tests cannot match.
        assertEquals(List.of("ZZTEST Nail Extensions"), names(null, "ZZTEST NAIL ext"));
    }

    @Test
    void percentSignInSearchIsMatchedLiterally() {
        // Unescaped, "50%" would also match "500". It must match only the literal "50%".
        assertEquals(List.of("ZZTEST 50% Off Bridal"), names(null, "50%"));
    }

    @Test
    void underscoreInSearchIsMatchedLiterally() {
        // Unescaped, "a_b" would also match "axb".
        assertEquals(List.of("ZZTEST a_b Special"), names(null, "a_b"));
    }

    @Test
    void filtersCombine() {
        assertEquals(List.of("ZZTEST Manicure"), names(true, "manicure"));
        assertEquals(List.of(), names(false, "manicure"));
    }

    @Test
    void blankNameFragmentIsIgnored() {
        assertTrue(names(null, "   ").containsAll(names(null, "ZZTEST")),
                "a blank search must not filter anything out");
    }
}
