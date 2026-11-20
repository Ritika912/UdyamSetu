import { ClipboardList, FileText, Package, TrendingUp } from "lucide-react";
import StatCard from "../StatCard.jsx";
import Panel from "../Panel.jsx";
import EmptyState from "../EmptyState.jsx";
import { ENQUIRY_STATUS_VALUES, getEnquiryStatusDisplay } from "../../utils/enquiryStatus.js";
import { formatDate, formatPercent } from "../../utils/formatting.js";
import { INSIGHT_STATUS_LABELS, sumStatusCounts } from "./insightCopy.js";

/**
 * Headline figures and the status breakdown from GET /api/insights/business.
 * Every number here is a count taken directly from the response.
 *
 * @param {object} props
 * @param {object} props.data  BusinessInsightsResponse
 */
function BusinessOverview({ data }) {
  const { statusCounts, totalEnquiries, withoutRequestedService, from, to } = data;
  const statusTotal = sumStatusCounts(statusCounts);
  const periodLabel = `${formatDate(from)} to ${formatDate(to)}`;

  return (
    <>
      <p className="insights-period-dates">Showing enquiries from {periodLabel}.</p>

      <section className="stats-grid">
        <StatCard
          title="Enquiries"
          value={totalEnquiries}
          description="Recorded in this period"
          icon={ClipboardList}
          color="lavender"
        />
        <StatCard
          title="Converted"
          value={statusCounts.CONVERTED ?? 0}
          description="Enquiries that became business"
          icon={TrendingUp}
          color="mint"
        />
        <StatCard
          title="Service unavailable"
          value={statusCounts.UNFULFILLED ?? 0}
          description="You could not provide the service"
          icon={Package}
          color="peach"
        />
        <StatCard
          title="Not linked to a service"
          value={withoutRequestedService}
          description="No service was recorded"
          icon={FileText}
          color="yellow"
        />
      </section>

      <Panel title="Where enquiries stand" description="How the enquiries in this period are doing.">
        {statusTotal === 0 ? (
          <EmptyState title="No enquiries in this period" description="Recorded enquiries will show here." />
        ) : (
          <ul className="bar-list">
            {ENQUIRY_STATUS_VALUES.map((status) => {
              const count = statusCounts[status] ?? 0;
              const share = statusTotal > 0 ? count / statusTotal : 0;
              const display = getEnquiryStatusDisplay(status);

              return (
                <li key={status} className="bar-row">
                  <div className="bar-label">
                    <span>{INSIGHT_STATUS_LABELS[status] ?? display.label}</span>
                    <span>
                      {count} {count === 1 ? "enquiry" : "enquiries"}
                      {count > 0 && ` · ${formatPercent(share)}`}
                    </span>
                  </div>
                  <div className="bar-track" aria-hidden="true">
                    <div className={`bar-fill ${display.tone}`} style={{ width: `${share * 100}%` }} />
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </Panel>
    </>
  );
}

export default BusinessOverview;
