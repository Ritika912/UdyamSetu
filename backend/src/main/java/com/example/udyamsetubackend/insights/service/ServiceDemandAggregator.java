package com.example.udyamsetubackend.insights.service;

import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import com.example.udyamsetubackend.enquiry.repository.FreeTextDemandRow;
import com.example.udyamsetubackend.enquiry.repository.LinkedServiceDemandRow;
import com.example.udyamsetubackend.insights.enums.ServiceSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The one place where enquiries are attributed to services. Both Business Insights and Opportunity Insights
 * use it, so they cannot disagree about identity.
 *
 * <ul>
 *   <li>Catalogue-linked enquiries (service_id set) are grouped by service id, as {@code SERVICE:<id>}. The name
 *       is the catalogue name.</li>
 *   <li>Enquiries with no catalogue link but with requested-service text are grouped as {@code TEXT:<normalised>}.
 *       The display name is the smallest original spelling, so it does not depend on row order.</li>
 *   <li>Historical enquiries with service_id NULL keep their text-based keys. Nothing is backfilled.</li>
 *   <li>Free text is never merged with a catalogue service, even when the names match.</li>
 * </ul>
 */
final class ServiceDemandAggregator {

    private ServiceDemandAggregator() {
    }

    /** Demand per service for the window in {@code window}, catalogue and free text together. */
    static List<ServiceDemand> load(EnquiryRepository repository, InsightWindow window) {
        LocalDateTime from = window.from();
        LocalDateTime to = window.toExclusive();
        LocalDateTime recentFrom = window.recentHalfStart();

        List<ServiceDemand> demand = new ArrayList<>();
        demand.addAll(catalogueDemand(repository, from, to, recentFrom));
        demand.addAll(freeTextDemand(repository, from, to, recentFrom));
        return demand;
    }

    private static List<ServiceDemand> catalogueDemand(EnquiryRepository repository, LocalDateTime from,
                                                       LocalDateTime to, LocalDateTime recentFrom) {
        Map<Long, Accumulator> byService = new LinkedHashMap<>();
        for (LinkedServiceDemandRow row : repository.countLinkedServiceDemand(from, to, recentFrom)) {
            Accumulator acc = byService.computeIfAbsent(row.getServiceId(),
                    id -> new Accumulator(row.getServiceName(), row.getActive()));
            acc.add(row.getStatus(), row.getTotal(), row.getRecentTotal());
        }
        List<ServiceDemand> result = new ArrayList<>();
        for (Map.Entry<Long, Accumulator> entry : byService.entrySet()) {
            Accumulator acc = entry.getValue();
            result.add(new ServiceDemand(ServiceKeys.forCatalogueService(entry.getKey()), ServiceSource.CATALOGUE,
                    acc.name, acc.active, acc.counts, acc.recent));
        }
        return result;
    }

    private static List<ServiceDemand> freeTextDemand(EnquiryRepository repository, LocalDateTime from,
                                                      LocalDateTime to, LocalDateTime recentFrom) {
        Map<String, Accumulator> byKey = new LinkedHashMap<>();
        for (FreeTextDemandRow row : repository.countFreeTextDemand(from, to, recentFrom)) {
            String key = ServiceKeys.forFreeText(row.getRawText());
            Accumulator acc = byKey.computeIfAbsent(key, k -> new Accumulator(null, null));
            acc.addSpelling(row.getRawText());
            acc.add(EnquiryStatus.valueOf(row.getStatus()), row.getTotal(), row.getRecentTotal());
        }
        List<ServiceDemand> result = new ArrayList<>();
        for (Map.Entry<String, Accumulator> entry : byKey.entrySet()) {
            Accumulator acc = entry.getValue();
            result.add(new ServiceDemand(entry.getKey(), ServiceSource.FREE_TEXT, acc.name, null,
                    acc.counts, acc.recent));
        }
        return result;
    }

    /** Collects counts for one service while the rows are read. */
    private static final class Accumulator {
        private String name;
        private final Boolean active;
        private final Map<EnquiryStatus, Long> counts = new EnumMap<>(EnquiryStatus.class);
        private long recent;

        Accumulator(String name, Boolean active) {
            this.name = name;
            this.active = active;
        }

        void add(EnquiryStatus status, Long total, Long recentTotal) {
            counts.merge(status, total, Long::sum);
            recent += recentTotal == null ? 0 : recentTotal;
        }

        /** Keeps the smallest original spelling, so the display name does not depend on row order. */
        void addSpelling(String spelling) {
            if (name == null || spelling.compareTo(name) < 0) {
                name = spelling;
            }
        }
    }
}
