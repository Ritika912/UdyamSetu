/**
 * Shows an error from a request or a form. Accepts an ApiError (or any object with
 * `message` and optional `fieldErrors`), or a plain string. Renders nothing when `error` is empty.
 *
 * @param {object} props
 * @param {Error|{ message: string, fieldErrors?: Record<string, string> }|string|null} [props.error]
 * @param {() => void} [props.onRetry]         adds a "Try again" button when given
 * @param {boolean} [props.showFieldErrors=false]  lists each backend field error under the message.
 *                                             Leave off inside forms that already show errors per field.
 */
function ErrorMessage({ error, onRetry, showFieldErrors = false }) {
  if (!error) return null;

  const message = typeof error === "string" ? error : error.message;
  const fieldErrors = typeof error === "object" && error.fieldErrors ? error.fieldErrors : {};
  const fieldEntries = Object.entries(fieldErrors);

  if (!message) return null;

  return (
    <div className="error-message" role="alert">
      <span>{message}</span>

      {showFieldErrors && fieldEntries.length > 0 && (
        <ul>
          {fieldEntries.map(([field, fieldMessage]) => (
            <li key={field}>{fieldMessage}</li>
          ))}
        </ul>
      )}

      {onRetry && (
        <button type="button" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

export default ErrorMessage;
