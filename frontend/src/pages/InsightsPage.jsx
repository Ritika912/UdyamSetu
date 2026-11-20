import { useState } from "react";
import PageHeader from "../components/PageHeader.jsx";
import InsightsView from "../components/insights/InsightsView.jsx";
import { PERIOD_OPTIONS } from "../components/insights/insightCopy.js";
import { INSIGHT_PERIODS } from "../api/insightsApi.js";

function InsightsPage() {
  const [period, setPeriod] = useState(INSIGHT_PERIODS.LAST_30_DAYS);

  return (
    <div className="dashboard-content">
      <PageHeader
        eyebrow="BUSINESS INSIGHTS & OPPORTUNITIES"
        title="Business Insights & Opportunities"
        description="See which services customers ask for most and what you could do next."
      />

      <div className="segmented-control" role="group" aria-label="Choose the period">
        {PERIOD_OPTIONS.map((option) => (
          <button
            key={option.value}
            type="button"
            aria-pressed={period === option.value}
            onClick={() => setPeriod(option.value)}
          >
            {option.label}
          </button>
        ))}
      </div>

      <InsightsView key={period} period={period} />
    </div>
  );
}

export default InsightsPage;
