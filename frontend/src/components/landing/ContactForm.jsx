import { useRef, useState } from "react";
import FormField from "../FormField.jsx";
import ErrorMessage from "../ErrorMessage.jsx";
import { EMPTY_CONTACT_FORM, validateContactForm } from "./contactFormValidation.js";
import { describeContactSubmitError } from "./contactSubmitErrors.js";

const SUCCESS_MESSAGE = "Thank you. Your message has been received.";

/**
 * Public contact form on the landing page. Validates the visible fields in the browser, then calls `onSubmit`.
 *
 * `onSubmit(values)` must return a promise. It resolves only when the message is accepted (HTTP 202). It rejects
 * with an ApiError, which is turned into visitor-safe wording by describeContactSubmitError.
 *
 * The `website` field is a honeypot. It is hidden from people and from assistive technology and skipped by the
 * keyboard. Real visitors leave it empty, and the backend uses a filled value to recognise automated submissions.
 *
 * @param {object} props
 * @param {(values: typeof EMPTY_CONTACT_FORM) => Promise<unknown>} props.onSubmit
 */
function ContactForm({ onSubmit }) {
  const [values, setValues] = useState(EMPTY_CONTACT_FORM);
  const [fieldErrors, setFieldErrors] = useState({});
  // notice is { tone: "success" | "error", message } or null. It is cleared when the visitor edits the form.
  const [notice, setNotice] = useState(null);
  const [isSending, setIsSending] = useState(false);
  // A ref blocks a second submit that starts before the re-render disables the button.
  const isSendingRef = useRef(false);

  function handleChange(event) {
    const { name, value } = event.target;
    setValues((previous) => ({ ...previous, [name]: value }));
    setFieldErrors((previous) => {
      if (!previous[name]) return previous;
      const next = { ...previous };
      delete next[name];
      return next;
    });
    setNotice(null);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (isSendingRef.current) return;

    const errors = validateContactForm(values);
    setFieldErrors(errors);
    setNotice(null);
    if (Object.keys(errors).length > 0) return;

    isSendingRef.current = true;
    setIsSending(true);
    try {
      await onSubmit(values);
      setValues(EMPTY_CONTACT_FORM);
      setNotice({ tone: "success", message: SUCCESS_MESSAGE });
    } catch (error) {
      const outcome = describeContactSubmitError(error);
      setNotice({ tone: "error", message: outcome.message });
      if (Object.keys(outcome.fieldErrors).length > 0) setFieldErrors(outcome.fieldErrors);
    } finally {
      isSendingRef.current = false;
      setIsSending(false);
    }
  }

  return (
    <section className="contact-form-card" aria-labelledby="contact-form-title">
      <h3 id="contact-form-title" className="contact-form-title">
        Send us a message
      </h3>

      <form className="contact-form" onSubmit={handleSubmit} noValidate aria-busy={isSending}>
        {notice?.tone === "error" && <ErrorMessage error={notice.message} />}
        {notice?.tone === "success" && (
          <p className="save-notice" role="status">
            {notice.message}
          </p>
        )}

        <fieldset className="contact-form-fields" disabled={isSending}>
          <FormField label="Full name" error={fieldErrors.fullName}>
            <input
              name="fullName"
              type="text"
              value={values.fullName}
              onChange={handleChange}
              autoComplete="name"
            />
          </FormField>

          <FormField label="Email address" error={fieldErrors.email}>
            <input
              name="email"
              type="email"
              value={values.email}
              onChange={handleChange}
              autoComplete="email"
              inputMode="email"
              autoCapitalize="none"
              spellCheck={false}
            />
          </FormField>

          <FormField label="Subject" error={fieldErrors.subject}>
            <input name="subject" type="text" value={values.subject} onChange={handleChange} />
          </FormField>

          <FormField label="Message" error={fieldErrors.message}>
            <textarea name="message" rows={5} value={values.message} onChange={handleChange} />
          </FormField>

          <div className="contact-honeypot" aria-hidden="true">
            <label htmlFor="contact-website">Website</label>
            <input
              id="contact-website"
              name="website"
              type="text"
              tabIndex={-1}
              autoComplete="off"
              value={values.website}
              onChange={handleChange}
            />
          </div>
        </fieldset>

        <button type="submit" className="primary-button contact-submit" disabled={isSending}>
          {isSending ? "Sending…" : "Send message"}
        </button>
      </form>
    </section>
  );
}

export default ContactForm;
