import { test } from "node:test";
import assert from "node:assert/strict";
import { ApiError } from "../../api/client.js";
import { CONTACT_MESSAGES, describeContactSubmitError } from "./contactSubmitErrors.js";

function apiError(status, extra = {}) {
  return new ApiError({ message: "backend text", status, ...extra });
}

test("400 with field errors shows them for the form's own fields only", () => {
  const outcome = describeContactSubmitError(
    apiError(400, { fieldErrors: { email: "Enter a valid email address.", website: "must be empty", fullName: "Too long" } })
  );

  assert.equal(outcome.message, "Please check the highlighted fields and try again.");
  assert.deepEqual(outcome.fieldErrors, { email: "Enter a valid email address.", fullName: "Too long" });
});

test("400 without field errors shows the general rejection message", () => {
  const outcome = describeContactSubmitError(apiError(400, { fieldErrors: { website: "must be empty" } }));

  assert.equal(outcome.message, CONTACT_MESSAGES.rejected);
  assert.deepEqual(outcome.fieldErrors, {});
});

test("403 is described as a verification failure with a reload hint", () => {
  assert.deepEqual(describeContactSubmitError(apiError(403)), {
    message: CONTACT_MESSAGES.notVerified,
    fieldErrors: {},
  });
});

test("413 asks the visitor to shorten the message", () => {
  assert.equal(describeContactSubmitError(apiError(413)).message, CONTACT_MESSAGES.tooLarge);
});

test("429 uses the Retry-After wait in minutes", () => {
  assert.equal(
    describeContactSubmitError(apiError(429, { retryAfterSeconds: 120 })).message,
    "Too many messages were sent from this connection. Please try again in about 2 minutes."
  );
});

test("429 with a short Retry-After says about a minute", () => {
  assert.equal(
    describeContactSubmitError(apiError(429, { retryAfterSeconds: 30 })).message,
    "Too many messages were sent from this connection. Please try again in about a minute."
  );
});

test("429 without Retry-After uses the general wait message", () => {
  assert.equal(describeContactSubmitError(apiError(429)).message, CONTACT_MESSAGES.rateLimited);
});

test("503 asks the visitor to try later or use the support email above", () => {
  const { message } = describeContactSubmitError(apiError(503));
  assert.equal(message, CONTACT_MESSAGES.unavailable);
  assert.match(message, /try again later or use the support email address above/);
});

test("a network failure (status 0) says the server cannot be reached", () => {
  assert.equal(describeContactSubmitError(apiError(0)).message, CONTACT_MESSAGES.network);
});

test("other server errors use the generic message and never show backend text", () => {
  const outcome = describeContactSubmitError(
    new ApiError({ message: "NullPointerException at com.udyamsetu.ContactService line 42", status: 500 })
  );

  assert.equal(outcome.message, CONTACT_MESSAGES.generic);
  assert.doesNotMatch(outcome.message, /NullPointer|ContactService|udyamsetu/i);
});

test("backend text is never shown for any mapped status", () => {
  for (const status of [0, 400, 403, 413, 429, 503, 500]) {
    const { message } = describeContactSubmitError(apiError(status, { message: "stack trace: SQLException" }));
    assert.doesNotMatch(message, /stack trace|SQLException|backend text/i, `status ${status}`);
  }
});

test("errors that are not ApiError get the generic message", () => {
  assert.equal(describeContactSubmitError(new Error("boom")).message, CONTACT_MESSAGES.generic);
});
