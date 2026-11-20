package com.example.udyamsetubackend.insights.service;

import java.util.Locale;

/**
 * Identity of a service for opportunity analysis.
 *
 * <ul>
 *   <li>{@code SERVICE:<id>} for enquiries linked to a catalogue service.</li>
 *   <li>{@code TEXT:<normalised>} for free-text requests. Normalised means trimmed, lower-cased, and with runs
 *       of whitespace collapsed to one space.</li>
 * </ul>
 *
 * <p>Free text is never matched to a catalogue service by similar names. That is a deliberate rule.
 */
public final class ServiceKeys {

    public static final String SERVICE_PREFIX = "SERVICE:";
    public static final String TEXT_PREFIX = "TEXT:";

    private ServiceKeys() {
    }

    public static String forCatalogueService(long serviceId) {
        return SERVICE_PREFIX + serviceId;
    }

    public static String forFreeText(String requestedService) {
        return TEXT_PREFIX + normalizeText(requestedService);
    }

    /** Trim, lower-case (root locale), and collapse internal whitespace runs into a single space. */
    public static String normalizeText(String text) {
        return text.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
