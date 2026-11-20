package com.example.udyamsetubackend.insights.service;

import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import com.example.udyamsetubackend.enquiry.repository.TrendCountRow;
import com.example.udyamsetubackend.insights.dto.BusinessInsightsResponse;
import com.example.udyamsetubackend.insights.dto.BusinessInsightsResponse.Thresholds;
import com.example.udyamsetubackend.insights.dto.BusinessInsightsResponse.TrendInsight;
import com.example.udyamsetubackend.insights.enums.InsightPeriod;
import com.example.udyamsetubackend.insights.enums.TrendGranularity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.udyamsetubackend.insights.service.InsightThresholds.HIGH_LOST_RATE;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.LAST_DAYS_WINDOW;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_REPEATED_UNFULFILLED;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_SAMPLE_SIZE;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_TREND_BUCKETS;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.MIN_TREND_ENQUIRIES;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.TOP_SERVICE_COUNT;
import static com.example.udyamsetubackend.insights.service.InsightThresholds.TREND_CHANGE_RATIO;

/**
 * Builds business insights for a reporting period. Counting is done in SQL; service identity comes from
 * {@link ServiceDemandAggregator}, the same source Opportunity Insights uses.
 */
@Service
public class BusinessInsightsService {

    private static final String DAY_FORMAT = "%Y-%m-%d";
    private static final String MONTH_FORMAT = "%Y-%m";

    private final EnquiryRepository enquiryRepository;

    public BusinessInsightsService(EnquiryRepository enquiryRepository) {
        this.enquiryRepository = enquiryRepository;
    }

    @Transactional(readOnly = true)
    public BusinessInsightsResponse getInsights(InsightPeriod period) {
        return getInsights(period, LocalDate.now());
    }

    /**
     * @param today the last day of the window (inclusive). Passed in so the result is reproducible in tests.
     */
    @Transactional(readOnly = true)
    public BusinessInsightsResponse getInsights(InsightPeriod period, LocalDate today) {
        boolean allTime = period == InsightPeriod.ALL_TIME;
        InsightWindow window = InsightWindow.of(period, today, enquiryRepository::findEarliestCreatedAt);
        LocalDateTime from = window.from();
        LocalDateTime to = window.toExclusive();

        Map<EnquiryStatus, Long> statusCounts = InsightQueries.statusCounts(enquiryRepository, from, to);
        long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();

        List<ServiceDemand> demand = ServiceDemandAggregator.load(enquiryRepository, window);
        long withService = demand.stream().mapToLong(ServiceDemand::total).sum();
        long withoutService = total - withService;

        TrendGranularity granularity = allTime ? TrendGranularity.MONTH : TrendGranularity.DAY;
        Map<String, Long> observed = trendCounts(granularity, from, to);
        TrendInsight trend = BusinessInsightRules.trend(granularity,
                expandBuckets(granularity, window.fromDate(), today, observed));

        return new BusinessInsightsResponse(
                period,
                window.fromDate(),
                today,
                total,
                withoutService,
                statusCounts,
                BusinessInsightRules.mostRequested(demand, total, TOP_SERVICE_COUNT),
                BusinessInsightRules.repeatedUnfulfilled(demand),
                BusinessInsightRules.highLostRate(demand),
                trend,
                notes(total, withoutService, demand),
                thresholds());
    }

    private Map<String, Long> trendCounts(TrendGranularity granularity, LocalDateTime from, LocalDateTime to) {
        String format = granularity == TrendGranularity.DAY ? DAY_FORMAT : MONTH_FORMAT;
        Map<String, Long> counts = new LinkedHashMap<>();
        for (TrendCountRow row : enquiryRepository.countTrend(format, from, to)) {
            counts.merge(row.getBucket(), row.getTotal(), Long::sum);
        }
        return counts;
    }

    /**
     * Returns every bucket in the window in order, with zero for buckets that had no enquiries, so gaps
     * show as gaps. For months the range starts at the earliest observed month, so a short history is not
     * padded with empty months.
     */
    private static Map<String, Long> expandBuckets(TrendGranularity granularity, LocalDate fromDate,
                                                   LocalDate today, Map<String, Long> observed) {
        Map<String, Long> buckets = new LinkedHashMap<>();
        if (granularity == TrendGranularity.DAY) {
            for (LocalDate day = fromDate; !day.isAfter(today); day = day.plusDays(1)) {
                buckets.put(day.toString(), observed.getOrDefault(day.toString(), 0L));
            }
            return buckets;
        }
        if (observed.isEmpty()) {
            return buckets;
        }
        YearMonth start = observed.keySet().stream().map(YearMonth::parse).min(YearMonth::compareTo).orElseThrow();
        YearMonth end = YearMonth.from(today);
        for (YearMonth month = start; !month.isAfter(end); month = month.plusMonths(1)) {
            buckets.put(month.toString(), observed.getOrDefault(month.toString(), 0L));
        }
        return buckets;
    }

    private static List<String> notes(long total, long withoutService, List<ServiceDemand> demand) {
        List<String> notes = new ArrayList<>();
        if (total == 0) {
            notes.add("No enquiries were created in this period.");
        }
        if (withoutService > 0) {
            notes.add(withoutService + " enquiries have no catalogue service and no requested service, and are "
                    + "excluded from service-level insights.");
        }
        long smallSampleHighLost = BusinessInsightRules.smallSampleHighLostCount(demand);
        if (smallSampleHighLost > 0) {
            notes.add(smallSampleHighLost + " service(s) have a high LOST share but fewer than "
                    + MIN_SAMPLE_SIZE + " enquiries, so they are not reported as high-LOST.");
        }
        return notes;
    }

    private static Thresholds thresholds() {
        return new Thresholds(
                LAST_DAYS_WINDOW,
                TOP_SERVICE_COUNT,
                MIN_REPEATED_UNFULFILLED,
                MIN_SAMPLE_SIZE,
                HIGH_LOST_RATE,
                MIN_TREND_ENQUIRIES,
                MIN_TREND_BUCKETS,
                TREND_CHANGE_RATIO);
    }
}
