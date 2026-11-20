import { apiClient } from "./client.js";

/**
 * GET /api/dashboard
 *
 * Response (DashboardResponse):
 *   totalEnquiries:    number
 *   newEnquiries:      number
 *   followUpsDueToday: number
 *   overdueFollowUps:  number
 *
 * @param {{ signal?: AbortSignal }} [options]
 */
export function getDashboard({ signal } = {}) {
  return apiClient.get("/dashboard", { signal });
}
