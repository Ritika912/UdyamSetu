import { ApiError } from "../../api/client.js";

/**
 * Turns a failed contact submission into text for the visitor. Backend exception text and raw response bodies
 * are never shown: each status has its own fixed wording. Field errors are shown only for the form's own fields.
 */
const FORM_FIELDS = Object.freeze(["fullName", "email", "subject", "message"]);

export const CONTACT_MESSAGES = Object.freeze({
  network: "Cannot reach the server. Check your connection and try again.",
  rejected: "We could not accept this message. Check the form and try again.",
  notVerified: "Your message could not be verified. Reload the page and try again.",
  tooLarge: "This message is too long. Shorten it and try again.",
  rateLimited: "Too many messages were sent from this connection. Please wait a few minutes and try again.",
  unavailable:
    "Message delivery is unavailable right now. Please try again later or use the support email address above.",
  generic: "Your message could not be sent. Please try again later.",
});

/** Keeps only the field errors that belong to a field on this form. */
function pickFormFieldErrors(fieldErrors) {
  return Object.fromEntries(
    FORM_FIELDS.filter((field) => typeof fieldErrors[field] === "string").map((field) => [
      field,
      fieldErrors[field],
    ])
  );
}

/** Describes a wait in plain words, for example "about 5 minutes". Null when the server gave no wait. */
function describeWait(seconds) {
  if (seconds === null) return null;
  if (seconds <= 90) return "about a minute";
  return `about ${Math.ceil(seconds / 60)} minutes`;
}

/**
 * @param {unknown} error  whatever the submission rejected with
 * @returns {{ message: string, fieldErrors: Record<string, string> }}
 */
export function describeContactSubmitError(error) {
  if (!(error instanceof ApiError)) {
    return { message: CONTACT_MESSAGES.generic, fieldErrors: {} };
  }

  switch (error.status) {
    case 0:
      return { message: CONTACT_MESSAGES.network, fieldErrors: {} };

    case 400: {
      const fieldErrors = pickFormFieldErrors(error.fieldErrors);
      const hasFieldErrors = Object.keys(fieldErrors).length > 0;
      return {
        message: hasFieldErrors ? "Please check the highlighted fields and try again." : CONTACT_MESSAGES.rejected,
        fieldErrors,
      };
    }

    case 403:
      return { message: CONTACT_MESSAGES.notVerified, fieldErrors: {} };

    case 413:
      return { message: CONTACT_MESSAGES.tooLarge, fieldErrors: {} };

    case 429: {
      const wait = describeWait(error.retryAfterSeconds);
      return {
        message: wait
          ? `Too many messages were sent from this connection. Please try again in ${wait}.`
          : CONTACT_MESSAGES.rateLimited,
        fieldErrors: {},
      };
    }

    case 503:
      return { message: CONTACT_MESSAGES.unavailable, fieldErrors: {} };

    default:
      return { message: CONTACT_MESSAGES.generic, fieldErrors: {} };
  }
}
