/**
 * Owner-friendly labels and style tones for the backend EnquiryStatus enum.
 * Keys must match the backend values exactly. `tone` is the CSS modifier used by .status-badge.
 */
const ENQUIRY_STATUS_DISPLAY = Object.freeze({
  NEW: { label: "New", tone: "new" },
  IN_PROGRESS: { label: "In Progress", tone: "in-progress" },
  PENDING: { label: "Pending", tone: "pending" },
  CONVERTED: { label: "Converted", tone: "converted" },
  UNFULFILLED: { label: "Service unavailable", tone: "unfulfilled" },
  LOST: { label: "Customer did not proceed", tone: "lost" },
});

/** Backend EnquiryStatus values in workflow order. Used for filters and status selectors. */
export const ENQUIRY_STATUS_VALUES = Object.freeze(Object.keys(ENQUIRY_STATUS_DISPLAY));

/**
 * Returns the display label and tone for a status. Unknown values are shown as received,
 * so a new backend status is visible rather than hidden.
 *
 * @param {string} status backend enum name, e.g. "IN_PROGRESS"
 * @returns {{ label: string, tone: string }}
 */
export function getEnquiryStatusDisplay(status) {
  return ENQUIRY_STATUS_DISPLAY[status] ?? { label: String(status ?? "Unknown"), tone: "unknown" };
}
