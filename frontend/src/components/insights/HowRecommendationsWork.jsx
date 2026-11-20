import { formatPercent } from "../../utils/formatting.js";

/**
 * Plain-language explanation of the thresholds the backend applies. Values come from the response,
 * so the text stays correct if the backend thresholds change.
 *
 * @param {object} props
 * @param {object} props.thresholds  OpportunityInsightsResponse.Thresholds
 */
function HowRecommendationsWork({ thresholds }) {
  return (
    <details className="how-it-works">
      <summary>How these recommendations are decided</summary>
      <ul>
        <li>
          A service needs at least {thresholds.minSampleSize} enquiries before it is used for a recommendation or a
          percentage.
        </li>
        <li>
          &ldquo;Could not provide it&rdquo; is raised when this happens at least {thresholds.minRepeatedUnfulfilled}{" "}
          times for a service.
        </li>
        <li>
          &ldquo;Many customers did not proceed&rdquo; is raised when at least {formatPercent(thresholds.highLostRate)}{" "}
          of a service&apos;s enquiries end that way.
        </li>
        <li>
          A service is flagged as requested often when it has at least {thresholds.minFrequentRequests} enquiries and
          makes up at least {formatPercent(thresholds.minFrequentShare)} of all enquiries.
        </li>
        <li>
          Growth is only called when the second half of the period has at least {thresholds.minRecentForGrowth}{" "}
          enquiries and more than {formatPercent(thresholds.trendChangeRatio)} above the first half.
        </li>
      </ul>
    </details>
  );
}

export default HowRecommendationsWork;
