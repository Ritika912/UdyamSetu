import { apiClient } from "./client.js";

/**
 * GET /api/services?active=true|false (the active parameter is optional; omit it for all services).
 *
 * Response: ServiceResponse[] ordered by name. Each item: id, name, description, category, price
 * (JSON number or null), isActive (boolean), createdAt, updatedAt.
 *
 * @param {{ active?: boolean, signal?: AbortSignal }} [options]
 */
export function getServices({ active, signal } = {}) {
  return apiClient.get("/services", { params: { active }, signal });
}

/**
 * Active services only. Used by the Add Enquiry service selector.
 *
 * @param {{ signal?: AbortSignal }} [options]
 */
export function getActiveServices({ signal } = {}) {
  return getServices({ active: true, signal });
}

/**
 * POST /api/services. Body: ServiceRequest. Resolves with ServiceResponse (HTTP 201).
 * 409 if another service already has the name (case-insensitive).
 *
 * @param {object} request
 * @param {{ signal?: AbortSignal }} [options]
 */
export function createService(request, { signal } = {}) {
  return apiClient.post("/services", { body: request, signal });
}

/**
 * PUT /api/services/{id}. Body: ServiceRequest. Replaces name, description, category and price.
 * A null isActive leaves the status unchanged; true reactivates. Resolves with ServiceResponse.
 * 409 if another service already has the name.
 *
 * @param {number} serviceId
 * @param {object} request
 * @param {{ signal?: AbortSignal }} [options]
 */
export function updateService(serviceId, request, { signal } = {}) {
  return apiClient.put(`/services/${serviceId}`, { body: request, signal });
}

/**
 * DELETE /api/services/{id}. Soft deactivation: the backend sets isActive=false and deletes nothing.
 * Enquiries keep their service link. Resolves with null (HTTP 204).
 *
 * @param {number} serviceId
 * @param {{ signal?: AbortSignal }} [options]
 */
export function deactivateService(serviceId, { signal } = {}) {
  return apiClient.delete(`/services/${serviceId}`, { signal });
}

function optionalText(value) {
  const trimmed = value.trim();
  return trimmed === "" ? null : trimmed;
}

function optionalPrice(value) {
  const trimmed = value.trim();
  return trimmed === "" ? null : Number(trimmed);
}

/** Converts a ServiceResponse into the form's string values. */
export function serviceToFormValues(service) {
  return {
    name: service.name ?? "",
    description: service.description ?? "",
    category: service.category ?? "",
    price: service.price === null || service.price === undefined ? "" : String(service.price),
  };
}

/**
 * Maps service form values to the backend ServiceRequest.
 *
 * @param {{ name: string, description: string, category: string, price: string }} form
 * @param {boolean|null} isActive  null leaves the status unchanged (used by create and edit);
 *                                 true reactivates (used by reactivation)
 * @returns {{ name: string, description: string|null, category: string|null, price: number|null, isActive: boolean|null }}
 */
export function toServiceRequest(form, isActive = null) {
  return {
    name: form.name.trim(),
    description: optionalText(form.description),
    category: optionalText(form.category),
    price: optionalPrice(form.price),
    isActive,
  };
}
