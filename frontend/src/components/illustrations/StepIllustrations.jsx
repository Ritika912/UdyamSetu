/** Brand-colour helpers: SVG presentation attributes cannot read CSS variables, so styles are set inline. */
const fill = (name) => ({ fill: `var(--${name})` });
const stroke = (name) => ({ stroke: `var(--${name})` });

/*
 * Three small tiles for the "How It Works" steps. Each is decorative (the step title carries the meaning),
 * so each SVG is hidden from screen readers.
 */

/** Capturing an enquiry: an enquiry card and a message bubble. */
export function CaptureIllustration() {
  return (
    <svg className="step-illustration" viewBox="0 0 96 96" aria-hidden="true" focusable="false" role="presentation">
      <rect width="96" height="96" rx="24" style={fill("peach")} />
      <rect x="18" y="24" width="50" height="46" rx="9" style={fill("white")} />
      <rect x="26" y="34" width="34" height="5" rx="2.5" style={fill("plum")} />
      <rect x="26" y="45" width="24" height="5" rx="2.5" style={fill("muted")} />
      <rect x="26" y="56" width="30" height="5" rx="2.5" style={fill("muted")} />
      <circle cx="68" cy="64" r="14" style={fill("teal")} />
      <circle cx="62" cy="64" r="2.4" style={fill("white")} />
      <circle cx="68" cy="64" r="2.4" style={fill("white")} />
      <circle cx="74" cy="64" r="2.4" style={fill("white")} />
    </svg>
  );
}

/** Understanding demand: three customers feeding a simple bar chart. */
export function DemandIllustration() {
  return (
    <svg className="step-illustration" viewBox="0 0 96 96" aria-hidden="true" focusable="false" role="presentation">
      <rect width="96" height="96" rx="24" style={fill("mint")} />
      <circle cx="28" cy="26" r="7" style={fill("yellow")} />
      <circle cx="48" cy="20" r="7" style={fill("yellow")} />
      <circle cx="68" cy="26" r="7" style={fill("yellow")} />
      <path d="M28 26 L48 46 L68 26" style={{ ...stroke("plum"), strokeWidth: 2, fill: "none", opacity: 0.35 }} />
      <rect x="22" y="58" width="12" height="22" rx="4" style={fill("teal")} />
      <rect x="40" y="50" width="12" height="30" rx="4" style={fill("teal-dark")} />
      <rect x="58" y="42" width="12" height="38" rx="4" style={fill("teal")} />
    </svg>
  );
}

/** Discovering opportunities: a lightbulb with rays. */
export function OpportunityIllustration() {
  return (
    <svg className="step-illustration" viewBox="0 0 96 96" aria-hidden="true" focusable="false" role="presentation">
      <rect width="96" height="96" rx="24" style={fill("yellow")} />
      <circle cx="48" cy="44" r="18" style={fill("white")} />
      <rect x="40" y="62" width="16" height="10" rx="3" style={fill("plum")} />
      <path d="M48 14 L48 20 M26 26 L31 31 M70 26 L65 31" style={{ ...stroke("teal-dark"), strokeWidth: 4, strokeLinecap: "round", fill: "none" }} />
      <path d="M40 46 L46 52 L56 40" style={{ ...stroke("teal"), strokeWidth: 3, strokeLinecap: "round", strokeLinejoin: "round", fill: "none" }} />
    </svg>
  );
}
