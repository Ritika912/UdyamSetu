import { Navigate, Outlet, useLocation } from "react-router";
import LoadingState from "./LoadingState.jsx";
import { useAuth } from "../auth/AuthContext.js";
import { routeDecision } from "../auth/authState.js";

/**
 * Wraps the owner's pages. While the session is being checked it shows a loading state. When the owner is
 * signed out it redirects to /login and remembers where they were going.
 */
function ProtectedRoute() {
  const { status } = useAuth();
  const location = useLocation();
  const decision = routeDecision(status);

  if (decision === "loading") {
    return (
      <div className="auth-loading">
        <LoadingState message="Checking your session…" />
      </div>
    );
  }

  if (decision === "login") {
    const { pathname, search, hash } = location;
    return <Navigate to="/login" replace state={{ from: { pathname, search, hash } }} />;
  }

  return <Outlet />;
}

export default ProtectedRoute;
