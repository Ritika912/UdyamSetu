import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

/**
 * Guards the redesigned login page: no sign-up or registration wording, no external references, no emoji,
 * no hardcoded colours in the illustration, and the sign-in form fields are still present.
 */
const read = (relativePath) => readFileSync(fileURLToPath(new URL(relativePath, import.meta.url)), "utf8");

/** Removes comments, so developer notes do not count as page content. */
const withoutComments = (text) => text.replace(/\/\*[\s\S]*?\*\//g, "").replace(/^\s*\/\/.*$/gm, "");

const loginPage = withoutComments(read("./LoginPage.jsx"));
const showcase = withoutComments(read("../components/login/LoginShowcase.jsx"));
const loginSource = `${loginPage}\n${showcase}`;

test("the login page never implies that sign-up or registration exists", () => {
  assert.doesNotMatch(loginSource, /sign[\s-]?up|register|registration|create (an )?account|free trial/i);
});

test("the login page loads nothing external and contains no embedded images", () => {
  assert.doesNotMatch(loginSource, /https?:\/\/|<image|xlink:href|@import/i);
});

test("the login page contains no emoji", () => {
  assert.doesNotMatch(loginSource, /[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{2B00}-\u{2BFF}]/u);
});

test("the illustration uses only the brand CSS variables, never literal hex colours", () => {
  assert.doesNotMatch(showcase, /#[0-9a-fA-F]{3,8}\b/);
});

test("every decorative SVG on the login page is hidden from screen readers", () => {
  const svgCount = (showcase.match(/<svg/g) || []).length;
  const hiddenCount = (showcase.match(/aria-hidden="true"/g) || []).length;
  assert.ok(svgCount >= 2, "expected the owner artwork and the demand bars");
  assert.ok(hiddenCount >= svgCount, "every SVG must be hidden from screen readers");
});

test("the sign-in form keeps its fields, labels and password toggle", () => {
  assert.match(loginPage, /name="username"/);
  assert.match(loginPage, /id="login-password"/);
  assert.match(loginPage, /autoComplete="current-password"/);
  assert.match(loginPage, /type=\{showPassword \? "text" : "password"\}/);
  assert.match(loginPage, /aria-pressed=\{showPassword\}/);
  assert.match(loginPage, /Sign in/);
});

test("the page has one main heading, so the heading order stays valid", () => {
  const headings = loginPage.match(/<h1\b/g) || [];
  assert.equal(headings.length, 1);
  assert.doesNotMatch(showcase, /<h1\b|<h2\b/);
});
