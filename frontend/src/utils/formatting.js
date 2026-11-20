/**
 * Returns the initials of a name, e.g. "Rahul Sharma" -> "RS". Used for avatar circles.
 *
 * @param {string} name
 * @returns {string}
 */
export function getInitials(name) {
  if (!name) return "";

  return name
    .trim()
    .split(/\s+/)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

/**
 * Formats a backend LocalDate string (e.g. "2026-12-01") for display, without a time zone shift.
 *
 * @param {string} value
 * @returns {string}
 */
export function formatDate(value) {
  if (!value) return "";

  const [year, month, day] = String(value).split("-").map(Number);
  if (!year || !month || !day) return String(value);

  return new Intl.DateTimeFormat("en-IN", { dateStyle: "medium" }).format(new Date(year, month - 1, day));
}

/**
 * Formats a backend year-month string (e.g. "2026-11") as "Nov 2026".
 *
 * @param {string} value
 * @returns {string}
 */
export function formatMonth(value) {
  if (!value) return "";

  const [year, month] = String(value).split("-").map(Number);
  if (!year || !month) return String(value);

  return new Intl.DateTimeFormat("en-IN", { month: "short", year: "numeric" }).format(new Date(year, month - 1, 1));
}

/**
 * Formats a ratio (0.4) as a whole or one-decimal percentage ("40%", "33.3%").
 * Returns an empty string for null, so callers can hide the value when the backend withholds it.
 *
 * @param {number|null} ratio
 * @returns {string}
 */
export function formatPercent(ratio) {
  if (ratio === null || ratio === undefined) return "";
  return `${Number((Number(ratio) * 100).toFixed(1))}%`;
}

/**
 * Shortens a backend LocalTime string (e.g. "10:30:00") to "10:30".
 *
 * @param {string} value
 * @returns {string}
 */
export function formatTime(value) {
  if (!value) return "";
  return String(value).slice(0, 5);
}

/**
 * Formats a rupee amount (backend BigDecimal, received as a JSON number), e.g. 5000 -> "₹5,000".
 *
 * @param {number|null} value
 * @returns {string}
 */
export function formatRupees(value) {
  if (value === null || value === undefined) return "";
  return `₹${Number(value).toLocaleString("en-IN", { maximumFractionDigits: 2 })}`;
}

/**
 * Formats a backend LocalDateTime string (e.g. "2026-11-19T10:30:00") for display.
 * The value has no offset, so it is shown as written, without a time zone conversion.
 *
 * @param {string} value
 * @returns {string}
 */
export function formatDateTime(value) {
  if (!value) return "";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);

  return new Intl.DateTimeFormat("en-IN", { dateStyle: "medium", timeStyle: "short" }).format(date);
}
