import { useCallback, useState } from "react";
import { Inbox, Plus } from "lucide-react";
import Panel from "../Panel.jsx";
import LoadingState from "../LoadingState.jsx";
import ErrorMessage from "../ErrorMessage.jsx";
import EmptyState from "../EmptyState.jsx";
import StatusBadge from "../StatusBadge.jsx";
import EnquiryDetailsModal from "./EnquiryDetailsModal.jsx";
import useApiData from "../../hooks/useApiData.js";
import { getEnquiries, getEnquirySourceLabel } from "../../api/enquiriesApi.js";
import { formatDateTime, getInitials } from "../../utils/formatting.js";

/**
 * Enquiries table from GET /api/enquiries, filtered by status on the server.
 *
 * The parent gives this component a `key` that changes with the filter and after each save, so the
 * list always loads fresh for its filter. Status updates inside the details dialog call `reload`
 * instead, so the dialog stays open.
 *
 * @param {object} props
 * @param {string} props.statusFilter  EnquiryStatus value, or "" for all statuses
 * @param {() => void} props.onAddEnquiry
 */
function EnquiryList({ statusFilter, onAddEnquiry }) {
  // Memoised on the filter so useApiData reloads when the filter changes.
  const loadEnquiries = useCallback(
    ({ signal }) => getEnquiries({ status: statusFilter, signal }),
    [statusFilter]
  );
  const enquiries = useApiData(loadEnquiries);
  const [selectedEnquiry, setSelectedEnquiry] = useState(null);
  const isFiltered = statusFilter !== "";

  let content;

  if (enquiries.isLoading) {
    content = <LoadingState message="Loading enquiries…" />;
  } else if (enquiries.error) {
    content = <ErrorMessage error={enquiries.error} onRetry={enquiries.reload} />;
  } else if (enquiries.data.length === 0) {
    content = isFiltered ? (
      <EmptyState
        icon={Inbox}
        title="No enquiries with this status"
        description="Choose another status in the filter to see other enquiries."
      />
    ) : (
      <EmptyState
        icon={Inbox}
        title="No enquiries yet"
        description="Customer enquiries will appear here once they are recorded."
        action={
          <button className="primary-button" type="button" onClick={onAddEnquiry}>
            <Plus size={19} />
            Add Enquiry
          </button>
        }
      />
    );
  } else {
    content = (
      <div className="table-container">
        <table className="enquiries-table">
          <thead>
            <tr>
              <th>Customer</th>
              <th>Contact</th>
              <th>Requested service</th>
              <th>Source</th>
              <th>Created</th>
              <th>Status</th>
              <th>
                <span className="visually-hidden">Actions</span>
              </th>
            </tr>
          </thead>

          <tbody>
            {enquiries.data.map((enquiry) => (
              <tr key={enquiry.id}>
                <td>
                  <div className="customer-cell">
                    <div className="customer-avatar">{getInitials(enquiry.customerName)}</div>
                    <div>
                      <strong>{enquiry.customerName}</strong>
                    </div>
                  </div>
                </td>
                <td>{enquiry.contactNumber}</td>
                <td>{enquiry.requestedService || "Not specified"}</td>
                <td>{getEnquirySourceLabel(enquiry.source) ?? "Not specified"}</td>
                <td>{formatDateTime(enquiry.createdAt)}</td>
                <td>
                  <StatusBadge status={enquiry.status} />
                </td>
                <td>
                  <button
                    className="text-button"
                    type="button"
                    onClick={() => setSelectedEnquiry(enquiry)}
                    aria-label={`View enquiry from ${enquiry.customerName}`}
                  >
                    View
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <>
      <Panel className="enquiries-panel">{content}</Panel>

      {selectedEnquiry && (
        <EnquiryDetailsModal
          key={selectedEnquiry.id}
          enquiry={selectedEnquiry}
          onClose={() => setSelectedEnquiry(null)}
          onStatusUpdated={(updatedEnquiry) => {
            setSelectedEnquiry(updatedEnquiry);
            enquiries.reload();
          }}
        />
      )}
    </>
  );
}

export default EnquiryList;
