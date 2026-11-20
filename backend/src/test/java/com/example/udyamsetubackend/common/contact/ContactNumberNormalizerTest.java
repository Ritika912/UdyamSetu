package com.example.udyamsetubackend.common.contact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContactNumberNormalizerTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "9876543210",
            "+919876543210",
            "919876543210",
            "+91 98765 43210",
            "+91-98765-43210",
            "98765-43210",
            " 9876543210 "
    })
    void indianNumbersNormalizeToTheTenDigitMobile(String raw) {
        assertEquals("9876543210", ContactNumberNormalizer.normalize(raw));
    }

    @Test
    void sameIndianNumberInDifferentFormatsProducesTheSameKey() {
        String plain = ContactNumberNormalizer.normalize("9876543210");
        assertEquals(plain, ContactNumberNormalizer.normalize("+91 98765-43210"));
        assertEquals(plain, ContactNumberNormalizer.normalize("919876543210"));
    }

    @Test
    void internationalNumbersKeepTheirFullDigitStringWithPlusPrefix() {
        assertEquals("+15551234567", ContactNumberNormalizer.normalize("+1 555-123-4567"));
        assertEquals("+447700900123", ContactNumberNormalizer.normalize("+447700900123"));
    }

    @Test
    void foreignNumberNeverCollidesWithAnIndianKey() {
        // Same ten digits, but with a + and a foreign code: must not equal the Indian key.
        assertEquals("+19876543210", ContactNumberNormalizer.normalize("+19876543210"));
        assertEquals("9876543210", ContactNumberNormalizer.normalize("9876543210"));
    }

    @Test
    void indianMobileMustStartWithSixToNine() {
        assertThrows(IllegalArgumentException.class, () -> ContactNumberNormalizer.normalize("1234567890"));
        assertThrows(IllegalArgumentException.class, () -> ContactNumberNormalizer.normalize("5876543210"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "+9198765432",          // +91 with the wrong length
            "+919876543210123",     // +91 with too many digits
            "+91",                  // +91 with no number
            "+912123456789",        // +91 followed by a non-mobile prefix
            "919876543",            // 91 prefix, 9 digits: ambiguous
            "09876543210",          // 11 digits without +: ambiguous (trunk prefix or not?)
            "15551234567",          // foreign digits without +: ambiguous with Indian formats
            "123456",               // below 7 digits
            "+1234567890123456",    // above 15 digits
            "98765ABCDE",           // letters
            "+98765+43210",         // second plus sign
            "98765.43210"           // unsupported separator
    })
    void ambiguousOrMalformedNumbersAreRejected(String raw) {
        assertThrows(IllegalArgumentException.class, () -> ContactNumberNormalizer.normalize(raw));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "-"})
    void blankOrMissingNumbersAreRejected(String raw) {
        assertThrows(IllegalArgumentException.class, () -> ContactNumberNormalizer.normalize(raw));
    }

    @Test
    void rejectionMessageNamesTheAcceptedFormats() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ContactNumberNormalizer.normalize("abc"));
        assertEquals(ContactNumberNormalizer.INVALID_MESSAGE, ex.getMessage());
    }
}
