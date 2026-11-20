import { Lightbulb } from "lucide-react";
import Panel from "../Panel.jsx";
import EmptyState from "../EmptyState.jsx";
import { formatPercent } from "../../utils/formatting.js";
import {
  getServiceSourceLabel,
  OPPORTUNITY_HEADINGS,
  plainNote,
  plainSuggestedAction,
  sumStatusCounts,
} from "./insightCopy.js";

/**
 * The evidence sentence for one recommendation. Built from the response counts, so every number
 * can be checked against the figures shown elsewhere on the page.
 */
function describeEvidence(item, totalEnquiries) {
  switch (item.type) {
    case "REPEATED_UNFULFILLED":
      return `You could not provide this service in ${item.supportingEnquiryCount} enquiries this period.`;
    case "FREQUENTLY_REQUESTED":
      return `${item.supportingEnquiryCount} of ${totalEnquiries} enquiries this period asked for it` +
        (totalEnquiries > 0 ? ` (${formatPercent(item.supportingEnquiryCount / totalEnquiries)}).` : ".");
    case "HIGH_LOST_RATE":
      return `${item.supportingEnquiryCount} of ${sumStatusCounts(item.statusCounts)} enquiries for this service ` +
        "ended with the customer not proceeding, for another reason.";
    case "INCREASING_DEMAND":
      return `${item.recentCount} enquiries in the second half of the period, compared with ${item.previousCount} in the first half.`;
    default:
      return "";
  }
}

function OpportunityCard({ item, totalEnquiries }) {
  const heading = OPPORTUNITY_HEADINGS[item.type] ?? item.title;
  const sourceLabel = getServiceSourceLabel(item.serviceSource);

  return (
    <li className="opportunity-card">
      <div className="opportunity-icon" aria-hidden="true">
        <Lightbulb size={18} />
      </div>

      <div className="opportunity-body">
        <strong>{heading}</strong>
        <span className="opportunity-service">
          {item.serviceName}
          {sourceLabel && ` · ${sourceLabel}`}
          {item.serviceActive === false && " · Currently deactivated"}
        </span>

        <p className="opportunity-evidence">{describeEvidence(item, totalEnquiries)}</p>

        <p className="opportunity-action">
          <strong>What you could do: </strong>
          {plainSuggestedAction(item.suggestedAction)}
        </p>
      </div>
    </li>
  );
}

/**
 * Recommendations from GET /api/insights/opportunities. The API decides what is recommended and
 * shows the counts behind each one; this component only explains them.
 *
 * @param {object} props
 * @param {object} props.data  OpportunityInsightsResponse
 */
function OpportunityList({ data }) {
  const { opportunities, notes, totalEnquiries, thresholds } = data;

  return (
    <Panel
      title="Recommendations"
      description="Things worth acting on, based only on the enquiries in this period."
    >
      {opportunities.length === 0 ? (
        <EmptyState
          icon={Lightbulb}
          title="No recommendations for this period"
          description={`Recommendations appear when a service has at least ${thresholds.minSampleSize} enquiries or a clear pattern of unmet demand.`}
        />
      ) : (
        <ul className="opportunity-list">
          {opportunities.map((item) => (
            <OpportunityCard key={`${item.serviceKey}-${item.type}`} item={item} totalEnquiries={totalEnquiries} />
          ))}
        </ul>
      )}

      {notes.length > 0 && (
        <ul className="insight-notes">
          {notes.map((note) => (
            <li key={note}>{plainNote(note)}</li>
          ))}
        </ul>
      )}
    </Panel>
  );
}

export default OpportunityList;
