import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

/**
 * Guards the public landing page against invented content: no email address, domain, testimonial, customer
 * count or pricing claim, and no leftover placeholder pages. The only permitted address is the official support email.
 */
const read = (relativePath) => readFileSync(fileURLToPath(new URL(relativePath, import.meta.url)), "utf8");

/** Removes comments, so developer notes do not count as page content. */
const withoutComments = (text) => text.replace(/\/\*[\s\S]*?\*\//g, "").replace(/^\s*\/\/.*$/gm, "");

const landingSource = withoutComments(
  [
    read("./LandingPage.jsx"),
    read("../components/landing/LandingHeader.jsx"),
    read("../components/landing/LandingSections.jsx"),
    read("../components/landing/ContactForm.jsx"),
    read("../components/landing/contactDetails.js"),
  ].join("\n")
);

const OFFICIAL_SUPPORT_EMAIL = "support@getudyamsetu.com";

test("landing page contains no email address or web domain other than the official support email", () => {
  const sourceWithoutOfficialEmail = landingSource.split(OFFICIAL_SUPPORT_EMAIL).join("");
  assert.doesNotMatch(sourceWithoutOfficialEmail, /@/);
  assert.doesNotMatch(sourceWithoutOfficialEmail, /\.(com|in|org|net|io)\b/i);
});

test("landing page contains no testimonials, customer counts or pricing claims", () => {
  assert.doesNotMatch(landingSource, /testimonial|trusted by|pricing plan|per month|\d+\+\s*(customers|businesses|users)/i);
});

test("landing page shows no placeholder page text", () => {
  assert.doesNotMatch(landingSource, /coming soon/i);
});

test("the contact section links the official support email as a mailto link", () => {
  assert.match(read("../components/landing/LandingSections.jsx"), /href=\{`mailto:\$\{SUPPORT_EMAIL\}`\}/);
  assert.match(read("../components/landing/contactDetails.js"), /SUPPORT_EMAIL = "support@getudyamsetu\.com"/);
  assert.doesNotMatch(landingSource, /placeholder/i);
});

test("the contact form sends only through the API layer, never by its own fetch or timer", () => {
  // The form gets its send function as a prop. The landing page passes the real API call, and the form itself
  // makes no network request and no timed fake success.
  assert.match(read("../components/landing/LandingSections.jsx"), /<ContactForm onSubmit=\{submitContactMessage\} \/>/);
  assert.doesNotMatch(read("../components/landing/ContactForm.jsx"), /fetch\(|setTimeout|XMLHttpRequest/);
});

test("the honeypot field is present, hidden from assistive technology and skipped by the keyboard", () => {
  const formSource = read("../components/landing/ContactForm.jsx");
  assert.match(formSource, /name="website"/);
  assert.match(formSource, /className="contact-honeypot" aria-hidden="true"/);
  assert.match(formSource, /tabIndex=\{-1\}/);
});

const illustrationFiles = [
  "../components/illustrations/HeroIllustration.jsx",
  "../components/illustrations/StepIllustrations.jsx",
  "../components/illustrations/ProblemGlyphs.jsx",
  "../components/illustrations/FeatureVisuals.jsx",
];

const illustrationSource = withoutComments(illustrationFiles.map(read).join("\n"));

test("the landing page never implies that sign-up or registration exists", () => {
  assert.doesNotMatch(landingSource, /sign[\s-]?up|register|registration|create (an )?account|free trial/i);
});

test("illustrations use only the brand CSS variables, never literal hex colours", () => {
  assert.doesNotMatch(illustrationSource, /#[0-9a-fA-F]{3,8}\b/);
});

test("illustrations load nothing external and contain no embedded images", () => {
  assert.doesNotMatch(illustrationSource, /https?:\/\/|<image|xlink:href|@import/i);
});

test("illustrations contain no emoji", () => {
  assert.doesNotMatch(illustrationSource, /\p{Extended_Pictographic}/u);
});

test("the landing page contains no emoji (the copyright symbol is allowed)", () => {
  // Emoji blocks only. The copyright symbol is in the Extended_Pictographic set but is ordinary footer text.
  assert.doesNotMatch(landingSource, /[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{2B00}-\u{2BFF}]/u);
});

test("every decorative SVG is hidden from screen readers", () => {
  const svgCount = (illustrationSource.match(/<svg/g) || []).length;
  const hiddenCount = (illustrationSource.match(/aria-hidden="true"/g) || []).length;
  assert.ok(svgCount >= 4, "expected the hero and three step illustrations");
  assert.equal(hiddenCount, svgCount);
});

test("the landing page links to the protected dashboard only through sign-in", () => {
  assert.doesNotMatch(landingSource, /to="\/dashboard"/);
  assert.match(landingSource, /to="\/login"/);
});
