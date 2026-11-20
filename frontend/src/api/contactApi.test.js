import { beforeEach, mock, test } from "node:test";
import assert from "node:assert/strict";
import { ApiError } from "./client.js";
import { submitContactMessage, toContactPayload } from "./contactApi.js";
import { clearCsrf, getCsrf, setAuthHooks } from "../auth/sessionBridge.js";

// These tests replace fetch with a small fake, so no backend or browser is needed.
globalThis.window = { location: { origin: "http://localhost:5173" } };

let calls = [];

function installFetch(queue) {
  calls = [];
  const responses = [...queue];
  globalThis.fetch = async (url, init) => {
    calls.push({ url: String(url), init });
    const next = responses.shift();
    if (next instanceof Error) throw next;
    return next;
  };
}

function json(status, body, headers = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", ...headers },
  });
}

function session(overrides = {}) {
  return json(200, {
    authenticated: false,
    username: null,
    csrfToken: "contact-token-1",
    csrfHeaderName: "X-XSRF-TOKEN",
    ...overrides,
  });
}

const VISITOR_FORM = Object.freeze({
  fullName: "  Asha Rao  ",
  email: " asha@business.in ",
  subject: "Enquiry",
  message: "  Do you offer weekend bookings?  ",
  website: "",
});

beforeEach(() => {
  clearCsrf();
  setAuthHooks({ onUnauthorized: mock.fn(), refreshCsrf: mock.fn(async () => false) });
});

test("the CSRF token is fetched from the session endpoint before the contact POST", async () => {
  installFetch([session(), new Response(null, { status: 202 })]);

  await submitContactMessage(VISITOR_FORM);

  assert.equal(calls.length, 2);
  assert.equal(calls[0].url, "/api/auth/session");
  assert.equal(calls[0].init.method, "GET");
  assert.equal(calls[1].url, "/api/contact");
  assert.equal(calls[1].init.method, "POST");
});

test("the POST sends the token under the header name the server supplied, with same-origin credentials", async () => {
  installFetch([session({ csrfHeaderName: "X-Server-Chosen-Header", csrfToken: "fresh-token" }), new Response(null, { status: 202 })]);

  await submitContactMessage(VISITOR_FORM);

  assert.equal(calls[1].init.headers["X-Server-Chosen-Header"], "fresh-token");
  assert.equal(calls[1].init.credentials, "same-origin");
  assert.equal(calls[1].init.headers["Content-Type"], "application/json");
});

test("the payload uses the backend field names, trims text, and sends the honeypot as an empty string", async () => {
  installFetch([session(), new Response(null, { status: 202 })]);

  await submitContactMessage(VISITOR_FORM);

  assert.deepEqual(JSON.parse(calls[1].init.body), {
    fullName: "Asha Rao",
    email: "asha@business.in",
    subject: "Enquiry",
    message: "Do you offer weekend bookings?",
    website: "",
  });
});

test("a filled honeypot is passed through unchanged, so the backend can recognise the automated submission", () => {
  assert.equal(toContactPayload({ ...VISITOR_FORM, website: "http://spam.test" }).website, "http://spam.test");
});

test("a 202 Accepted with no body resolves, which is the only success", async () => {
  installFetch([session(), new Response(null, { status: 202 })]);

  assert.equal(await submitContactMessage(VISITOR_FORM), null);
});

test("any other success status is rejected, so the form cannot report a fake success", async () => {
  installFetch([session(), json(200, { ok: true })]);

  await assert.rejects(submitContactMessage(VISITOR_FORM), (error) => error instanceof ApiError && error.status === 200);
});

test("when the session request fails, no contact POST is sent", async () => {
  installFetch([new TypeError("fetch failed")]);

  await assert.rejects(submitContactMessage(VISITOR_FORM), (error) => error instanceof ApiError && error.status === 0);

  assert.equal(calls.length, 1);
});

test("a session without a usable token sends no contact POST", async () => {
  installFetch([session({ csrfToken: "", csrfHeaderName: "" })]);

  await assert.rejects(submitContactMessage(VISITOR_FORM), (error) => error.status === 403);

  assert.equal(calls.length, 1);
  assert.equal(getCsrf(), null);
});

test("a 403 on the POST refreshes the token once and retries, the same as other mutations", async () => {
  const refreshCsrf = mock.fn(async () => true);
  setAuthHooks({ onUnauthorized: mock.fn(), refreshCsrf });
  installFetch([session({ csrfToken: "stale" }), json(403, { message: "Access denied." }), new Response(null, { status: 202 })]);

  await submitContactMessage(VISITOR_FORM);

  assert.equal(refreshCsrf.mock.callCount(), 1);
  assert.equal(calls.length, 3);
  assert.equal(calls[1].url, "/api/contact");
  assert.equal(calls[2].url, "/api/contact");
});

test("the token is kept in memory after a successful submission", async () => {
  installFetch([session({ csrfToken: "kept-in-memory" }), new Response(null, { status: 202 })]);

  await submitContactMessage(VISITOR_FORM);

  assert.deepEqual(getCsrf(), { headerName: "X-XSRF-TOKEN", token: "kept-in-memory" });
});
