import { useCallback, useEffect, useMemo, useReducer } from "react";
import { useNavigate } from "react-router";
import { AuthContext } from "./AuthContext.js";
import { authReducer, initialAuthState, interpretLogoutError } from "./authState.js";
import { getSession, login as loginRequest, logout as logoutRequest } from "../api/authApi.js";
import { clearCsrf, setAuthHooks, setCsrf } from "./sessionBridge.js";

/**
 * Owns the owner's sign-in state for the whole application.
 *
 * - On start-up and on every browser refresh, asks the server for the session (GET /api/auth/session).
 * - Keeps the CSRF token in memory only, under the header name the server returned.
 * - Gives the API client two callbacks: refresh the token, and handle a 401 from a protected API.
 */
function AuthProvider({ children }) {
  const [state, dispatch] = useReducer(authReducer, initialAuthState);
  const navigate = useNavigate();

  /** Fetches the server's view of the session and stores its token. Resolves true when a token is available. */
  const refreshCsrf = useCallback(async () => {
    try {
      const session = await getSession();
      setCsrf(session.csrfHeaderName, session.csrfToken);
      dispatch({ type: "SESSION_LOADED", session });
      return true;
    } catch (error) {
      clearCsrf();
      dispatch({ type: "SESSION_FAILED", message: error.message });
      return false;
    }
  }, []);

  /** A protected API said the session has ended. Forget the token and the signed-in state. */
  const handleUnauthorized = useCallback(() => {
    clearCsrf();
    dispatch({ type: "UNAUTHORIZED" });
  }, []);

  // Registered before the first session check, so the API client can use the callbacks from its first request.
  useEffect(() => {
    setAuthHooks({ onUnauthorized: handleUnauthorized, refreshCsrf });
  }, [handleUnauthorized, refreshCsrf]);

  // Start-up check, and again after every browser refresh (the page reloads and state is rebuilt from the server).
  useEffect(() => {
    refreshCsrf();
  }, [refreshCsrf]);

  const login = useCallback(async (username, password) => {
    const session = await loginRequest(username, password);
    // The backend rotates the token at sign-in, so the pre-login token is replaced rather than kept.
    setCsrf(session.csrfHeaderName, session.csrfToken);
    dispatch({ type: "SIGNED_IN", username: session.username });
  }, []);

  const logout = useCallback(async () => {
    try {
      await logoutRequest();
    } catch (error) {
      if (interpretLogoutError(error) !== "signed-out") {
        throw error;
      }
      // The session had already ended on the server. Continue with the local sign-out.
    }
    clearCsrf();
    // Navigate first, so the protected route does not redirect back with a "return to" target.
    navigate("/login", { replace: true });
    dispatch({ type: "SIGNED_OUT" });
  }, [navigate]);

  const value = useMemo(
    () => ({
      status: state.status,
      username: state.username,
      sessionError: state.sessionError,
      login,
      logout,
    }),
    [state, login, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export default AuthProvider;
