package com.example.udyamsetubackend.enquiry.repository;

/**
 * Enquiry count for one time bucket, formatted by the database (for example {@code 2026-11-19} or {@code 2026-11}).
 */
public interface TrendCountRow {

    String getBucket();

    Long getTotal();
}
