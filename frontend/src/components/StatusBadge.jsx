import { getEnquiryStatusDisplay } from "../utils/enquiryStatus.js";

/**
 * Shows an enquiry status with an owner-friendly label.
 *
 * @param {object} props
 * @param {string} props.status backend EnquiryStatus value, e.g. "UNFULFILLED"
 */
function StatusBadge({ status }) {
  const { label, tone } = getEnquiryStatusDisplay(status);

  return <span className={`status-badge ${tone}`}>{label}</span>;
}

export default StatusBadge;
