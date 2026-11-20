export const EMPTY_CONTACT_FORM = Object.freeze({
  fullName: "",
  email: "",
  subject: "",
  message: "",
  // Honeypot: hidden from visitors and never validated. It must stay empty for a real visitor.
  website: "",
});

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Checks the contact form fields on the client. Returns one message per invalid field;
 * an empty object means the form can be sent.
 *
 * @param {typeof EMPTY_CONTACT_FORM} values
 * @returns {Partial<Record<keyof typeof EMPTY_CONTACT_FORM, string>>}
 */
export function validateContactForm(values) {
  const errors = {};
  const email = values.email.trim();

  if (values.fullName.trim() === "") errors.fullName = "Enter your full name.";

  if (email === "") errors.email = "Enter your email address.";
  else if (!EMAIL_PATTERN.test(email)) errors.email = "Enter a valid email address.";

  if (values.subject.trim() === "") errors.subject = "Enter a subject.";
  if (values.message.trim() === "") errors.message = "Enter your message.";

  return errors;
}
