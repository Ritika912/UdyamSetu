import { useRef, useState } from "react";
import Modal from "../Modal.jsx";
import ErrorMessage from "../ErrorMessage.jsx";
import FormField from "../FormField.jsx";
import StatusBadge from "../StatusBadge.jsx";
import { getEnquirySourceLabel, updateEnquiryStatus } from "../../api/enquiriesApi.js";
import { getEnquiryStatusDisplay, ENQUIRY_STATUS_VALUES } from "../../utils/enquiryStatus.js";
import {
  formatDate,
  formatDateTime,
  formatRupees,
  formatTime,
} from "../../utils/formatting.js";

const NOT_PROVIDED = "Not provided";

function DetailRow({ label, children }) {
  return (
    <div className="enquiry-detail-row">
      <dt>{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}

/** Explains the requested-service value: catalogue service, free text, or nothing. */
function describeRequestedService(enquiry) {
  if (!enquiry.requestedService) return "Not specified";
  if (enquiry.serviceId !== null && enquiry.serviceId !== undefined) {
    return `${enquiry.requestedService} (from your service list)`;
  }
  return `${enquiry.requestedService} (not in your service list)`;
}

/**
 * Shows every field of one enquiry (EnquiryResponse) and lets the owner change its status through
 * PATCH /api/enquiries/{id}/status. The status is saved only when the owner presses "Update status".
 *
 * @param {object} props
 * @param {object} props.enquiry           EnquiryResponse for the enquiry being viewed
 * @param {() => void} props.onClose
 * @param {(updated: object) => void} props.onStatusUpdated  receives the EnquiryResponse from the PATCH
 */
function EnquiryDetailsModal({ enquiry, onClose, onStatusUpdated }) {
  const [chosenStatus, setChosenStatus] = useState(enquiry.status);
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const [savedMessage, setSavedMessage] = useState("");
  // A ref blocks a second submit that starts before the re-render disables the button.
  const isSavingRef = useRef(false);

  const hasStatusChange = chosenStatus !== enquiry.status;

  function handleClose() {
    if (!isSaving) onClose();
  }

  async function handleStatusSubmit(event) {
    event.preventDefault();
    if (isSavingRef.current || !hasStatusChange) return;

    isSavingRef.current = true;
    setIsSaving(true);
    setSaveError(null);
    setSavedMessage("");

    try {
      const updatedEnquiry = await updateEnquiryStatus(enquiry.id, chosenStatus);
      onStatusUpdated(updatedEnquiry);
      setSavedMessage(`Status updated to ${getEnquiryStatusDisplay(updatedEnquiry.status).label}.`);
    } catch (error) {
      setSaveError(error);
    } finally {
      isSavingRef.current = false;
      setIsSaving(false);
    }
  }

  const sourceLabel = getEnquirySourceLabel(enquiry.source);

  return (
    <Modal
      isOpen
      onClose={handleClose}
      title={enquiry.customerName}
      description={`Enquiry received ${formatDateTime(enquiry.createdAt)}`}
    >
      <dl className="enquiry-details">
        <DetailRow label="Contact number">{enquiry.contactNumber}</DetailRow>
        <DetailRow label="Requested service">{describeRequestedService(enquiry)}</DetailRow>
        <DetailRow label="Requirements">{enquiry.requirements || NOT_PROVIDED}</DetailRow>
        <DetailRow label="Source">{sourceLabel || NOT_PROVIDED}</DetailRow>
        <DetailRow label="Occasion">{enquiry.occasion || NOT_PROVIDED}</DetailRow>
        <DetailRow label="Budget">{formatRupees(enquiry.budget) || NOT_PROVIDED}</DetailRow>
        <DetailRow label="Expected date">{formatDate(enquiry.expectedDate) || NOT_PROVIDED}</DetailRow>
        <DetailRow label="Follow-up">
          {enquiry.followUpDate
            ? `${formatDate(enquiry.followUpDate)}${enquiry.followUpTime ? `, ${formatTime(enquiry.followUpTime)}` : ""}`
            : NOT_PROVIDED}
        </DetailRow>
        <DetailRow label="Follow-up notes">{enquiry.followUpNotes || NOT_PROVIDED}</DetailRow>
        <DetailRow label="Current status">
          <StatusBadge status={enquiry.status} />
        </DetailRow>
      </dl>

      <form className="enquiry-status-form" onSubmit={handleStatusSubmit}>
        <FormField label="Change status">
          <select
            name="status"
            value={chosenStatus}
            onChange={(event) => {
              setChosenStatus(event.target.value);
              setSavedMessage("");
            }}
            disabled={isSaving}
          >
            {ENQUIRY_STATUS_VALUES.map((status) => (
              <option key={status} value={status}>
                {getEnquiryStatusDisplay(status).label}
              </option>
            ))}
          </select>
        </FormField>

        <p className="form-hint">
          Service unavailable means you could not provide the service. Customer did not proceed means the
          customer did not go ahead for another reason.
        </p>

        <ErrorMessage error={saveError} />
        {savedMessage && (
          <p className="save-notice" role="status">
            {savedMessage}
          </p>
        )}

        <div className="modal-actions">
          <button type="button" className="cancel-button" onClick={handleClose} disabled={isSaving}>
            Close
          </button>
          <button type="submit" className="primary-button" disabled={isSaving || !hasStatusChange}>
            {isSaving ? "Saving…" : "Update status"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

export default EnquiryDetailsModal;
