import { useRef, useState } from "react";
import Modal from "../Modal.jsx";
import ErrorMessage from "../ErrorMessage.jsx";
import EnquiryForm from "./EnquiryForm.jsx";
import { ApiError } from "../../api/client.js";
import {
  createEnquiry,
  OTHER_SERVICE_CHOICE,
  toEnquiryRequest,
} from "../../api/enquiriesApi.js";

const EMPTY_FORM = Object.freeze({
  customerName: "",
  contactNumber: "",
  source: "",
  serviceChoice: "",
  requestedService: "",
  requirements: "",
  occasion: "",
  budget: "",
  expectedDate: "",
  followUpDate: "",
  followUpTime: "",
  followUpNotes: "",
});

const FORM_FIELD_NAMES = Object.keys(EMPTY_FORM);

/** Backend DTO field names that differ from the form's field names. */
const BACKEND_TO_FORM_FIELD = Object.freeze({ serviceId: "serviceChoice" });

/**
 * Splits backend field errors into messages shown beside a field, and messages that have no
 * matching field (shown at the top of the form instead).
 */
function splitFieldErrors(fieldErrors) {
  const byField = {};
  const unmatched = [];

  Object.entries(fieldErrors).forEach(([backendField, message]) => {
    const formField = BACKEND_TO_FORM_FIELD[backendField] ?? backendField;
    if (FORM_FIELD_NAMES.includes(formField)) {
      byField[formField] = message;
    } else {
      unmatched.push(message);
    }
  });

  return { byField, unmatched };
}

/** Checks only what the backend cannot see as missing: a chosen "other" service needs its name. */
function findLocalErrors(values) {
  const errors = {};
  if (values.serviceChoice === OTHER_SERVICE_CHOICE && values.requestedService.trim() === "") {
    errors.requestedService = "Enter the service the customer asked for.";
  }
  return errors;
}

/**
 * Add Enquiry dialog. Owns the form values, so they survive a failed save. The parent mounts this
 * component only while the dialog is open, so closing it discards the values.
 *
 * onSaved receives the EnquiryResponse from the backend, and only after HTTP 201.
 *
 * @param {object} props
 * @param {() => void} props.onClose
 * @param {(enquiry: object) => void} props.onSaved
 */
function AddEnquiryModal({ onClose, onSaved }) {
  const [values, setValues] = useState(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  // A ref blocks a second submit that starts before the re-render disables the form.
  const isSubmittingRef = useRef(false);

  function handleChange(event) {
    const { name, value } = event.target;
    setValues((previous) => ({ ...previous, [name]: value }));
    setFieldErrors((previous) => {
      if (!previous[name]) return previous;
      const next = { ...previous };
      delete next[name];
      return next;
    });
  }

  function handleClose() {
    if (!isSaving) onClose();
  }

  function showSaveError(error) {
    if (!(error instanceof ApiError)) {
      setFormError("Something went wrong while saving. Your entries are still here. Please try again.");
      return;
    }

    const backendFieldErrors = error.fieldErrors ?? {};
    if (Object.keys(backendFieldErrors).length === 0) {
      // Not a field-level validation error (for example, server unreachable).
      setFormError(error.message);
      return;
    }

    const { byField, unmatched } = splitFieldErrors(backendFieldErrors);
    setFieldErrors(byField);
    setFormError(["Please check the highlighted fields.", ...unmatched].join(" "));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (isSubmittingRef.current) return;

    const localErrors = findLocalErrors(values);
    if (Object.keys(localErrors).length > 0) {
      setFieldErrors(localErrors);
      setFormError("Please check the highlighted fields.");
      return;
    }

    isSubmittingRef.current = true;
    setIsSaving(true);
    setFieldErrors({});
    setFormError("");

    try {
      const savedEnquiry = await createEnquiry(toEnquiryRequest(values));
      onSaved(savedEnquiry);
    } catch (error) {
      showSaveError(error);
    } finally {
      isSubmittingRef.current = false;
      setIsSaving(false);
    }
  }

  return (
    <Modal
      isOpen
      onClose={handleClose}
      title="Add New Enquiry"
      description="Enter the customer's enquiry details. Customer name and phone number are required."
    >
      <form onSubmit={handleSubmit} aria-busy={isSaving}>
        <ErrorMessage error={formError} />

        <EnquiryForm
          values={values}
          fieldErrors={fieldErrors}
          onChange={handleChange}
          isDisabled={isSaving}
        />

        <div className="modal-actions">
          <button type="button" className="cancel-button" onClick={handleClose} disabled={isSaving}>
            Cancel
          </button>

          <button type="submit" className="primary-button" disabled={isSaving}>
            {isSaving ? "Saving…" : "Save Enquiry"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

export default AddEnquiryModal;
