import { useRef, useState } from "react";
import Modal from "../Modal.jsx";
import FormField from "../FormField.jsx";
import ErrorMessage from "../ErrorMessage.jsx";
import { ApiError } from "../../api/client.js";
import {
  createService,
  serviceToFormValues,
  toServiceRequest,
  updateService,
} from "../../api/servicesApi.js";
import { DUPLICATE_SERVICE_NAME_MESSAGE } from "./serviceMessages.js";

const EMPTY_FORM = Object.freeze({ name: "", description: "", category: "", price: "" });

/** Checks the rules the owner can get wrong. The backend checks them again. */
function findLocalErrors(values) {
  const errors = {};
  if (values.name.trim() === "") {
    errors.name = "Enter the service name.";
  }
  if (values.price.trim() !== "") {
    const price = Number(values.price);
    if (!Number.isFinite(price) || price < 0) {
      errors.price = "Enter a price of 0 or more, or leave it empty.";
    }
  }
  return errors;
}

/**
 * Add a service, or edit one when `service` is given. Editing never changes whether a service is
 * active; that is done with the Deactivate and Reactivate actions.
 *
 * @param {object} props
 * @param {object} [props.service]  ServiceResponse being edited
 * @param {() => void} props.onClose
 * @param {(saved: object) => void} props.onSaved  called only after the backend confirms
 */
function ServiceFormModal({ service = null, onClose, onSaved }) {
  const isEditing = service !== null;
  const [values, setValues] = useState(() => (service ? serviceToFormValues(service) : EMPTY_FORM));
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState("");
  const [isSaving, setIsSaving] = useState(false);
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

    if (error.status === 409) {
      setFieldErrors({ name: DUPLICATE_SERVICE_NAME_MESSAGE });
      return;
    }

    const backendFieldErrors = error.fieldErrors ?? {};
    if (Object.keys(backendFieldErrors).length > 0) {
      setFieldErrors(backendFieldErrors);
      setFormError("Please check the highlighted fields.");
      return;
    }

    setFormError(error.message);
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
      const request = toServiceRequest(values, null);
      const saved = isEditing
        ? await updateService(service.id, request)
        : await createService(request);
      onSaved(saved);
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
      title={isEditing ? "Edit Service" : "Add Service"}
      description={
        isEditing
          ? "Change the details customers see. To hide this service from new enquiries, use Deactivate on the list."
          : "Add a service your business offers. Enquiries can then be linked to it."
      }
    >
      <form onSubmit={handleSubmit} aria-busy={isSaving}>
        <ErrorMessage error={formError} />

        <fieldset className="enquiry-form-fields" disabled={isSaving}>
          <FormField label="Service name" error={fieldErrors.name}>
            <input
              name="name"
              type="text"
              value={values.name}
              onChange={handleChange}
              placeholder="e.g. Bridal makeup"
              maxLength={150}
              required
            />
          </FormField>

          <FormField label="Category" optional error={fieldErrors.category}>
            <input
              name="category"
              type="text"
              value={values.category}
              onChange={handleChange}
              placeholder="e.g. Beauty"
              maxLength={100}
            />
          </FormField>

          <FormField label="Price (₹)" optional error={fieldErrors.price}>
            <input
              name="price"
              type="number"
              value={values.price}
              onChange={handleChange}
              placeholder="e.g. 2500"
              step="any"
              inputMode="decimal"
            />
          </FormField>

          <FormField label="Description" optional error={fieldErrors.description}>
            <textarea
              name="description"
              value={values.description}
              onChange={handleChange}
              placeholder="What the service includes"
              rows={3}
              maxLength={2000}
            />
          </FormField>
        </fieldset>

        <div className="modal-actions">
          <button type="button" className="cancel-button" onClick={handleClose} disabled={isSaving}>
            Cancel
          </button>
          <button type="submit" className="primary-button" disabled={isSaving}>
            {isSaving ? "Saving…" : isEditing ? "Save changes" : "Add Service"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

export default ServiceFormModal;
