package com.example.udyamsetubackend.catalog.dto;

import com.example.udyamsetubackend.catalog.entity.ServiceOffering;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bean Validation rules for ServiceRequest, checked with the Jakarta Validator directly.
 * No Spring context and no database are involved.
 */
class ServiceDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private static ServiceRequest request(String name, String description, String category,
                                          BigDecimal price, Boolean isActive) {
        return new ServiceRequest(name, description, category, price, isActive);
    }

    private Set<ConstraintViolation<ServiceRequest>> violations(ServiceRequest request) {
        return validator.validate(request);
    }

    private static String repeat(char c, int count) {
        return String.valueOf(c).repeat(count);
    }

    @Test
    void nameOnlyRequestIsValid() {
        assertTrue(violations(request("Haircut", null, null, null, null)).isEmpty());
    }

    @Test
    void fullRequestIsValid() {
        assertTrue(violations(request("Bridal Makeup", "Full bridal look", "Makeup",
                new BigDecimal("4500.00"), true)).isEmpty());
    }

    @Test
    void nullNameIsRejected() {
        assertFalse(violations(request(null, null, null, null, null)).isEmpty());
    }

    @Test
    void blankNameIsRejectedIncludingWhitespaceOnly() {
        assertFalse(violations(request("", null, null, null, null)).isEmpty());
        assertFalse(violations(request("   ", null, null, null, null)).isEmpty());
    }

    @Test
    void nameAtMaximumLengthIsValidAndOneOverIsRejected() {
        assertTrue(violations(request(repeat('a', 150), null, null, null, null)).isEmpty());
        assertFalse(violations(request(repeat('a', 151), null, null, null, null)).isEmpty());
    }

    @Test
    void descriptionAtMaximumLengthIsValidAndOneOverIsRejected() {
        assertTrue(violations(request("Haircut", repeat('d', 2000), null, null, null)).isEmpty());
        assertFalse(violations(request("Haircut", repeat('d', 2001), null, null, null)).isEmpty());
    }

    @Test
    void categoryAtMaximumLengthIsValidAndOneOverIsRejected() {
        assertTrue(violations(request("Haircut", null, repeat('c', 100), null, null)).isEmpty());
        assertFalse(violations(request("Haircut", null, repeat('c', 101), null, null)).isEmpty());
    }

    @Test
    void zeroPriceIsValidAndNegativePriceIsRejected() {
        assertTrue(violations(request("Free Consult", null, null, new BigDecimal("0.00"), null)).isEmpty());
        assertFalse(violations(request("Haircut", null, null, new BigDecimal("-0.01"), null)).isEmpty());
    }

    @Test
    void priceDigitLimitsAreEnforced() {
        // 10 integer digits and 2 decimals is the maximum (DECIMAL(12,2)).
        assertTrue(violations(request("Big", null, null, new BigDecimal("9999999999.99"), null)).isEmpty());
        assertFalse(violations(request("Big", null, null, new BigDecimal("99999999999.00"), null)).isEmpty());
        assertFalse(violations(request("Precise", null, null, new BigDecimal("10.001"), null)).isEmpty());
    }

    @Test
    void nullPriceAndNullIsActiveAreAccepted() {
        assertTrue(violations(request("Haircut", null, null, null, null)).isEmpty());
        assertTrue(violations(request("Haircut", null, null, null, false)).isEmpty());
    }

    @Test
    void responseMapsEveryEntityFieldIncludingIsActive() {
        ServiceOffering entity = new ServiceOffering("Facial");
        entity.setDescription("Deep cleanse");
        entity.setCategory("Skin");
        entity.setPrice(new BigDecimal("800.00"));
        entity.setActive(false);

        ServiceResponse response = ServiceResponse.from(entity);

        assertEquals("Facial", response.name());
        assertEquals("Deep cleanse", response.description());
        assertEquals("Skin", response.category());
        assertEquals(new BigDecimal("800.00"), response.price());
        assertFalse(response.isActive());
        assertEquals(null, response.id());
        assertEquals(null, response.createdAt());
        assertEquals(null, response.updatedAt());
    }

    @Test
    void responseExposesCreatedAndUpdatedTimestampTypes() {
        assertEquals(LocalDateTime.class,
                ServiceResponse.class.getRecordComponents()[6].getType());
        assertEquals(LocalDateTime.class,
                ServiceResponse.class.getRecordComponents()[7].getType());
    }
}
