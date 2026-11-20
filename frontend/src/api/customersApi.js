import { apiClient } from "./client.js";

/**
 * GET /api/customers
 *
 * Response: CustomerResponse[] newest first. Each item: id, customerName, contactNumber, notes,
 * createdAt, updatedAt (LocalDateTime strings). The backend has no email field and no enquiry count
 * on this response.
 *
 * @param {{ signal?: AbortSignal }} [options]
 */
export function getCustomers({ signal } = {}) {
  return apiClient.get("/customers", { signal });
}

/**
 * GET /api/customers/{id}/enquiries
 *
 * Response: EnquiryResponse[] newest first. 404 if the customer does not exist.
 *
 * @param {number} customerId
 * @param {{ signal?: AbortSignal }} [options]
 */
export function getCustomerEnquiries(customerId, { signal } = {}) {
  return apiClient.get(`/customers/${customerId}/enquiries`, { signal });
}

/**
 * POST /api/customers. Body: CustomerRequest (see toCustomerRequest).
 * Resolves with the created CustomerResponse (HTTP 201). 409 if the contact number already exists.
 *
 * @param {object} request
 * @param {{ signal?: AbortSignal }} [options]
 */
export function createCustomer(request, { signal } = {}) {
  return apiClient.post("/customers", { body: request, signal });
}

/**
 * PUT /api/customers/{id}. Body: CustomerRequest, which replaces the editable fields.
 * Resolves with the updated CustomerResponse. 409 if another customer has the contact number.
 *
 * @param {number} customerId
 * @param {object} request
 * @param {{ signal?: AbortSignal }} [options]
 */
export function updateCustomer(customerId, request, { signal } = {}) {
  return apiClient.put(`/customers/${customerId}`, { body: request, signal });
}

/**
 * DELETE /api/customers/{id}. Resolves with null (HTTP 204).
 * 409 if the customer has any enquiry, because enquiry history must not be lost.
 *
 * @param {number} customerId
 * @param {{ signal?: AbortSignal }} [options]
 */
export function deleteCustomer(customerId, { signal } = {}) {
  return apiClient.delete(`/customers/${customerId}`, { signal });
}

function optionalText(value) {
  const trimmed = value.trim();
  return trimmed === "" ? null : trimmed;
}

/**
 * Maps the customer form values (strings) to the backend CustomerRequest: exactly customerName,
 * contactNumber and notes. Blank notes are sent as null.
 *
 * @param {{ customerName: string, contactNumber: string, notes: string }} form
 * @returns {{ customerName: string, contactNumber: string, notes: string|null }}
 */
export function toCustomerRequest(form) {
  return {
    customerName: form.customerName.trim(),
    contactNumber: form.contactNumber.trim(),
    notes: optionalText(form.notes),
  };
}
