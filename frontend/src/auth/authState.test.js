import { test } from "node:test";
import assert from "node:assert/strict";
import {
  AUTH_STATUS,
  POST_LOGIN_PATH,
  authReducer,
  initialAuthState,
  interpretLogoutError,
  routeDecision,
  safeReturnPath,
} from "./authState.js";

test("the initial state is loading, so protected pages wait for the session check", () => {
  assert.equal(initialAuthState.status, AUTH_STATUS.LOADING);
  assert.equal(routeDecision(initialAuthState.status), "loading");
});

test("a signed-in session from the server makes the owner authenticated", () => {
  const next = authReducer(initialAuthState, {
    type: "SESSION_LOADED",
    session: { authenticated: true, username: "owner" },
  });
  assert.deepEqual(next, { status: AUTH_STATUS.AUTHENTICATED, username: "owner", sessionError: null });
});

test("an anonymous session from the server makes the owner anonymous, even if they were signed in before", () => {
  const signedIn = { status: AUTH_STATUS.AUTHENTICATED, username: "owner", sessionError: null };
  const next = authReducer(signedIn, { type: "SESSION_LOADED", session: { authenticated: false, username: null } });
  assert.equal(next.status, AUTH_STATUS.ANONYMOUS);
  assert.equal(next.username, null);
});

test("a failed session check leaves the owner anonymous and keeps the reason", () => {
  const next = authReducer(initialAuthState, { type: "SESSION_FAILED", message: "Cannot reach the server." });
  assert.equal(next.status, AUTH_STATUS.ANONYMOUS);
  assert.equal(next.sessionError, "Cannot reach the server.");
});

test("a 401 from a protected API clears the signed-in state", () => {
  const signedIn = { status: AUTH_STATUS.AUTHENTICATED, username: "owner", sessionError: null };
  const next = authReducer(signedIn, { type: "UNAUTHORIZED" });
  assert.deepEqual(next, { status: AUTH_STATUS.ANONYMOUS, username: null, sessionError: null });
});

test("signing in and signing out move between the two signed states", () => {
  const signedIn = authReducer(initialAuthState, { type: "SIGNED_IN", username: "owner" });
  assert.equal(signedIn.status, AUTH_STATUS.AUTHENTICATED);
  assert.equal(signedIn.username, "owner");
  const signedOut = authReducer(signedIn, { type: "SIGNED_OUT" });
  assert.equal(signedOut.status, AUTH_STATUS.ANONYMOUS);
  assert.equal(signedOut.username, null);
});

test("unknown actions leave the state unchanged", () => {
  assert.equal(authReducer(initialAuthState, { type: "SOMETHING_ELSE" }), initialAuthState);
});

test("route decision: loading waits, authenticated shows the page, anything else goes to login", () => {
  assert.equal(routeDecision(AUTH_STATUS.LOADING), "loading");
  assert.equal(routeDecision(AUTH_STATUS.AUTHENTICATED), "content");
  assert.equal(routeDecision(AUTH_STATUS.ANONYMOUS), "login");
});

test("safe return path keeps in-app paths, including query and hash", () => {
  assert.equal(safeReturnPath("/customers"), "/customers");
  assert.equal(safeReturnPath("/enquiries?status=NEW#top"), "/enquiries?status=NEW#top");
});

test("safe return path rejects open redirects and falls back to the dashboard", () => {
  assert.equal(safeReturnPath("//evil.example.com"), POST_LOGIN_PATH);
  assert.equal(safeReturnPath("https://evil.example.com"), POST_LOGIN_PATH);
  assert.equal(safeReturnPath("javascript:alert(1)"), POST_LOGIN_PATH);
  assert.equal(safeReturnPath(undefined), POST_LOGIN_PATH);
  assert.equal(safeReturnPath(42), POST_LOGIN_PATH);
});

test("the login page and the public landing page are never a post-login destination", () => {
  assert.equal(safeReturnPath("/login"), "/dashboard");
  assert.equal(safeReturnPath("/login?next=/x"), "/dashboard");
  assert.equal(safeReturnPath("/"), "/dashboard");
});

test("with no saved destination the owner goes to the dashboard after sign-in", () => {
  assert.equal(POST_LOGIN_PATH, "/dashboard");
  assert.equal(safeReturnPath(undefined), "/dashboard");
});

test("a logout that gets 401 counts as already signed out, other failures do not", () => {
  assert.equal(interpretLogoutError({ status: 401 }), "signed-out");
  assert.equal(interpretLogoutError({ status: 500 }), "failed");
  assert.equal(interpretLogoutError({ status: 0 }), "failed");
  assert.equal(interpretLogoutError(undefined), "failed");
});
