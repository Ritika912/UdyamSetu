import { apiClient } from "./client.js";

/** Enquiry sources, matching the backend EnquirySource enum. Labels are shown to owners. */
export const ENQUIRY_SOURCE_OPTIONS = Object.freeze([
  { value: "WEBSITE", label: "Website" },
  { value: "PHONE", label: "Phone call" },
  { value: "WHATSAPP", label: "WhatsApp" },
  { value: "WALK_IN", label: "Walk-in" },
  { value: "SOCIAL_MEDIA", label: "Social media" },
  { value: "OTHER", label: "Other" },
]);

/** Service selector value meaning "a service that is not in the catalogue" (free text). */
export const OTHER_SERVICE_CHOICE = "OTHER";

/**
 * GET /api/enquiries?status=&customerId=
 *
 * Response: EnquiryResponse[] sorted by createdAt descending, then id descending.
 * The endpoint has no limit parameter; callers that need a short list slice the result.
 *
 * EnquiryResponse: id, customerId, serviceId, customerName, contactNumber, source,
 * requestedService, requirements, occasion, budget, expectedDate, followUpDate,
 * followUpTime, followUpNotes, status (EnquiryStatus), createdAt (yyyy-MM-ddTHH:mm:ss).
 *
 * @param {{ status?: string, customerId?: number, signal?: AbortSignal }} [options]
 */
export function getEnquiries({ status, customerId, signal } = {}) {
  return apiClient.get("/enquiries", { params: { status, customerId }, signal });
}

/**
 * PATCH /api/enquiries/{id}/status. Body: EnquiryStatusRequest { status } (status is required).
 * Any status may be set; the backend enforces no transition rules.
 * Resolves with the updated EnquiryResponse. Rejects with ApiError on failure.
 *
 * @param {number} enquiryId
 * @param {string} status  EnquiryStatus enum name, e.g. "UNFULFILLED"
 * @param {{ signal?: AbortSignal }} [options]
 */
export function updateEnquiryStatus(enquiryId, status, { signal } = {}) {
  return apiClient.patch(`/enquiries/${enquiryId}/status`, { body: { status }, signal });
}

/** Owner-facing label for an EnquiryResponse.source value. Returns null for a missing source. */
export function getEnquirySourceLabel(source) {
  if (!source) return null;
  const option = ENQUIRY_SOURCE_OPTIONS.find((item) => item.value === source);
  return option ? option.label : String(source);
}

/**
 * POST /api/enquiries. Body: EnquiryRequest (see toEnquiryRequest).
 * Resolves with the saved EnquiryResponse (HTTP 201). Rejects with ApiError on failure.
 *
 * @param {object} request  output of toEnquiryRequest
 * @param {{ signal?: AbortSignal }} [options]
 */
export function createEnquiry(request, { signal } = {}) {
  return apiClient.post("/enquiries", { body: request, signal });
}

function optionalText(value) {
  const trimmed = value.trim();
  return trimmed === "" ? null : trimmed;
}

function optionalNumber(value) {
  const trimmed = value.trim();
  return trimmed === "" ? null : Number(trimmed);
}

/** Form time values are "HH:mm"; the backend LocalTime field is written with seconds. */
function optionalTime(value) {
  if (!value) return null;
  return value.length === 5 ? `${value}:00` : value;
}

/**
 * Maps the Add Enquiry form values (all strings, as typed) to the backend EnquiryRequest.
 *
 * Exactly the 12 EnquiryRequest fields are produced. Empty optional values are sent as null.
 * A catalogue service is sent as serviceId with requestedService left null (the backend copies
 * the catalogue name). The "other service" choice is sent as requestedService text with serviceId null.
 * Fields the form does not offer (for example, a business name) are never sent.
 *
 * @param {object} form  values from AddEnquiryModal
 * @returns {object} EnquiryRequest payload
 */
export function toEnquiryRequest(form) {
  const isCatalogueService =
    form.serviceChoice !== "" && form.serviceChoice !== OTHER_SERVICE_CHOICE;

  return {
    customerName: form.customerName.trim(),
    contactNumber: form.contactNumber.trim(),
    source: form.source || null,
    serviceId: isCatalogueService ? Number(form.serviceChoice) : null,
    requestedService:
      form.serviceChoice === OTHER_SERVICE_CHOICE ? optionalText(form.requestedService) : null,
    requirements: optionalText(form.requirements),
    occasion: optionalText(form.occasion),
    budget: optionalNumber(form.budget),
    expectedDate: form.expectedDate || null,
    followUpDate: form.followUpDate || null,
    followUpTime: optionalTime(form.followUpTime),
    followUpNotes: optionalText(form.followUpNotes),
  };
}
