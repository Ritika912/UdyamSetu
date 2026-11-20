import { beforeEach, mock, test } from "node:test";
import assert from "node:assert/strict";
import { apiClient, ApiError } from "./client.js";
import { clearCsrf, setAuthHooks, setCsrf } from "../auth/sessionBridge.js";

// These tests replace fetch and window with small fakes, so no backend or browser is needed.
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

function empty(status) {
  return new Response(null, { status });
}

function installHooks({ refreshResult = true, onRefresh } = {}) {
  const hooks = {
    onUnauthorized: mock.fn(),
    refreshCsrf: mock.fn(async () => {
      if (onRefresh) onRefresh();
      return refreshResult;
    }),
  };
  setAuthHooks(hooks);
  return hooks;
}

beforeEach(() => {
  clearCsrf();
});

test("mutations send the stored token under the header name the server supplied", async () => {
  setCsrf("X-Test-Csrf", "token-1");
  installHooks();
  installFetch([json(201, { id: 1 })]);

  await apiClient.post("/enquiries", { body: { customerName: "A" } });

  assert.equal(calls[0].init.method, "POST");
  assert.equal(calls[0].init.headers["X-Test-Csrf"], "token-1");
  assert.equal(calls[0].init.credentials, "same-origin");
});

test("PUT, PATCH and DELETE also send the token", async () => {
  setCsrf("X-Test-Csrf", "token-2");
  installHooks();
  installFetch([json(200, { id: 1 }), json(200, { id: 1 }), empty(204)]);

  await apiClient.put("/customers/1", { body: {} });
  await apiClient.patch("/enquiries/1/status", { body: { status: "NEW" } });
  await apiClient.delete("/customers/1");

  assert.deepEqual(
    calls.map((call) => call.init.method),
    ["PUT", "PATCH", "DELETE"]
  );
  calls.forEach((call) => assert.equal(call.init.headers["X-Test-Csrf"], "token-2"));
});

test("GET requests do not carry the CSRF token", async () => {
  setCsrf("X-Test-Csrf", "token-3");
  installHooks();
  installFetch([json(200, [])]);

  await apiClient.get("/customers");

  assert.equal(calls[0].init.headers["X-Test-Csrf"], undefined);
});

test("a mutation with no token in memory refreshes first and then sends the fresh token", async () => {
  const hooks = installHooks({ onRefresh: () => setCsrf("X-Test-Csrf", "fresh") });
  installFetch([json(201, { id: 2 })]);

  await apiClient.post("/customers", { body: {} });

  assert.equal(hooks.refreshCsrf.mock.callCount(), 1);
  assert.equal(calls[0].init.headers["X-Test-Csrf"], "fresh");
});

test("401 from a protected business API clears the signed-in state and throws ApiError", async () => {
  setCsrf("X-Test-Csrf", "token-4");
  const hooks = installHooks();
  installFetch([json(401, { message: "Authentication is required. Please sign in." })]);

  await assert.rejects(apiClient.get("/customers"), (error) => error instanceof ApiError && error.status === 401);

  assert.equal(hooks.onUnauthorized.mock.callCount(), 1);
});

test("401 from an auth endpoint is returned to the caller and does not sign the owner out", async () => {
  setCsrf("X-Test-Csrf", "token-5");
  const hooks = installHooks();
  installFetch([json(401, { message: "Invalid username or password." })]);

  await assert.rejects(
    apiClient.post("/auth/login", { body: { username: "x", password: "y" }, authRequest: true }),
    (error) => error.status === 401 && error.message === "Invalid username or password."
  );

  assert.equal(hooks.onUnauthorized.mock.callCount(), 0);
});

test("a 403 on a mutation refreshes the token and retries exactly once", async () => {
  setCsrf("X-Test-Csrf", "stale");
  const hooks = installHooks({ onRefresh: () => setCsrf("X-Test-Csrf", "fresh") });
  installFetch([json(403, { message: "Access denied." }), json(201, { id: 3 })]);

  const result = await apiClient.post("/enquiries", { body: {} });

  assert.deepEqual(result, { id: 3 });
  assert.equal(calls.length, 2);
  assert.equal(calls[0].init.headers["X-Test-Csrf"], "stale");
  assert.equal(calls[1].init.headers["X-Test-Csrf"], "fresh");
  assert.equal(hooks.refreshCsrf.mock.callCount(), 1);
});

test("a second 403 after the refresh is thrown without a third attempt", async () => {
  setCsrf("X-Test-Csrf", "stale");
  const hooks = installHooks({ onRefresh: () => setCsrf("X-Test-Csrf", "fresh") });
  installFetch([json(403, { message: "Access denied." }), json(403, { message: "Access denied." })]);

  await assert.rejects(apiClient.post("/enquiries", { body: {} }), (error) => error.status === 403);

  assert.equal(calls.length, 2);
  assert.equal(hooks.refreshCsrf.mock.callCount(), 1);
});

test("a 403 on a GET is not retried", async () => {
  setCsrf("X-Test-Csrf", "token-6");
  const hooks = installHooks();
  installFetch([json(403, { message: "Access denied." })]);

  await assert.rejects(apiClient.get("/customers"), (error) => error.status === 403);

  assert.equal(calls.length, 1);
  assert.equal(hooks.refreshCsrf.mock.callCount(), 0);
});

test("a 500 on a mutation is not retried, because it may already have been processed", async () => {
  setCsrf("X-Test-Csrf", "token-7");
  const hooks = installHooks();
  installFetch([json(500, { message: "An unexpected error occurred. Please try again later." })]);

  await assert.rejects(apiClient.post("/enquiries", { body: {} }), (error) => error.status === 500);

  assert.equal(calls.length, 1);
  assert.equal(hooks.refreshCsrf.mock.callCount(), 0);
});

test("a network failure on a mutation is not retried and becomes ApiError with status 0", async () => {
  setCsrf("X-Test-Csrf", "token-8");
  const hooks = installHooks();
  installFetch([new TypeError("fetch failed")]);

  await assert.rejects(apiClient.post("/enquiries", { body: {} }), (error) => error.status === 0);

  assert.equal(calls.length, 1);
  assert.equal(hooks.refreshCsrf.mock.callCount(), 0);
});

test("an aborted mutation is rethrown as AbortError and not retried", async () => {
  setCsrf("X-Test-Csrf", "token-9");
  const hooks = installHooks();
  installFetch([new DOMException("aborted", "AbortError")]);

  await assert.rejects(apiClient.post("/enquiries", { body: {} }), (error) => error.name === "AbortError");

  assert.equal(calls.length, 1);
  assert.equal(hooks.refreshCsrf.mock.callCount(), 0);
});

test("if the refresh fails, the original 403 is thrown and nothing is retried", async () => {
  setCsrf("X-Test-Csrf", "stale");
  const hooks = installHooks({ refreshResult: false });
  installFetch([json(403, { message: "Access denied." })]);

  await assert.rejects(apiClient.post("/enquiries", { body: {} }), (error) => error.status === 403);

  assert.equal(calls.length, 1);
  assert.equal(hooks.refreshCsrf.mock.callCount(), 1);
});

test("204 No Content resolves to null", async () => {
  setCsrf("X-Test-Csrf", "token-10");
  installHooks();
  installFetch([empty(204)]);

  assert.equal(await apiClient.delete("/services/1"), null);
});

test("backend field errors are preserved on ApiError", async () => {
  setCsrf("X-Test-Csrf", "token-11");
  installHooks();
  installFetch([
    json(400, { message: "Request validation failed", fieldErrors: { contactNumber: "Bad number" } }),
  ]);

  await assert.rejects(apiClient.post("/customers", { body: {} }), (error) => {
    assert.equal(error.status, 400);
    assert.equal(error.fieldErrors.contactNumber, "Bad number");
    return true;
  });
});

test("Retry-After in seconds is exposed on ApiError for a 429", async () => {
  installHooks();
  installFetch([json(429, { message: "Too many requests" }, { "Retry-After": "120" })]);

  await assert.rejects(apiClient.get("/auth/session"), (error) => {
    assert.equal(error.status, 429);
    assert.equal(error.retryAfterSeconds, 120);
    return true;
  });
});

test("a missing or date-form Retry-After leaves retryAfterSeconds null", async () => {
  installHooks();
  installFetch([
    json(429, { message: "Too many requests" }),
    json(429, { message: "Too many requests" }, { "Retry-After": "Wed, 21 Oct 2026 07:28:00 GMT" }),
  ]);

  await assert.rejects(apiClient.get("/x"), (error) => error.retryAfterSeconds === null);
  await assert.rejects(apiClient.get("/x"), (error) => error.retryAfterSeconds === null);
});

test("expectedStatus rejects a different success status", async () => {
  setCsrf("X-Test-Csrf", "token-12");
  installHooks();
  installFetch([json(200, { ok: true })]);

  await assert.rejects(
    apiClient.post("/contact", { body: {}, expectedStatus: 202 }),
    (error) => error instanceof ApiError && error.status === 200
  );
});

test("expectedStatus accepts the requested status with an empty body as null", async () => {
  setCsrf("X-Test-Csrf", "token-13");
  installHooks();
  installFetch([empty(202)]);

  assert.equal(await apiClient.post("/contact", { body: {}, expectedStatus: 202 }), null);
});

test("the token is never written to web storage during a request and retry", async () => {
  const storageCalls = [];
  const spy = (name) => (...args) => storageCalls.push([name, ...args]);
  const fakeStorage = { setItem: spy("setItem"), getItem: spy("getItem"), removeItem: spy("removeItem"), clear: spy("clear") };
  globalThis.localStorage = fakeStorage;
  globalThis.sessionStorage = fakeStorage;

  setCsrf("X-Test-Csrf", "stale");
  installHooks({ onRefresh: () => setCsrf("X-Test-Csrf", "fresh") });
  installFetch([json(403, { message: "Access denied." }), json(201, { id: 4 })]);

  await apiClient.post("/enquiries", { body: {} });

  assert.equal(storageCalls.length, 0);
});
