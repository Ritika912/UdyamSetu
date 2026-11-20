import { useCallback } from "react";
import LoadingState from "../LoadingState.jsx";
import ErrorMessage from "../ErrorMessage.jsx";
import BusinessOverview from "./BusinessOverview.jsx";
import DemandTrend from "./DemandTrend.jsx";
import ServiceDemandPanel from "./ServiceDemandPanel.jsx";
import OpportunityList from "./OpportunityList.jsx";
import { plainNote } from "./insightCopy.js";
import HowRecommendationsWork from "./HowRecommendationsWork.jsx";
import Panel from "../Panel.jsx";
import useApiData from "../../hooks/useApiData.js";
import { getBusinessInsights, getOpportunities } from "../../api/insightsApi.js";

/**
 * Shows one data block: loading, then error with retry, then the content for the loaded data.
 * Each block has its own state, so one failed request does not hide the other.
 */
function DataBlock({ result, loadingMessage, children }) {
  if (result.isLoading) return <LoadingState message={loadingMessage} />;
  if (result.error) return <ErrorMessage error={result.error} onRetry={result.reload} />;
  return children(result.data);
}

/**
 * The full insights view for one period. The parent gives it a `key` equal to the period, so both
 * datasets reload whenever the period changes.
 *
 * @param {object} props
 * @param {"LAST_30_DAYS"|"ALL_TIME"} props.period
 */
function InsightsView({ period }) {
  // Memoised on the period so useApiData reloads when the period changes.
  const loadBusiness = useCallback(
    ({ signal }) => getBusinessInsights({ period, signal }),
    [period]
  );
  const loadOpportunities = useCallback(
    ({ signal }) => getOpportunities({ period, signal }),
    [period]
  );

  const business = useApiData(loadBusiness);
  const opportunities = useApiData(loadOpportunities);

  return (
    <div className="insights-stack">
      <DataBlock result={opportunities} loadingMessage="Finding recommendations…">
        {(data) => (
          <>
            <OpportunityList data={data} />
            <HowRecommendationsWork thresholds={data.thresholds} />
          </>
        )}
      </DataBlock>

      <DataBlock result={business} loadingMessage="Loading your business figures…">
        {(data) => (
          <>
            <BusinessOverview data={data} />

            <DemandTrend trend={data.demandTrend} thresholds={data.thresholds} />

            <section className="insights-two-column">
              <ServiceDemandPanel
                variant="requested"
                title="Most requested services"
                description="The services customers asked for most in this period."
                items={data.mostRequestedServices}
                totalEnquiries={data.totalEnquiries}
                emptyText="No service requests in this period yet."
              />
              <ServiceDemandPanel
                variant="unavailable"
                title="Services you could not provide"
                description="Customers asked for these, but you could not provide them. Decide whether to offer them."
                items={data.repeatedUnfulfilledDemand}
                totalEnquiries={data.totalEnquiries}
                emptyText="No service has been unavailable more than once in this period."
              />
              <ServiceDemandPanel
                variant="notConverted"
                title="Enquiries where the customer did not proceed"
                description="Customers did not proceed for another reason. Review how these enquiries were followed up."
                items={data.highLostRateServices}
                totalEnquiries={data.totalEnquiries}
                emptyText="No service has many customers who did not proceed."
              />
            </section>

            {data.notes.length > 0 && (
              <Panel title="Good to know">
                <ul className="insight-notes">
                  {data.notes.map((note) => (
                    <li key={note}>{plainNote(note)}</li>
                  ))}
                </ul>
              </Panel>
            )}
          </>
        )}
      </DataBlock>
    </div>
  );
}

export default InsightsView;
