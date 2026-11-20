package com.example.udyamsetubackend.insights.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ServiceKeysTest {

    @Test
    void catalogueServiceKeyUsesServiceId() {
        assertEquals("SERVICE:42", ServiceKeys.forCatalogueService(42L));
    }

    @Test
    void freeTextIsTrimmedLowercasedAndWhitespaceCollapsed() {
        assertEquals("TEXT:bridal makeup", ServiceKeys.forFreeText("  Bridal   Makeup "));
        assertEquals("TEXT:bridal makeup", ServiceKeys.forFreeText("BRIDAL\tMAKEUP"));
        assertEquals("TEXT:bridal makeup", ServiceKeys.forFreeText("bridal\n makeup"));
    }

    @Test
    void accentedAndPlainSpellingsAreNotMerged() {
        // Deliberate: a name is never merged with a similar one, even when only an accent differs.
        assertNotEquals(ServiceKeys.forFreeText("Café"), ServiceKeys.forFreeText("Cafe"));
    }

    @Test
    void catalogueAndFreeTextKeysNeverCollide() {
        assertNotEquals(ServiceKeys.forCatalogueService(7L), ServiceKeys.forFreeText("7"));
    }

    @Test
    void normaliseIsIdempotent() {
        String once = ServiceKeys.normalizeText("  Nail   Art ");
        assertEquals(once, ServiceKeys.normalizeText(once));
    }
}
