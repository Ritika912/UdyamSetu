import { test } from "node:test";
import assert from "node:assert/strict";
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative, sep } from "node:path";
import { fileURLToPath } from "node:url";

/**
 * Static checks over the application source (tests excluded):
 * - no web storage, no document.cookie, so no token or session identifier is stored in the browser;
 * - no hardcoded CSRF header name in the API or auth code, so the header comes from the server.
 */
const SRC_DIR = fileURLToPath(new URL("..", import.meta.url));

function sourceFiles(dir) {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) return sourceFiles(path);
    const isSource = /\.(js|jsx)$/.test(name) && !/\.test\.(js|jsx)$/.test(name);
    return isSource ? [path] : [];
  });
}

/** Removes comments, so documentation that names a forbidden API does not count as using it. */
function withoutComments(text) {
  return text.replace(/\/\*[\s\S]*?\*\//g, "").replace(/^\s*\/\/.*$/gm, "");
}

// Forward slashes on every platform, so the path patterns below match on Windows too.
const files = sourceFiles(SRC_DIR).map((path) => ({
  path: `src/${relative(SRC_DIR, path).split(sep).join("/")}`,
  text: withoutComments(readFileSync(path, "utf8")),
}));

test("no application source uses localStorage, sessionStorage or document.cookie", () => {
  const forbidden = /\b(localStorage|sessionStorage|document\.cookie)\b/;
  const hits = files.filter((file) => forbidden.test(file.text)).map((file) => file.path);
  assert.deepEqual(hits, []);
});

test("the CSRF header name is not hardcoded in the API layer, the auth layer or the login page", () => {
  const scoped = files.filter((file) => /^src\/(api|auth)\/|^src\/pages\/LoginPage/.test(file.path));
  const hits = scoped.filter((file) => /X-XSRF-TOKEN/i.test(file.text)).map((file) => file.path);
  assert.deepEqual(hits, []);
});

test("the test scan found the source files it is meant to check", () => {
  assert.ok(files.some((file) => file.path === "src/api/client.js"));
  assert.ok(files.some((file) => file.path === "src/auth/AuthProvider.jsx"));
});
