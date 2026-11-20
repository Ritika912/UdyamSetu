/**
 * Shown while data is loading. Announced to screen readers as a status message.
 *
 * @param {object} props
 * @param {string} [props.message="Loading…"]
 */
function LoadingState({ message = "Loading…" }) {
  return (
    <div className="loading-state" role="status">
      <span className="loading-spinner" aria-hidden="true" />
      <span>{message}</span>
    </div>
  );
}

export default LoadingState;
