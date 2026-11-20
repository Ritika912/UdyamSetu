import { getAuthHooks, getCsrf } from "../auth/sessionBridge.js";

/**
 * Centralised fetch wrapper for the UdyamSetu backend.
 *
 * In development, Vite proxies `/api` to the Spring Boot server (see vite.config.js),
 * so requests here use a relative base path.
 */
export const API_BASE_URL = "/api";

/**
 * Error thrown for every failed request.
 *
 * `status` is the HTTP status (0 when the server could not be reached).
 * `fieldErrors` maps request field names to messages for 400 validation responses.
 * `retryAfterSeconds` is the server's Retry-After wait, when it sent a number of seconds (otherwise null).
 */
export class ApiError extends Error {
  constructor({ message, status, fieldErrors = {}, timestamp = null, retryAfterSeconds = null }) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
    this.timestamp = timestamp;
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

/** Reads a Retry-After header given as a whole number of seconds. HTTP-date values are ignored. */
function parseRetryAfter(value) {
  if (!value) return null;
  const seconds = Number(value);
  return Number.isInteger(seconds) && seconds >= 0 ? seconds : null;
}

const UNSAFE_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

function buildUrl(path, params) {
  const url = new URL(`${API_BASE_URL}${path}`, window.location.origin);

  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    });
  }

  return url.pathname + url.search;
}

function fallbackMessage(status) {
  if (status === 404) return "The requested item could not be found.";
  if (status === 409) return "This conflicts with existing data.";
  if (status >= 500) return "Something went wrong on the server. Please try again.";
  return "The request could not be completed.";
}

async function toApiError(response) {
  let body = null;

  try {
    body = await response.json();
  } catch {
    // Non-JSON error body (for example a proxy error page). Use the fallback message.
  }

  // Backend ErrorResponse: { timestamp, status, error, message, fieldErrors }
  const isErrorResponse = body && typeof body === "object" && typeof body.message === "string";

  return new ApiError({
    message: isErrorResponse && body.message ? body.message : fallbackMessage(response.status),
    status: response.status,
    fieldErrors: isErrorResponse && body.fieldErrors ? body.fieldErrors : {},
    timestamp: isErrorResponse ? body.timestamp : null,
    retryAfterSeconds: parseRetryAfter(response.headers.get("Retry-After")),
  });
}

/**
 * One HTTP attempt, with no retries.
 *
 * Unsafe methods carry the CSRF token from memory under the header name the server supplied. A 401 from a
 * business API signals the owner's session has ended, so the provider is told. Auth endpoints (`authRequest`)
 * report 401 to their caller instead, because a 401 there is a normal answer, such as wrong credentials.
 */
async function sendOnce(path, { method, params, body, signal, authRequest = false, expectedStatus }) {
  const headers = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";

  if (UNSAFE_METHODS.has(method)) {
    const csrf = getCsrf();
    if (!csrf) {
      // No token in memory. The caller refreshes it and retries once, the same as a stale-token 403.
      throw new ApiError({ message: "Security token missing. Please try again.", status: 403 });
    }
    headers[csrf.headerName] = csrf.token;
  }

  let response;

  try {
    response = await fetch(buildUrl(path, params), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
      credentials: "same-origin",
    });
  } catch (error) {
    if (error.name === "AbortError") throw error;

    throw new ApiError({
      message: "Cannot reach the server. Check your connection and try again.",
      status: 0,
    });
  }

  if (!response.ok) {
    const error = await toApiError(response);
    if (error.status === 401 && !authRequest) {
      getAuthHooks().onUnauthorized();
    }
    throw error;
  }

  // A success status other than the one the caller asked for is not a success for that caller.
  if (expectedStatus !== undefined && response.status !== expectedStatus) {
    throw new ApiError({ message: fallbackMessage(response.status), status: response.status });
  }

  if (response.status === 204) return null;

  // Some successful responses (for example 202 Accepted) have no body at all.
  const text = await response.text();
  return text === "" ? null : JSON.parse(text);
}

/**
 * Performs a JSON request against the backend.
 *
 * Unsafe methods (POST, PUT, PATCH, DELETE) are retried at most once, and only after a 403. A 403 is produced by
 * the security filters before any controller runs, so the request was not processed and a retry cannot repeat
 * a change. The retry first gets a fresh token from the server. Any other failure, including 5xx, a network
 * error, or an aborted request, is never retried.
 *
 * @param {string} path      path below /api, e.g. "/dashboard"
 * @param {object} [options]
 * @param {string} [options.method="GET"]
 * @param {object} [options.params]  query parameters; null, undefined and "" are omitted
 * @param {object} [options.body]    sent as JSON
 * @param {AbortSignal} [options.signal]
 * @param {boolean} [options.authRequest=false]  true for /api/auth/* calls, see sendOnce
 * @param {number} [options.expectedStatus]  when set, any other success status is rejected with ApiError
 * @returns {Promise<any>} parsed JSON body, or null for 204 No Content and for an empty body
 * @throws {ApiError} for HTTP errors and network failures (status 0).
 *                    An aborted request rejects with the native AbortError.
 */
export async function request(path, options = {}) {
  const method = options.method ?? "GET";
  const attempt = () => sendOnce(path, { ...options, method });

  if (!UNSAFE_METHODS.has(method)) {
    return attempt();
  }

  if (!getCsrf()) {
    await getAuthHooks().refreshCsrf();
  }

  try {
    return await attempt();
  } catch (error) {
    if (!(error instanceof ApiError) || error.status !== 403) throw error;

    const refreshed = await getAuthHooks().refreshCsrf();
    if (!refreshed) throw error;

    return attempt();
  }
}

export const apiClient = {
  get: (path, options) => request(path, { ...options, method: "GET" }),
  post: (path, options) => request(path, { ...options, method: "POST" }),
  patch: (path, options) => request(path, { ...options, method: "PATCH" }),
  put: (path, options) => request(path, { ...options, method: "PUT" }),
  delete: (path, options) => request(path, { ...options, method: "DELETE" }),
};
