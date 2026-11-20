import { ArrowUpRight, Sparkles, Package, Megaphone, Hourglass, TrendingUp, Lightbulb } from "lucide-react";
import LoadingState from "../LoadingState.jsx";
import ErrorMessage from "../ErrorMessage.jsx";
import EmptyState from "../EmptyState.jsx";
import { useNavigate } from "react-router";

/** Icon and colour for each backend OpportunityCategory. Unknown categories fall back to a lightbulb. */
const CATEGORY_DISPLAY = Object.freeze({
  SUPPLY_GAP: { icon: Package, tone: "lavender" },
  DEMAND_PROMOTION: { icon: Megaphone, tone: "lavender" },
  CONVERSION_RISK: { icon: Hourglass, tone: "yellow" },
  GROWTH: { icon: TrendingUp, tone: "lavender" },
});

function OpportunityItem({ opportunity }) {
  const { icon: Icon, tone } = CATEGORY_DISPLAY[opportunity.category] ?? { icon: Lightbulb, tone: "lavender" };

  return (
    <div className="insight-item">
      <div className={`insight-icon ${tone}`}>
        <Icon size={18} />
      </div>
      <div>
        <strong>{opportunity.title}</strong>
        <p>{opportunity.explanation}</p>
        {opportunity.suggestedAction && <p>Suggested: {opportunity.suggestedAction}</p>}
        <p>
          {opportunity.serviceName} · {opportunity.supportingEnquiryCount} supporting{" "}
          {opportunity.supportingEnquiryCount === 1 ? "enquiry" : "enquiries"}
        </p>
      </div>
    </div>
  );
}

/**
 * Business Assistant panel. Shows the rule-based recommendations from
 * GET /api/insights/opportunities?period=LAST_30_DAYS. The frontend does not generate them.
 *
 * @param {object} props
 * @param {{ data: object|null, error: Error|null, isLoading: boolean, reload: () => void }} props.result
 */
function BusinessAssistantPanel({ result }) {
  const { data, error, isLoading, reload } = result;
  const navigate = useNavigate();

  let content;

  if (isLoading) {
    content = <LoadingState message="Finding opportunities…" />;
  } else if (error) {
    content = <ErrorMessage error={error} onRetry={reload} />;
  } else if (data.opportunities.length === 0) {
    content = (
      <EmptyState
        icon={Sparkles}
        title="No recommendations right now"
        description={data.notes.length > 0 ? data.notes.join(" ") : "Recommendations appear once there are enough enquiries."}
      />
    );
  } else {
    content = data.opportunities.map((opportunity) => (
      <OpportunityItem key={opportunity.serviceKey + opportunity.type} opportunity={opportunity} />
    ));
  }

  return (
    <div className="panel assistant-panel">
      <div className="assistant-heading">
        <div className="assistant-icon">
          <Sparkles size={21} />
        </div>
        <div>
          <h3>Business Assistant</h3>
          <p>A little help to grow your business</p>
        </div>
      </div>

      <div className="assistant-intro">
        <span className="assistant-tag">LAST 30 DAYS</span>
        <h4>Here are a few things worth your attention.</h4>
      </div>

      {content}

      <button className="assistant-button" type="button" onClick={() => navigate("/insights")}>
        View suggestions <ArrowUpRight size={16} />
      </button>
    </div>
  );
}

export default BusinessAssistantPanel;
