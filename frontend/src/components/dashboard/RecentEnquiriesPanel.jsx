import { ArrowUpRight, Inbox } from "lucide-react";
import Panel from "../Panel.jsx";
import LoadingState from "../LoadingState.jsx";
import ErrorMessage from "../ErrorMessage.jsx";
import EmptyState from "../EmptyState.jsx";
import StatusBadge from "../StatusBadge.jsx";
import { formatDateTime, getInitials } from "../../utils/formatting.js";
import { useNavigate } from "react-router";

/**
 * Table of the most recent enquiries (EnquiryResponse[] from GET /api/enquiries).
 *
 * @param {object} props
 * @param {{ data: object[]|null, error: Error|null, isLoading: boolean, reload: () => void }} props.result
 */
function RecentEnquiriesPanel({ result }) {
  const { data, error, isLoading, reload } = result;
  const navigate = useNavigate();

  let content;

  if (isLoading) {
    content = <LoadingState message="Loading enquiries…" />;
  } else if (error) {
    content = <ErrorMessage error={error} onRetry={reload} />;
  } else if (data.length === 0) {
    content = (
      <EmptyState
        icon={Inbox}
        title="No enquiries yet"
        description="Customer enquiries will appear here once they are recorded."
      />
    );
  } else {
    content = (
      <div className="table-container">
        <table className="enquiries-table">
          <thead>
            <tr>
              <th>Customer</th>
              <th>Requirement</th>
              <th>Status</th>
            </tr>
          </thead>

          <tbody>
            {data.map((enquiry) => (
              <tr key={enquiry.id}>
                <td>
                  <div className="customer-cell">
                    <div className="customer-avatar">{getInitials(enquiry.customerName)}</div>
                    <div>
                      <strong>{enquiry.customerName}</strong>
                      <span>{enquiry.contactNumber}</span>
                      <small>{formatDateTime(enquiry.createdAt)}</small>
                    </div>
                  </div>
                </td>

                <td>{enquiry.requestedService || enquiry.requirements || "Not specified"}</td>

                <td>
                  <StatusBadge status={enquiry.status} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <Panel
      className="enquiries-panel"
      title="Recent Enquiries"
      description="Keep track of your latest customer enquiries."
      action={
        <button className="text-button" type="button" onClick={() => navigate("/enquiries")}>
          View all <ArrowUpRight size={16} />
        </button>
      }
    >
      {content}
    </Panel>
  );
}

export default RecentEnquiriesPanel;
