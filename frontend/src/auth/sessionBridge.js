/**
 * In-memory bridge between the API client and the authentication provider.
 *
 * The CSRF token and its header name exist only in this module's memory. They are never written to a cookie,
 * localStorage or sessionStorage, and a page reload discards them. The application then asks the server again
 * through GET /api/auth/session. Passwords and session identifiers never reach this module at all: the session
 * identifier is an HttpOnly cookie that JavaScript cannot read.
 */

let csrf = null;

let hooks = {
  /** Called when a protected API returns 401. The provider clears the signed-in state. */
  onUnauthorized: () => {},
  /** Fetches a fresh token. Resolves true when a token is now available. */
  refreshCsrf: async () => false,
};

/** Stores the token and the header name the server asked for. Pass nulls to clear. */
export function setCsrf(headerName, token) {
  csrf = headerName && token ? { headerName, token } : null;
}

/** @returns {{ headerName: string, token: string } | null} */
export function getCsrf() {
  return csrf;
}

export function clearCsrf() {
  csrf = null;
}

/** Registers the provider's callbacks. Called once by the authentication provider. */
export function setAuthHooks(nextHooks) {
  hooks = { ...hooks, ...nextHooks };
}

export function getAuthHooks() {
  return hooks;
}
