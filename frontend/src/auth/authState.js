/**
 * Pure authentication state logic. Kept free of React and browser APIs so it can be tested directly.
 */

export const AUTH_STATUS = Object.freeze({
  LOADING: "loading",
  ANONYMOUS: "anonymous",
  AUTHENTICATED: "authenticated",
});

export const initialAuthState = Object.freeze({
  status: AUTH_STATUS.LOADING,
  username: null,
  sessionError: null,
});

/**
 * @param {object} state
 * @param {{ type: string, session?: object, username?: string, message?: string }} action
 */
export function authReducer(state, action) {
  switch (action.type) {
    case "SESSION_LOADED":
      // The server's answer is authoritative, including when it says the owner is signed out.
      return action.session?.authenticated
        ? { status: AUTH_STATUS.AUTHENTICATED, username: action.session.username, sessionError: null }
        : { status: AUTH_STATUS.ANONYMOUS, username: null, sessionError: null };
    case "SESSION_FAILED":
      return { status: AUTH_STATUS.ANONYMOUS, username: null, sessionError: action.message ?? null };
    case "SIGNED_IN":
      return { status: AUTH_STATUS.AUTHENTICATED, username: action.username ?? null, sessionError: null };
    case "SIGNED_OUT":
    case "UNAUTHORIZED":
      return { status: AUTH_STATUS.ANONYMOUS, username: null, sessionError: null };
    default:
      return state;
  }
}

/**
 * What a protected route should do for the current status.
 * @returns {"loading" | "login" | "content"}
 */
export function routeDecision(status) {
  if (status === AUTH_STATUS.LOADING) return "loading";
  if (status === AUTH_STATUS.AUTHENTICATED) return "content";
  return "login";
}

/** Where an owner goes after signing in when they did not ask for a particular page. */
export const POST_LOGIN_PATH = "/dashboard";

/**
 * Returns a path that is safe to send the owner to after sign-in. Anything that is not a plain in-app path
 * falls back to the dashboard. So do the public landing page ("/") and the login page itself, because a
 * signed-in owner should not land back on either. This blocks open redirects such as "//example.com".
 */
export function safeReturnPath(candidate) {
  if (typeof candidate !== "string") return POST_LOGIN_PATH;
  if (!candidate.startsWith("/") || candidate.startsWith("//")) return POST_LOGIN_PATH;
  if (candidate === "/" || candidate === "/login" || candidate.startsWith("/login?") || candidate.startsWith("/login#")) {
    return POST_LOGIN_PATH;
  }
  return candidate;
}

/**
 * A logout that fails with 401 means the session had already ended, so the owner is signed out locally.
 * Any other failure leaves the owner signed in, because the server session may still be active.
 * @returns {"signed-out" | "failed"}
 */
export function interpretLogoutError(error) {
  return error?.status === 401 ? "signed-out" : "failed";
}
