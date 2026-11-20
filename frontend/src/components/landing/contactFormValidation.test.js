import { test } from "node:test";
import assert from "node:assert/strict";
import { EMPTY_CONTACT_FORM, validateContactForm } from "./contactFormValidation.js";

const VALID_FORM = Object.freeze({
  fullName: "Asha Rao",
  email: "asha@business.co.in",
  subject: "Service question",
  message: "Do you offer weekend bookings?",
});

test("a complete form with a valid email has no errors", () => {
  assert.deepEqual(validateContactForm(VALID_FORM), {});
});

test("an empty form reports every required field", () => {
  assert.deepEqual(validateContactForm(EMPTY_CONTACT_FORM), {
    fullName: "Enter your full name.",
    email: "Enter your email address.",
    subject: "Enter a subject.",
    message: "Enter your message.",
  });
});

test("blank spaces count as empty", () => {
  const errors = validateContactForm({ ...VALID_FORM, fullName: "   ", subject: "\t", message: " " });
  assert.equal(errors.fullName, "Enter your full name.");
  assert.equal(errors.subject, "Enter a subject.");
  assert.equal(errors.message, "Enter your message.");
});

test("an email without an @ sign or a dot after the @ is rejected", () => {
  for (const email of ["asha", "asha@", "asha@business", "asha business@x.in", "@business.in"]) {
    assert.equal(
      validateContactForm({ ...VALID_FORM, email }).email,
      "Enter a valid email address.",
      `expected "${email}" to be rejected`
    );
  }
});

test("surrounding spaces in the email are ignored", () => {
  assert.deepEqual(validateContactForm({ ...VALID_FORM, email: "  asha@business.in  " }), {});
});
