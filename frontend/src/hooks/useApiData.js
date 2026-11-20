import { useEffect, useState } from "react";

/**
 * Loads one resource through the API layer and tracks its loading, error and retry state.
 * Each dashboard section uses its own instance, so one failed request does not hide the others.
 *
 * `fetchData` must be a stable function (defined outside the component) because it is an effect dependency.
 *
 * @param {({ signal: AbortSignal }) => Promise<any>} fetchData
 * @returns {{ data: any, error: Error|null, isLoading: boolean, reload: () => void }}
 */
export default function useApiData(fetchData) {
  const [attempt, setAttempt] = useState(0);
  const [outcome, setOutcome] = useState(null);

  useEffect(() => {
    const controller = new AbortController();

    fetchData({ signal: controller.signal })
      .then((data) => {
        if (!controller.signal.aborted) setOutcome({ attempt, data, error: null });
      })
      .catch((error) => {
        if (!controller.signal.aborted) setOutcome({ attempt, data: null, error });
      });

    return () => controller.abort();
  }, [fetchData, attempt]);

  // Results from an earlier attempt are ignored, so a retry always shows the loading state.
  const isCurrent = outcome !== null && outcome.attempt === attempt;

  return {
    data: isCurrent ? outcome.data : null,
    error: isCurrent ? outcome.error : null,
    isLoading: !isCurrent,
    reload: () => setAttempt((previous) => previous + 1),
  };
}
