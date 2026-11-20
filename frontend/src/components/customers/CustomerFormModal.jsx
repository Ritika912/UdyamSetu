import { useRef, useState } from "react";
import Modal from "../Modal.jsx";
import FormField from "../FormField.jsx";
import ErrorMessage from "../ErrorMessage.jsx";
import { ApiError } from "../../api/client.js";
import { createCustomer, toCustomerRequest, updateCustomer } from "../../api/customersApi.js";

const EMPTY_FORM = Object.freeze({ customerName: "", contactNumber: "", notes: "" });

function valuesFromCustomer(customer) {
  if (!customer) return EMPTY_FORM;
  return {
    customerName: customer.customerName ?? "",
    contactNumber: customer.contactNumber ?? "",
    notes: customer.notes ?? "",
  };
}

/** Required-field checks the backend would also apply, so the owner sees them without a round trip. */
function findLocalErrors(values) {
  const errors = {};
  if (values.customerName.trim() === "") errors.customerName = "Enter the customer's name.";
  if (values.contactNumber.trim() === "") errors.contactNumber = "Enter the customer's phone number.";
  return errors;
}

/**
 * Add or edit a customer. Pass `customer` to edit; omit it to add.
 * Values survive a failed save. onSaved receives the CustomerResponse, and only after the backend confirms.
 *
 * @param {object} props
 * @param {object} [props.customer]  CustomerResponse being edited
 * @param {() => void} props.onClose
 * @param {(saved: object) => void} props.onSaved
 */
function CustomerFormModal({ customer = null, onClose, onSaved }) {
  const isEditing = customer !== null;
  const [values, setValues] = useState(() => valuesFromCustomer(customer));
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

    // 409 on create or edit means another customer already has this contact number.
    if (error.status === 409) {
      setFieldErrors({ contactNumber: error.message });
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
      const request = toCustomerRequest(values);
      const saved = isEditing
        ? await updateCustomer(customer.id, request)
        : await createCustomer(request);
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
      title={isEditing ? "Edit Customer" : "Add Customer"}
      description={
        isEditing
          ? "Change the customer's details. Enquiries keep the name and number they were recorded with."
          : "Customers are identified by their phone number, so each number can belong to one customer only."
      }
    >
      <form onSubmit={handleSubmit} aria-busy={isSaving}>
        <ErrorMessage error={formError} />

        <fieldset className="enquiry-form-fields" disabled={isSaving}>
          <FormField label="Customer name" error={fieldErrors.customerName}>
            <input
              name="customerName"
              type="text"
              value={values.customerName}
              onChange={handleChange}
              placeholder="Enter customer name"
              maxLength={100}
              autoComplete="off"
              required
            />
          </FormField>

          <FormField label="Phone number" error={fieldErrors.contactNumber}>
            <input
              name="contactNumber"
              type="tel"
              value={values.contactNumber}
              onChange={handleChange}
              placeholder="e.g. 9876543210"
              maxLength={20}
              autoComplete="off"
              required
            />
          </FormField>

          <p className="form-hint">Use a 10-digit mobile number, or + and the country code followed by the number.</p>

          <FormField label="Notes" optional error={fieldErrors.notes}>
            <textarea
              name="notes"
              value={values.notes}
              onChange={handleChange}
              placeholder="Anything useful to remember about this customer"
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
            {isSaving ? "Saving…" : isEditing ? "Save changes" : "Add Customer"}
          </button>
        </div>
      </form>
    </Modal>
  );
}

export default CustomerFormModal;
