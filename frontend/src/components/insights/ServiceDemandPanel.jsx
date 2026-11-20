import Panel from "../Panel.jsx";
import EmptyState from "../EmptyState.jsx";
import { formatPercent } from "../../utils/formatting.js";
import { getServiceSourceLabel } from "./insightCopy.js";

/** Plain-language headline and detail for one service, per panel variant. */
function describeItem(item, variant, totalEnquiries) {
  const share = totalEnquiries > 0 ? formatPercent(item.totalEnquiries / totalEnquiries) : "";

  if (variant === "requested") {
    return {
      headline: `${item.totalEnquiries} ${item.totalEnquiries === 1 ? "enquiry" : "enquiries"}`,
      detail: share ? `${share} of all enquiries in this period` : "",
    };
  }

  // The API withholds rates for small samples (rate is null), so show the rate only when it exists.
  const rate = item.sampleSufficient && item.totalEnquiries > 0
    ? (variant === "unavailable" ? item.unfulfilledRate : item.lostRate)
    : null;
  const rateText = rate === null || rate === undefined ? "" : ` (${formatPercent(rate)})`;

  if (variant === "unavailable") {
    return {
      headline: `Could not provide it ${item.unfulfilledCount} ${item.unfulfilledCount === 1 ? "time" : "times"}`,
      detail: `Out of ${item.totalEnquiries} enquiries for this service${rateText}`,
    };
  }

  return {
    headline: `${item.lostCount} ${item.lostCount === 1 ? "enquiry" : "enquiries"} where the customer did not proceed`,
    detail: `Out of ${item.totalEnquiries} enquiries for this service${rateText}`,
  };
}

/**
 * One list of services from GET /api/insights/business. The variant decides which counts are shown.
 * Services from the owner's list and services typed in on enquiries are labelled separately.
 *
 * @param {object} props
 * @param {"requested"|"unavailable"|"notConverted"} props.variant
 * @param {string} props.title
 * @param {string} props.description
 * @param {object[]} props.items           DemandInsight[]
 * @param {number} props.totalEnquiries
 * @param {string} props.emptyText
 */
function ServiceDemandPanel({ variant, title, description, items, totalEnquiries, emptyText }) {
  return (
    <Panel title={title} description={description} className="service-demand-panel">
      {items.length === 0 ? (
        <EmptyState title={emptyText} />
      ) : (
        <ul className="demand-list">
          {items.map((item) => {
            const { headline, detail } = describeItem(item, variant, totalEnquiries);
            return (
              <li key={item.serviceKey} className="demand-item">
                <div>
                  <strong>{item.service}</strong>
                  <span className={`source-tag ${item.serviceSource === "CATALOGUE" ? "catalogue" : "typed"}`}>
                    {getServiceSourceLabel(item.serviceSource)}
                  </span>
                </div>
                <div className="demand-figures">
                  <strong>{headline}</strong>
                  {detail && <span>{detail}</span>}
                  {!item.sampleSufficient && (
                    <small>Few enquiries so far. Treat this as an early sign, not a firm conclusion.</small>
                  )}
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </Panel>
  );
}

export default ServiceDemandPanel;
