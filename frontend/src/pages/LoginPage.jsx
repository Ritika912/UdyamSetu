import { useRef, useState } from "react";
import { Navigate, useLocation } from "react-router";
import { Eye, EyeOff } from "lucide-react";
import FormField from "../components/FormField.jsx";
import LoginShowcase from "../components/login/LoginShowcase.jsx";
import "../components/login/login.css";
import ErrorMessage from "../components/ErrorMessage.jsx";
import LoadingState from "../components/LoadingState.jsx";
import { useAuth } from "../auth/AuthContext.js";
import { AUTH_STATUS, safeReturnPath } from "../auth/authState.js";
import { ApiError } from "../api/client.js";

const EMPTY_FORM = Object.freeze({ username: "", password: "" });

const MESSAGES = Object.freeze({
  invalidCredentials: "Username or password is incorrect.",
  notVerified: "Your sign-in could not be verified. Reload the page and try again.",
  generic: "Sign-in failed. Please try again.",
});

function validate(values) {
  const errors = {};
  if (values.username.trim() === "") errors.username = "Enter your username.";
  if (values.password === "") errors.password = "Enter your password.";
  return errors;
}

/**
 * Turns a failed sign-in into a message for the owner. Wrong credentials clear the password field, because
 * the owner must type it again. Lockout (429) shows the server's own wording.
 */
function describeLoginError(error) {
  if (!(error instanceof ApiError)) return { message: MESSAGES.generic };

  if (error.status === 401) return { message: MESSAGES.invalidCredentials, clearPassword: true };
  if (error.status === 403) return { message: MESSAGES.notVerified };
  if (error.status === 429) return { message: error.message };

  if (error.status === 400 && Object.keys(error.fieldErrors).length > 0) {
    return { message: "Please check the highlighted fields.", fieldErrors: error.fieldErrors };
  }
  return { message: error.message || MESSAGES.generic };
}

/**
 * Owner sign-in. Shows a loading state while the session is checked. Once signed in, returns the owner to
 * the page they asked for before sign-in (passed in the location state by ProtectedRoute).
 */
function LoginPage() {
  const { status, sessionError, login } = useAuth();
  const location = useLocation();
  const [values, setValues] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  // A ref blocks a second submit that starts before the re-render disables the button.
  const isSubmittingRef = useRef(false);

  const from = location.state?.from;
  const returnPath = safeReturnPath(
    from ? `${from.pathname ?? "/"}${from.search ?? ""}${from.hash ?? ""}` : "/"
  );

  if (status === AUTH_STATUS.LOADING) {
    return (
      <main className="login-page">
        <LoadingState message="Checking your session…" />
      </main>
    );
  }

  if (status === AUTH_STATUS.AUTHENTICATED) {
    return <Navigate to={returnPath} replace />;
  }

  function handleChange(event) {
    const { name, value } = event.target;
    setValues((previous) => ({ ...previous, [name]: value }));
    setFieldErrors((previous) => {
      if (!previous[name]) return previous;
      const next = { ...previous };
      delete next[name];
      return next;
    });
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (isSubmittingRef.current) return;

    const errors = validate(values);
    setFieldErrors(errors);
    setFormError("");
    if (Object.keys(errors).length > 0) return;

    isSubmittingRef.current = true;
    setIsSubmitting(true);

    try {
      // On success the provider's status changes, and the render above sends the owner to returnPath.
      await login(values.username.trim(), values.password);
    } catch (error) {
      const outcome = describeLoginError(error);
      setFormError(outcome.message);
      if (outcome.fieldErrors) setFieldErrors(outcome.fieldErrors);
      if (outcome.clearPassword) setValues((previous) => ({ ...previous, password: "" }));
    } finally {
      isSubmittingRef.current = false;
      setIsSubmitting(false);
    }
  }

  const passwordError = fieldErrors.password;

  return (
    <main className="login-page">
      <LoginShowcase />

      <section className="login-panel" aria-labelledby="login-title">
        <div className="login-panel-inner">
          <p className="login-eyebrow">OWNER SIGN-IN</p>
          <h1 id="login-title">Welcome back</h1>
          <p className="login-intro">Sign in to manage your enquiries, customers and services.</p>

          {sessionError && (
          <ErrorMessage error={`We could not check your session. ${sessionError}`} />
        )}

        <form className="login-form" onSubmit={handleSubmit} noValidate aria-busy={isSubmitting}>
          <ErrorMessage error={formError} />

          <fieldset className="enquiry-form-fields" disabled={isSubmitting}>
            <FormField label="Username" error={fieldErrors.username}>
              <input
                name="username"
                type="text"
                value={values.username}
                onChange={handleChange}
                autoComplete="username"
                autoCapitalize="none"
                spellCheck={false}
              />
            </FormField>

            <div className="form-field">
              <label className="form-label" htmlFor="login-password">
                Password
              </label>
              <div className="input-with-action">
                <input
                  id="login-password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  value={values.password}
                  onChange={handleChange}
                  autoComplete="current-password"
                  className="form-control"
                  aria-invalid={passwordError ? true : undefined}
                  aria-describedby={passwordError ? "login-password-error" : undefined}
                />
                <button
                  type="button"
                  className="input-action"
                  onClick={() => setShowPassword((previous) => !previous)}
                  aria-label={showPassword ? "Hide password" : "Show password"}
                  aria-pressed={showPassword}
                >
                  {showPassword ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
                </button>
              </div>
              {passwordError && (
                <p className="form-error" id="login-password-error">
                  {passwordError}
                </p>
              )}
            </div>
          </fieldset>

          <button type="submit" className="primary-button login-submit" disabled={isSubmitting}>
            {isSubmitting ? "Signing in…" : "Sign in"}
          </button>
        </form>
        </div>
      </section>
    </main>
  );
}

export default LoginPage;
