import { apiClient } from "./client.js";

/** Accepted values for the `period` query parameter (InsightPeriod enum). */
export const INSIGHT_PERIODS = Object.freeze({
  LAST_30_DAYS: "LAST_30_DAYS",
  ALL_TIME: "ALL_TIME",
});

/**
 * GET /api/insights/business?period=LAST_30_DAYS|ALL_TIME
 *
 * Response (BusinessInsightsResponse): period, from, to (yyyy-MM-dd), totalEnquiries,
 * withoutRequestedService, statusCounts (map of status name to count),
 * mostRequestedServices[], repeatedUnfulfilledDemand[], highLostRateServices[],
 * demandTrend, notes[], thresholds.
 *
 * @param {{ period?: keyof typeof INSIGHT_PERIODS, signal?: AbortSignal }} [options]
 */
export function getBusinessInsights({ period = INSIGHT_PERIODS.LAST_30_DAYS, signal } = {}) {
  return apiClient.get("/insights/business", { params: { period }, signal });
}

/**
 * GET /api/insights/opportunities?period=LAST_30_DAYS|ALL_TIME
 *
 * Response (OpportunityInsightsResponse): period, from, to, totalEnquiries,
 * opportunities[] (type, category, title, explanation, serviceKey, serviceName,
 * serviceSource, serviceActive, supportingEnquiryCount, recentCount, previousCount,
 * statusCounts, periodFrom, periodTo, suggestedAction), notes[], thresholds.
 *
 * @param {{ period?: keyof typeof INSIGHT_PERIODS, signal?: AbortSignal }} [options]
 */
export function getOpportunities({ period = INSIGHT_PERIODS.LAST_30_DAYS, signal } = {}) {
  return apiClient.get("/insights/opportunities", { params: { period }, signal });
}
