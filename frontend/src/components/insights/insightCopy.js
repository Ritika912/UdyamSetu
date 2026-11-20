import { INSIGHT_PERIODS } from "../../api/insightsApi.js";

/** Period choices. Values are the backend InsightPeriod names. */
export const PERIOD_OPTIONS = Object.freeze([
  { value: INSIGHT_PERIODS.LAST_30_DAYS, label: "Last 30 days" },
  { value: INSIGHT_PERIODS.ALL_TIME, label: "All time" },
]);

/**
 * Owner-facing heading for each opportunity type. The API's own title uses internal terms
 * (for example "High LOST rate"), so the headings are written here in plain language.
 */
export const OPPORTUNITY_HEADINGS = Object.freeze({
  REPEATED_UNFULFILLED: "Customers keep asking for something you could not provide",
  FREQUENTLY_REQUESTED: "A service customers ask for often",
  HIGH_LOST_RATE: "Many customers did not proceed with this service",
  INCREASING_DEMAND: "Requests for this service are growing",
});

/**
 * Status labels used on the Insights page only. Keys are the backend EnquiryStatus values, which are
 * not changed. The labels match the plain wording agreed for owners. Other pages keep their own labels.
 */
export const INSIGHT_STATUS_LABELS = Object.freeze({
  NEW: "New",
  IN_PROGRESS: "In Progress",
  PENDING: "Pending",
  CONVERTED: "Converted",
  UNFULFILLED: "Service unavailable",
  LOST: "Customer did not proceed",
});

/**
 * Plain wording for suggested actions. Each entry replaces one fragment of the backend text and keeps
 * the rest of the sentence unchanged. Fragments are matched exactly; anything else is shown as received.
 */
const SUGGESTED_ACTION_WORDING = Object.freeze([
  {
    from: "mark it as not offered so enquiries are not taken for it",
    to: "deactivate it in Services so new enquiries cannot select it",
  },
  {
    from: "add it to the catalogue by hand",
    to: "add it to your service list by hand",
  },
  {
    from: "The system does not create catalogue entries from free text.",
    to: "The app does not add services to your list on its own.",
  },
  {
    from: "Consider adding it to the catalogue so it can be booked and tracked.",
    to: "Consider adding it to your service list so it can be booked and tracked.",
  },
  {
    from: "Unfulfilled demand is reported separately.",
    to: "Requests you could not provide are shown separately.",
  },
]);

/**
 * Plain wording for backend notes. Each pattern matches one fixed note template with its numbers; the
 * numbers are kept as received. Notes that match no pattern are shown as received.
 */
const NOTE_WORDING = Object.freeze([
  {
    pattern: /^(\d+) enquiries have no catalogue service and no requested service, and are excluded from service-level insights\.$/,
    to: (m) => `${m[1]} enquiries do not name a service, so they are left out of the service breakdowns.`,
  },
  {
    pattern: /^(\d+) enquiries have no catalogue service and no requested-service text, so they cannot be attributed to a service\.$/,
    to: (m) => `${m[1]} enquiries do not name a service, so they cannot be counted under any service.`,
  },
  {
    pattern: /^(\d+) service\(s\) have a high LOST share but fewer than (\d+) enquiries, so they are not reported as high-LOST\.$/,
    to: (m) =>
      `${m[1]} service(s) have many customers who did not proceed, but fewer than ${m[2]} enquiries, so they are not flagged.`,
  },
  {
    pattern: /^(\d+) service\(s\) have repeated UNFULFILLED enquiries but fewer than (\d+) enquiries in total, so no recommendation is made for them\.$/,
    to: (m) =>
      `${m[1]} service(s) have repeated requests for a service the business could not provide, but fewer than ${m[2]} enquiries in total, so no recommendation is made for them.`,
  },
  {
    pattern: /^(\d+) lower-priority opportunities were not shown \(limit (\d+)\)\.$/,
    to: (m) => `${m[1]} lower-priority recommendations are not shown (limit ${m[2]}).`,
  },
]);

/** Returns a backend note in plain business language, or the note unchanged when no pattern matches. */
export function plainNote(note) {
  for (const rule of NOTE_WORDING) {
    const match = rule.pattern.exec(note);
    if (match) return rule.to(match);
  }
  return note;
}

/** Returns a backend suggested action in plain business language, replacing only the known fragments. */
export function plainSuggestedAction(action) {
  return SUGGESTED_ACTION_WORDING.reduce(
    (text, rule) => text.replace(rule.from, rule.to),
    action ?? ""
  );
}

/** Where a service name came from: the owner's service list, or typed in on an enquiry. */
export function getServiceSourceLabel(serviceSource) {
  if (serviceSource === "CATALOGUE") return "From your service list";
  if (serviceSource === "FREE_TEXT") return "Typed in, not in your service list";
  return "";
}

/** Total enquiries from a statusCounts map (every status is present in the response). */
export function sumStatusCounts(statusCounts) {
  return Object.values(statusCounts ?? {}).reduce((total, count) => total + count, 0);
}
