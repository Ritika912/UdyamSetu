import { CalendarClock } from "lucide-react";
import Panel from "../Panel.jsx";
import EmptyState from "../EmptyState.jsx";
import { formatDate, formatMonth, formatPercent } from "../../utils/formatting.js";

const HEADLINES = Object.freeze({
  RISING: "Enquiries are going up",
  FALLING: "Enquiries are going down",
  STABLE: "Enquiries are about the same as before",
  INSUFFICIENT_DATA: "Not enough enquiries yet to see a trend",
});

function formatBucket(bucket, granularity) {
  return granularity === "MONTH" ? formatMonth(bucket) : formatDate(bucket);
}

/** Shows the label under every nth bar so a 30-day chart stays readable. */
function shouldShowLabel(index, total) {
  if (total <= 8) return true;
  return index === 0 || index === total - 1 || index % Math.ceil(total / 6) === 0;
}

/**
 * Enquiry volume over the period, from demandTrend. Bars are the response's own counts (zero-filled
 * by the backend). The headline and the explanation follow the backend's direction, and say so when
 * there are too few enquiries to judge.
 *
 * @param {object} props
 * @param {object} props.trend       TrendInsight
 * @param {object} props.thresholds  BusinessInsightsResponse.Thresholds
 */
function DemandTrend({ trend, thresholds }) {
  const unit = trend.granularity === "MONTH" ? "months" : "days";
  const points = trend.points ?? [];
  const maxCount = Math.max(1, ...points.map((point) => point.count));

  let explanation;
  if (trend.direction === "INSUFFICIENT_DATA") {
    explanation = `A trend needs at least ${thresholds.minTrendEnquiries} enquiries spread over at least ${thresholds.minTrendBuckets} ${unit}.`;
  } else {
    explanation =
      `First half of the period: ${trend.previousPeriodCount} enquiries. Second half: ${trend.recentPeriodCount}. ` +
      `A change of more than ${formatPercent(thresholds.trendChangeRatio)} counts as rising or falling.`;
  }

  return (
    <Panel title="Enquiries over time" description="How many enquiries came in each day or month.">
      <div className="trend-summary">
        <CalendarClock size={18} aria-hidden="true" />
        <div>
          <strong>{HEADLINES[trend.direction] ?? "Trend"}</strong>
          <p>{explanation}</p>
        </div>
      </div>

      {points.length === 0 ? (
        <EmptyState title="No enquiries to chart" description="Enquiries in this period will appear here." />
      ) : (
        <div className="trend-chart" role="img" aria-label={`Enquiries per ${unit === "days" ? "day" : "month"}`}>
          {points.map((point, index) => (
            <div className="trend-column" key={point.bucket} title={`${formatBucket(point.bucket, trend.granularity)}: ${point.count}`}>
              <span className="trend-count">{point.count > 0 ? point.count : ""}</span>
              <div className="trend-bar-area">
                <div
                  className="trend-bar"
                  style={{ height: `${(point.count / maxCount) * 100}%` }}
                />
              </div>
              <span className="trend-label">
                {shouldShowLabel(index, points.length) ? formatBucket(point.bucket, trend.granularity) : ""}
              </span>
            </div>
          ))}
        </div>
      )}
    </Panel>
  );
}

export default DemandTrend;
