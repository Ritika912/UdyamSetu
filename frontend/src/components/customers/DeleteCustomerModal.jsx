import { useRef, useState } from "react";
import Modal from "../Modal.jsx";
import { ApiError } from "../../api/client.js";
import { deleteCustomer } from "../../api/customersApi.js";

const ENQUIRY_BLOCK_MESSAGE =
  "This customer cannot be deleted because they have enquiries. Deleting them would remove that history, " +
  "so UdyamSetu keeps the record. You can edit their details instead.";

/**
 * Confirms and performs DELETE /api/customers/{id}.
 * A 409 means the customer has enquiry history: the dialog explains why and keeps the delete button disabled.
 *
 * @param {object} props
 * @param {object} props.customer  CustomerResponse to delete
 * @param {() => void} props.onClose
 * @param {() => void} props.onDeleted  called only after the backend confirms (HTTP 204)
 */
function DeleteCustomerModal({ customer, onClose, onDeleted }) {
  const [isDeleting, setIsDeleting] = useState(false);
  const [isBlocked, setIsBlocked] = useState(false);
  const [deleteError, setDeleteError] = useState("");
  const [backendDetail, setBackendDetail] = useState("");
  const isDeletingRef = useRef(false);

  function handleClose() {
    if (!isDeleting) onClose();
  }

  async function handleDelete() {
    if (isDeletingRef.current || isBlocked) return;

    isDeletingRef.current = true;
    setIsDeleting(true);
    setDeleteError("");
    setBackendDetail("");

    try {
      await deleteCustomer(customer.id);
      onDeleted();
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setIsBlocked(true);
        setDeleteError(ENQUIRY_BLOCK_MESSAGE);
        setBackendDetail(error.message);
      } else if (error instanceof ApiError) {
        setDeleteError(error.message);
      } else {
        setDeleteError("The customer could not be deleted. Please try again.");
      }
    } finally {
      isDeletingRef.current = false;
      setIsDeleting(false);
    }
  }

  return (
    <Modal
      isOpen
      onClose={handleClose}
      title="Delete customer?"
      description={`This removes ${customer.customerName} from your customer list. This cannot be undone.`}
    >
      {deleteError && (
        <div className="error-message" role="alert">
          <span>{deleteError}</span>
          {backendDetail && <small>Server message: {backendDetail}</small>}
        </div>
      )}

      <div className="modal-actions">
        <button type="button" className="cancel-button" onClick={handleClose} disabled={isDeleting}>
          {isBlocked ? "Close" : "Cancel"}
        </button>
        {!isBlocked && (
          <button type="button" className="danger-button" onClick={handleDelete} disabled={isDeleting}>
            {isDeleting ? "Deleting…" : "Delete customer"}
          </button>
        )}
      </div>
    </Modal>
  );
}

export default DeleteCustomerModal;
