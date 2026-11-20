import { useRef, useState } from "react";
import Modal from "../Modal.jsx";
import ErrorMessage from "../ErrorMessage.jsx";
import { ApiError } from "../../api/client.js";
import {
  deactivateService,
  serviceToFormValues,
  toServiceRequest,
  updateService,
} from "../../api/servicesApi.js";
import { DUPLICATE_SERVICE_NAME_MESSAGE } from "./serviceMessages.js";

const COPY = Object.freeze({
  deactivate: {
    title: "Deactivate service?",
    confirmLabel: "Deactivate",
    busyLabel: "Deactivating…",
    body: (name) =>
      `${name} will no longer be offered when you record new enquiries. Past enquiries keep this service, and you can reactivate it later.`,
  },
  reactivate: {
    title: "Reactivate service?",
    confirmLabel: "Reactivate",
    busyLabel: "Reactivating…",
    body: (name) => `${name} will be available again when you record new enquiries.`,
  },
});

/**
 * Confirms a soft deactivation (DELETE /api/services/{id}) or a reactivation
 * (PUT /api/services/{id} with isActive: true). Nothing is deleted in either case.
 *
 * Reactivation sends the service's current name, description, category and price, because the PUT
 * replaces those fields. Without them, reactivating would clear the price.
 *
 * @param {object} props
 * @param {object} props.service  ServiceResponse
 * @param {"deactivate"|"reactivate"} props.mode
 * @param {() => void} props.onClose
 * @param {() => void} props.onDone  called only after the backend confirms
 */
function ServiceStatusModal({ service, mode, onClose, onDone }) {
  const copy = COPY[mode];
  const [isWorking, setIsWorking] = useState(false);
  const [isBlocked, setIsBlocked] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const isWorkingRef = useRef(false);

  function handleClose() {
    if (!isWorking) onClose();
  }

  async function handleConfirm() {
    if (isWorkingRef.current || isBlocked) return;

    isWorkingRef.current = true;
    setIsWorking(true);
    setErrorMessage("");

    try {
      if (mode === "deactivate") {
        await deactivateService(service.id);
      } else {
        await updateService(service.id, toServiceRequest(serviceToFormValues(service), true));
      }
      onDone();
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setIsBlocked(true);
        setErrorMessage(DUPLICATE_SERVICE_NAME_MESSAGE);
      } else if (error instanceof ApiError) {
        setErrorMessage(error.message);
      } else {
        setErrorMessage("The change could not be saved. Please try again.");
      }
    } finally {
      isWorkingRef.current = false;
      setIsWorking(false);
    }
  }

  return (
    <Modal isOpen onClose={handleClose} title={copy.title} description={copy.body(service.name)}>
      <ErrorMessage error={errorMessage} />

      <div className="modal-actions">
        <button type="button" className="cancel-button" onClick={handleClose} disabled={isWorking}>
          {isBlocked ? "Close" : "Cancel"}
        </button>
        {!isBlocked && (
          <button type="button" className="primary-button" onClick={handleConfirm} disabled={isWorking}>
            {isWorking ? copy.busyLabel : copy.confirmLabel}
          </button>
        )}
      </div>
    </Modal>
  );
}

export default ServiceStatusModal;
