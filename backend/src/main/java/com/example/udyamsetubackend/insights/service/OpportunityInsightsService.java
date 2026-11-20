package com.example.udyamsetubackend.insights.service;

import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import com.example.udyamsetubackend.insights.dto.OpportunityInsightsResponse;
import com.example.udyamsetubackend.insights.dto.OpportunityInsightsResponse.Opportunity;
import com.example.udyamsetubackend.insights.dto.OpportunityInsightsResponse.Thresholds;
import com.example.udyamsetubackend.insights.enums.InsightPeriod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.example.udyamsetubackend.insights.service.InsightThresholds.HIGH_LOST_RATE;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.LAST_DAYS_WINDOW;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MAX_OPPORTUNITIES;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_FREQUENT_REQUESTS;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_FREQUENT_SHARE;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_PREVIOUS_FOR_GROWTH;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_RECENT_FOR_GROWTH;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_REPEATED_UNFULFILLED;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_SAMPLE_SIZE;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.TREND_CHANGE_RATIO;

/**
 * Builds rule-based business opportunities for a reporting period. Service identity comes from
 * {@link ServiceDemandAggregator}, so it matches Business Insights exactly.
 *
 * <p>Catalogue-linked demand reads {@code enquiries.service_id}, which exists after migrations 002 and 003.
 */
@Service
public class OpportunityInsightsService {

    private final EnquiryRepository enquiryRepository;

    public OpportunityInsightsService(EnquiryRepository enquiryRepository) {
        this.enquiryRepository = enquiryRepository;
    }

    @Transactional(readOnly = true)
    public OpportunityInsightsResponse getOpportunities(InsightPeriod period) {
        return getOpportunities(period, LocalDate.now());
    }

    /**
     * @param today the last day of the window (inclusive). Passed in so the result is reproducible in tests.
     */
    @Transactional(readOnly = true)
    public OpportunityInsightsResponse getOpportunities(InsightPeriod period, LocalDate today) {
        InsightWindow window = InsightWindow.of(period, today, enquiryRepository::findEarliestCreatedAt);

        long total = InsightQueries.statusCounts(enquiryRepository, window.from(), window.toExclusive())
                .values().stream().mapToLong(Long::longValue).sum();

        List<ServiceDemand> demand = ServiceDemandAggregator.load(enquiryRepository, window);

        List<Opportunity> all = OpportunityRules.evaluateAll(demand, total, window.fromDate(), window.toDate());
        List<Opportunity> shown = all.stream().limit(MAX_OPPORTUNITIES).toList();

        List<String> notes = notes(demand, total);
        if (all.size() > shown.size()) {
            notes.add((all.size() - shown.size()) + " lower-priority opportunities were not shown (limit "
                    + MAX_OPPORTUNITIES + ").");
        }

        return new OpportunityInsightsResponse(
                period,
                window.fromDate(),
                window.toDate(),
                total,
                shown,
                notes,
                thresholds());
    }

    private static List<String> notes(List<ServiceDemand> demand, long total) {
        List<String> notes = new ArrayList<>();
        long accountedFor = demand.stream().mapToLong(ServiceDemand::total).sum();
        if (total == 0) {
            notes.add("No enquiries were created in this period.");
        }
        if (total - accountedFor > 0) {
            notes.add((total - accountedFor) + " enquiries have no catalogue service and no requested-service text, "
                    + "so they cannot be attributed to a service.");
        }
        long smallSample = OpportunityRules.smallSampleUnfulfilledCount(demand);
        if (smallSample > 0) {
            notes.add(smallSample + " service(s) have repeated UNFULFILLED enquiries but fewer than "
                    + MIN_SAMPLE_SIZE + " enquiries in total, so no recommendation is made for them.");
        }
        return notes;
    }

    private static Thresholds thresholds() {
        return new Thresholds(
                LAST_DAYS_WINDOW,
                MIN_SAMPLE_SIZE,
                MIN_REPEATED_UNFULFILLED,
                MIN_FREQUENT_REQUESTS,
                MIN_FREQUENT_SHARE,
                HIGH_LOST_RATE,
                MIN_RECENT_FOR_GROWTH,
                MIN_PREVIOUS_FOR_GROWTH,
                TREND_CHANGE_RATIO,
                MAX_OPPORTUNITIES);
    }
}
