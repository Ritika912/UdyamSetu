package com.example.udyamsetubackend.insights.service;

import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.insights.dto.OpportunityInsightsResponse.Opportunity;
import com.example.udyamsetubackend.insights.enums.OpportunityType;
import com.example.udyamsetubackend.insights.enums.ServiceSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.example.udyamsetubackend.insights.service.InsightThresholds.HIGH_LOST_RATE;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MAX_OPPORTUNITIES;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_FREQUENT_REQUESTS;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_FREQUENT_SHARE;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_PREVIOUS_FOR_GROWTH;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_RECENT_FOR_GROWTH;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_REPEATED_UNFULFILLED;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_SAMPLE_SIZE;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.TREND_CHANGE_RATIO;

/**
 * Rule-based opportunities. Each rule reads counts and returns a recommendation with the counts behind it, or
 * nothing. Pure functions with no framework or database access.
 *
 * <p>Rules, in brief:
 * <ul>
 *   <li>Repeated UNFULFILLED: the business could not provide the service at least twice, out of at least
 *       {@link InsightThresholds#MIN_SAMPLE_SIZE} enquiries.</li>
 *   <li>Frequently requested: at least {@link InsightThresholds#MIN_FREQUENT_REQUESTS} enquiries and at least
 *       {@link InsightThresholds#MIN_FREQUENT_SHARE} of the period's enquiries.</li>
 *   <li>High LOST rate: LOST share at or above {@link InsightThresholds#HIGH_LOST_RATE}. LOST is customer
 *       attrition, not unmet demand, so it is never counted as UNFULFILLED.</li>
 *   <li>Increasing demand: the recent half exceeds the earlier half by more than
 *       {@link InsightThresholds#TREND_CHANGE_RATIO}, with enough recent enquiries and a non-zero earlier baseline
 *       ({@link InsightThresholds#MIN_PREVIOUS_FOR_GROWTH}). Demand that appears from nothing is not called a trend.</li>
 * </ul>
 */
public final class OpportunityRules {

    private static final Comparator<Opportunity> MOST_SIGNIFICANT_FIRST =
            Comparator.comparingLong(Opportunity::supportingEnquiryCount).reversed()
                    .thenComparing(Opportunity::type)
                    .thenComparing(Opportunity::serviceName, String.CASE_INSENSITIVE_ORDER);

    private OpportunityRules() {
    }

    /**
     * Every recommendation, most significant first (by supporting enquiry count, then type, then name). Not
     * capped; callers decide how many to show and must report any omitted.
     */
    public static List<Opportunity> evaluateAll(List<ServiceDemand> demand, long totalEnquiries,
                                                LocalDate periodFrom, LocalDate periodTo) {
        List<Opportunity> found = new ArrayList<>();
        for (ServiceDemand service : demand) {
            repeatedUnfulfilled(service, periodFrom, periodTo).ifPresent(found::add);
            frequentlyRequested(service, totalEnquiries, periodFrom, periodTo).ifPresent(found::add);
            highLostRate(service, periodFrom, periodTo).ifPresent(found::add);
            increasingDemand(service, periodFrom, periodTo).ifPresent(found::add);
        }
        return found.stream().sorted(MOST_SIGNIFICANT_FIRST).toList();
    }

    /** {@link #evaluateAll} capped at {@link InsightThresholds#MAX_OPPORTUNITIES}. */
    public static List<Opportunity> evaluate(List<ServiceDemand> demand, long totalEnquiries,
                                             LocalDate periodFrom, LocalDate periodTo) {
        return evaluateAll(demand, totalEnquiries, periodFrom, periodTo).stream().limit(MAX_OPPORTUNITIES).toList();
    }

    /**
     * Services with at least {@link InsightThresholds#MIN_REPEATED_UNFULFILLED} UNFULFILLED enquiries but fewer
     * than {@link InsightThresholds#MIN_SAMPLE_SIZE} in total. They are left out on purpose.
     */
    public static long smallSampleUnfulfilledCount(List<ServiceDemand> demand) {
        return demand.stream()
                .filter(s -> s.count(EnquiryStatus.UNFULFILLED) >= MIN_REPEATED_UNFULFILLED)
                .filter(s -> s.total() < MIN_SAMPLE_SIZE)
                .count();
    }

    static Optional<Opportunity> repeatedUnfulfilled(ServiceDemand s, LocalDate from, LocalDate to) {
        long unfulfilled = s.count(EnquiryStatus.UNFULFILLED);
        if (unfulfilled < MIN_REPEATED_UNFULFILLED || s.total() < MIN_SAMPLE_SIZE) {
            return Optional.empty();
        }
        String explanation = "The business could not provide \"" + s.serviceName() + "\" in " + unfulfilled
                + " of " + s.total() + " enquiries (" + pct(unfulfilled, s.total()) + ") this period.";
        String action = s.source() == ServiceSource.CATALOGUE
                ? "Check whether you can offer this service reliably (staff, materials, pricing). If you can, "
                + "keep it bookable. If not, mark it as not offered so enquiries are not taken for it."
                : "Decide whether this is a service you could offer. If it is, add it to the catalogue by hand. "
                + "The system does not create catalogue entries from free text.";
        return Optional.of(build(OpportunityType.REPEATED_UNFULFILLED, s, unfulfilled,
                "Repeated unmet demand: " + s.serviceName(), explanation, action, from, to));
    }

    static Optional<Opportunity> frequentlyRequested(ServiceDemand s, long totalEnquiries,
                                                     LocalDate from, LocalDate to) {
        if (s.total() < MIN_FREQUENT_REQUESTS || totalEnquiries <= 0) {
            return Optional.empty();
        }
        BigDecimal share = BusinessInsightRules.rate(s.total(), totalEnquiries);
        if (share.compareTo(MIN_FREQUENT_SHARE) < 0) {
            return Optional.empty();
        }
        String explanation = s.total() + " of " + totalEnquiries + " enquiries this period (" + percent(share)
                + ") asked for \"" + s.serviceName() + "\".";
        String action = s.source() == ServiceSource.CATALOGUE
                ? "Make sure this service is priced, described and easy to book, and consider featuring it."
                : "Consider adding it to the catalogue so it can be booked and tracked.";
        return Optional.of(build(OpportunityType.FREQUENTLY_REQUESTED, s, s.total(),
                "Frequently requested: " + s.serviceName(), explanation, action, from, to));
    }

    static Optional<Opportunity> highLostRate(ServiceDemand s, LocalDate from, LocalDate to) {
        long lost = s.count(EnquiryStatus.LOST);
        if (s.total() < MIN_SAMPLE_SIZE || BusinessInsightRules.rate(lost, s.total()).compareTo(HIGH_LOST_RATE) < 0) {
            return Optional.empty();
        }
        String explanation = lost + " of " + s.total() + " enquiries for \"" + s.serviceName() + "\" ("
                + pct(lost, s.total()) + ") ended LOST: the customer did not proceed for another reason. "
                + "This is not unmet service demand.";
        String action = "Review how these enquiries were followed up: response time, quoted price, and follow-up "
                + "notes. Unfulfilled demand is reported separately.";
        return Optional.of(build(OpportunityType.HIGH_LOST_RATE, s, lost,
                "High LOST rate: " + s.serviceName(), explanation, action, from, to));
    }

    static Optional<Opportunity> increasingDemand(ServiceDemand s, LocalDate from, LocalDate to) {
        long recent = s.recentCount();
        long previous = s.previousCount();
        if (s.total() < MIN_SAMPLE_SIZE || recent < MIN_RECENT_FOR_GROWTH || previous < MIN_PREVIOUS_FOR_GROWTH) {
            return Optional.empty();
        }
        BigDecimal threshold = BigDecimal.valueOf(previous).multiply(BigDecimal.ONE.add(TREND_CHANGE_RATIO));
        if (BigDecimal.valueOf(recent).compareTo(threshold) <= 0) {
            return Optional.empty();
        }
        String explanation = "\"" + s.serviceName() + "\" had " + recent + " enquiries in the recent half of the "
                + "period, compared with " + previous + " in the earlier half.";
        String action = "Check capacity, stock and staffing for this service before demand grows further, and "
                + "consider highlighting it.";
        return Optional.of(build(OpportunityType.INCREASING_DEMAND, s, recent,
                "Increasing demand: " + s.serviceName(), explanation, action, from, to));
    }

    private static Opportunity build(OpportunityType type, ServiceDemand s, long supporting, String title,
                                     String explanation, String action, LocalDate from, LocalDate to) {
        String fullExplanation = s.active() != null && !s.active()
                ? explanation + " This service is currently deactivated in the catalogue."
                : explanation;
        return new Opportunity(
                type,
                type.category(),
                title,
                fullExplanation,
                s.serviceKey(),
                s.serviceName(),
                s.source(),
                s.active(),
                supporting,
                s.recentCount(),
                s.previousCount(),
                BusinessInsightRules.fullStatusMap(s.countsByStatus()),
                from,
                to,
                action);
    }

    private static String pct(long part, long total) {
        return percent(BusinessInsightRules.rate(part, total));
    }

    private static String percent(BigDecimal ratio) {
        return BusinessInsightRules.percent(ratio);
    }
}
