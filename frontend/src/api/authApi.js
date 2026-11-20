import { apiClient } from "./client.js";

/**
 * GET /api/auth/session. Public.
 * Response: { authenticated, username, csrfToken, csrfHeaderName }. The token is for the current session.
 *
 * @param {{ signal?: AbortSignal }} [options]
 */
export function getSession({ signal } = {}) {
  return apiClient.get("/auth/session", { signal, authRequest: true });
}

/**
 * POST /api/auth/login. Public, but needs the current CSRF token (the client sends it).
 * Response: { authenticated: true, username, csrfToken, csrfHeaderName }, where csrfToken is new.
 * 401 for wrong credentials, 429 when locked out, 403 when the token or origin is rejected.
 *
 * @param {string} username
 * @param {string} password
 */
export function login(username, password) {
  return apiClient.post("/auth/login", { body: { username, password }, authRequest: true });
}

/**
 * POST /api/auth/logout. Needs a signed-in session and the CSRF token. Resolves with null (204).
 */
export function logout() {
  return apiClient.post("/auth/logout", { authRequest: true });
}
