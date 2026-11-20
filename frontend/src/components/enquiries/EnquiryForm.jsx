import FormField from "../FormField.jsx";
import ErrorMessage from "../ErrorMessage.jsx";
import useApiData from "../../hooks/useApiData.js";
import { getActiveServices } from "../../api/servicesApi.js";
import { ENQUIRY_SOURCE_OPTIONS, OTHER_SERVICE_CHOICE } from "../../api/enquiriesApi.js";

// Defined outside the component so useApiData receives a stable reference.
function loadActiveServices({ signal }) {
  return getActiveServices({ signal });
}

/**
 * Enquiry fields for the Add Enquiry modal. Values and errors are owned by AddEnquiryModal.
 * The whole field set is disabled while a save is in progress.
 *
 * @param {object} props
 * @param {object} props.values         form values keyed by field name (strings)
 * @param {object} props.fieldErrors    backend or local message per field name
 * @param {(event: Event) => void} props.onChange
 * @param {boolean} props.isDisabled
 */
function EnquiryForm({ values, fieldErrors, onChange, isDisabled }) {
  const services = useApiData(loadActiveServices);
  const activeServices = services.data ?? [];

  return (
    <fieldset className="enquiry-form-fields" disabled={isDisabled}>
      <FormField label="Customer name" error={fieldErrors.customerName}>
        <input
          name="customerName"
          type="text"
          value={values.customerName}
          onChange={onChange}
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
          onChange={onChange}
          placeholder="e.g. 9876543210"
          maxLength={20}
          autoComplete="off"
          required
        />
      </FormField>

      <FormField label="Where did the enquiry come from?" optional error={fieldErrors.source}>
        <select name="source" value={values.source} onChange={onChange}>
          <option value="">Not specified</option>
          {ENQUIRY_SOURCE_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </FormField>

      <FormField label="Service requested" optional error={fieldErrors.serviceChoice}>
        <select
          name="serviceChoice"
          value={values.serviceChoice}
          onChange={onChange}
          disabled={services.isLoading}
        >
          <option value="">Not specified</option>
          {activeServices.map((service) => (
            <option key={service.id} value={String(service.id)}>
              {service.name}
            </option>
          ))}
          <option value={OTHER_SERVICE_CHOICE}>Other (not in my service list)</option>
        </select>
      </FormField>

      {services.isLoading && <p className="form-hint">Loading your services…</p>}
      {services.error && (
        <ErrorMessage
          error={`Your service list could not be loaded. ${services.error.message}`}
          onRetry={services.reload}
        />
      )}

      {values.serviceChoice === OTHER_SERVICE_CHOICE && (
        <FormField label="Service the customer asked for" error={fieldErrors.requestedService}>
          <input
            name="requestedService"
            type="text"
            value={values.requestedService}
            onChange={onChange}
            placeholder="e.g. Bridal makeup"
            maxLength={150}
          />
        </FormField>
      )}

      <FormField label="Requirements" optional error={fieldErrors.requirements}>
        <textarea
          name="requirements"
          value={values.requirements}
          onChange={onChange}
          placeholder="Describe what the customer needs"
          rows={3}
          maxLength={2000}
        />
      </FormField>

      <FormField label="Occasion" optional error={fieldErrors.occasion}>
        <input
          name="occasion"
          type="text"
          value={values.occasion}
          onChange={onChange}
          placeholder="e.g. Wedding, birthday"
          maxLength={100}
        />
      </FormField>

      <FormField label="Budget (₹)" optional error={fieldErrors.budget}>
        <input
          name="budget"
          type="number"
          value={values.budget}
          onChange={onChange}
          placeholder="e.g. 5000"
          min="0"
          step="0.01"
          inputMode="decimal"
        />
      </FormField>

      <FormField label="Expected date" optional error={fieldErrors.expectedDate}>
        <input name="expectedDate" type="date" value={values.expectedDate} onChange={onChange} />
      </FormField>

      <FormField label="Follow-up date" optional error={fieldErrors.followUpDate}>
        <input name="followUpDate" type="date" value={values.followUpDate} onChange={onChange} />
      </FormField>

      <FormField label="Follow-up time" optional error={fieldErrors.followUpTime}>
        <input name="followUpTime" type="time" value={values.followUpTime} onChange={onChange} />
      </FormField>

      <FormField label="Follow-up notes" optional error={fieldErrors.followUpNotes}>
        <textarea
          name="followUpNotes"
          value={values.followUpNotes}
          onChange={onChange}
          placeholder="What to remember when you follow up"
          rows={2}
          maxLength={2000}
        />
      </FormField>
    </fieldset>
  );
}

export default EnquiryForm;
