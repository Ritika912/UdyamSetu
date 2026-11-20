import { apiClient } from "./client.js";
import { getSession } from "./authApi.js";
import { setCsrf } from "../auth/sessionBridge.js";

/**
 * Sends a visitor's message from the public landing page. Public: no sign-in is needed.
 *
 * Before each submission the CSRF token is fetched from GET /api/auth/session and kept in memory only, so the
 * POST always carries a token that matches the current session. The token is sent under the header name the
 * server returned. `website` is the hidden honeypot field; real visitors never fill it, so it is always sent empty.
 *
 * Resolves only when the server answers 202 Accepted. Any other success status is rejected.
 *
 * @param {{ fullName: string, email: string, subject: string, message: string, website: string }} form
 * @returns {Promise<null>}
 * @throws {ApiError} see contactSubmitErrors.js for how each status is shown to the visitor
 */
export async function submitContactMessage(form) {
  const session = await getSession();
  setCsrf(session.csrfHeaderName, session.csrfToken);

  return apiClient.post("/contact", {
    body: toContactPayload(form),
    expectedStatus: 202,
  });
}

/** Builds the request body exactly as the backend contract names it. Text fields are trimmed. */
export function toContactPayload(form) {
  return {
    fullName: form.fullName.trim(),
    email: form.email.trim(),
    subject: form.subject.trim(),
    message: form.message.trim(),
    website: form.website,
  };
}
