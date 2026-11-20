package com.example.udyamsetubackend.enquiry.repository;

/**
 * Demand for one exact trimmed free-text spelling and status. Case and whitespace are normalised in Java, not
 * here, so that the grouping rules live in one place. {@code getStatus()} is the stored enum name.
 */
public interface FreeTextDemandRow {

    String getRawText();

    String getStatus();

    Long getTotal();

    Long getRecentTotal();
}
