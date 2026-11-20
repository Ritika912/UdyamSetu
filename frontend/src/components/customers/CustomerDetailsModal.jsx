import { useCallback } from "react";
import Modal from "../Modal.jsx";
import LoadingState from "../LoadingState.jsx";
import ErrorMessage from "../ErrorMessage.jsx";
import EmptyState from "../EmptyState.jsx";
import StatusBadge from "../StatusBadge.jsx";
import useApiData from "../../hooks/useApiData.js";
import { getCustomerEnquiries } from "../../api/customersApi.js";
import { getEnquirySourceLabel } from "../../api/enquiriesApi.js";
import { formatDateTime } from "../../utils/formatting.js";
import { Inbox } from "lucide-react";

const NOT_PROVIDED = "Not provided";

function DetailRow({ label, children }) {
  return (
    <div className="enquiry-detail-row">
      <dt>{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}

function EnquiryHistory({ history }) {
  if (history.isLoading) {
    return <LoadingState message="Loading enquiry history…" />;
  }

  if (history.error) {
    return <ErrorMessage error={history.error} onRetry={history.reload} />;
  }

  if (history.data.length === 0) {
    return (
      <EmptyState
        icon={Inbox}
        title="No enquiries yet"
        description="Enquiries from this customer will appear here."
      />
    );
  }

  return (
    <ul className="history-list">
      {history.data.map((enquiry) => (
        <li key={enquiry.id} className="history-item">
          <div>
            <strong>{formatDateTime(enquiry.createdAt)}</strong>
            <span>{enquiry.requestedService || "Service not specified"}</span>
            {getEnquirySourceLabel(enquiry.source) && (
              <span>Source: {getEnquirySourceLabel(enquiry.source)}</span>
            )}
            {enquiry.requirements && <p>{enquiry.requirements}</p>}
          </div>
          <StatusBadge status={enquiry.status} />
        </li>
      ))}
    </ul>
  );
}

/**
 * Customer details with enquiry history (GET /api/customers/{id}/enquiries). Offers Edit and Delete.
 * Delete is disabled when the history shows enquiries, because the backend would refuse it with 409.
 *
 * @param {object} props
 * @param {object} props.customer  CustomerResponse from the list
 * @param {() => void} props.onClose
 * @param {() => void} props.onEdit
 * @param {() => void} props.onDelete
 */
function CustomerDetailsModal({ customer, onClose, onEdit, onDelete }) {
  const loadHistory = useCallback(
    ({ signal }) => getCustomerEnquiries(customer.id, { signal }),
    [customer.id]
  );
  const history = useApiData(loadHistory);

  const enquiryCount = history.data ? history.data.length : null;
  const hasEnquiries = enquiryCount !== null && enquiryCount > 0;

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={customer.customerName}
      description={`Customer since ${formatDateTime(customer.createdAt)}`}
    >
      <dl className="enquiry-details">
        <DetailRow label="Phone number">{customer.contactNumber}</DetailRow>
        <DetailRow label="Notes">{customer.notes || NOT_PROVIDED}</DetailRow>
        <DetailRow label="Last updated">{formatDateTime(customer.updatedAt) || NOT_PROVIDED}</DetailRow>
      </dl>

      <section className="details-section" aria-labelledby="customer-history-heading">
        <h3 id="customer-history-heading">
          Enquiry history{enquiryCount !== null && ` (${enquiryCount})`}
        </h3>
        <EnquiryHistory history={history} />
      </section>

      {hasEnquiries && (
        <p className="form-hint">
          This customer has enquiries, so the record cannot be deleted. Their history is kept so your records
          stay complete.
        </p>
      )}

      <div className="modal-actions">
        <button type="button" className="danger-button" onClick={onDelete} disabled={hasEnquiries}>
          Delete customer
        </button>
        <button type="button" className="cancel-button" onClick={onClose}>
          Close
        </button>
        <button type="button" className="primary-button" onClick={onEdit}>
          Edit customer
        </button>
      </div>
    </Modal>
  );
}

export default CustomerDetailsModal;
