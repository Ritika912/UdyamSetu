import { Users, FileText, CalendarClock, Hourglass } from "lucide-react";
import StatCard from "../StatCard.jsx";
import Panel from "../Panel.jsx";
import LoadingState from "../LoadingState.jsx";
import ErrorMessage from "../ErrorMessage.jsx";

/**
 * Summary cards from GET /api/dashboard. Shows a loading or error state in place of the cards.
 *
 * @param {object} props
 * @param {{ data: object|null, error: Error|null, isLoading: boolean, reload: () => void }} props.result
 */
function DashboardStats({ result }) {
  const { data, error, isLoading, reload } = result;

  if (isLoading) {
    return (
      <section className="stats-grid">
        <Panel className="grid-status">
          <LoadingState message="Loading your dashboard…" />
        </Panel>
      </section>
    );
  }

  if (error) {
    return (
      <section className="stats-grid">
        <Panel className="grid-status">
          <ErrorMessage error={error} onRetry={reload} />
        </Panel>
      </section>
    );
  }

  return (
    <section className="stats-grid">
      <StatCard
        title="Total Enquiries"
        value={data.totalEnquiries}
        description="All enquiries recorded"
        icon={Users}
        color="lavender"
      />
      <StatCard
        title="New Enquiries"
        value={data.newEnquiries}
        description="Not yet started"
        icon={FileText}
        color="peach"
      />
      <StatCard
        title="Follow-ups Due Today"
        value={data.followUpsDueToday}
        description="Customers to reconnect with today"
        icon={CalendarClock}
        color="yellow"
      />
      <StatCard
        title="Overdue Follow-ups"
        value={data.overdueFollowUps}
        description="Follow-up date has passed"
        icon={Hourglass}
        color="mint"
      />
    </section>
  );
}

export default DashboardStats;
