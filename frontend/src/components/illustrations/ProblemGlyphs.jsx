/** Brand-colour helpers: SVG presentation attributes cannot read CSS variables, so styles are set inline. */
const fill = (name, opacity) => (opacity === undefined ? { fill: `var(--${name})` } : { fill: `var(--${name})`, fillOpacity: opacity });
const stroke = (name, opacity) => (opacity === undefined ? { stroke: `var(--${name})` } : { stroke: `var(--${name})`, strokeOpacity: opacity });

/*
 * Three glyphs for the problem section. Each sits on its own tinted panel, so the three problems look
 * different from one another. Decorative only, so each SVG is hidden from screen readers.
 */

/** Enquiries get scattered: four sources that never meet in one place. */
export function ScatteredGlyph() {
  return (
    <svg className="problem-glyph-svg" viewBox="0 0 140 140" aria-hidden="true" focusable="false" role="presentation">
      <path d="M30 30 L70 70 M104 34 L70 70 M30 110 L70 70 M108 108 L70 70" style={{ ...stroke("plum", 0.3), strokeWidth: 2.5, strokeDasharray: "5 6", fill: "none" }} />
      <circle cx="30" cy="30" r="15" style={fill("white")} />
      <circle cx="108" cy="34" r="15" style={fill("white")} />
      <circle cx="30" cy="110" r="15" style={fill("white")} />
      <circle cx="108" cy="108" r="15" style={fill("white")} />
      <circle cx="70" cy="70" r="19" style={fill("teal")} />
      <circle cx="70" cy="70" r="6" style={fill("white")} />
    </svg>
  );
}

/** Follow-ups are forgotten: a calendar with a note that has slipped off the page. */
export function ForgottenGlyph() {
  return (
    <svg className="problem-glyph-svg" viewBox="0 0 140 140" aria-hidden="true" focusable="false" role="presentation">
      <rect x="22" y="30" width="78" height="82" rx="14" style={fill("white")} />
      <rect x="22" y="30" width="78" height="20" rx="10" style={fill("teal")} />
      <rect x="22" y="42" width="78" height="8" style={fill("teal")} />
      <rect x="34" y="62" width="12" height="10" rx="3" style={fill("lavender")} />
      <rect x="54" y="62" width="12" height="10" rx="3" style={fill("lavender")} />
      <rect x="74" y="62" width="12" height="10" rx="3" style={fill("peach")} />
      <rect x="34" y="82" width="12" height="10" rx="3" style={fill("lavender")} />
      <rect x="54" y="82" width="12" height="10" rx="3" style={fill("lavender")} />
      <g transform="rotate(-14 108 92)">
        <rect x="86" y="74" width="46" height="40" rx="6" style={fill("yellow")} />
        <rect x="93" y="84" width="30" height="4" rx="2" style={fill("plum", 0.5)} />
        <rect x="93" y="94" width="20" height="4" rx="2" style={fill("plum", 0.5)} />
      </g>
    </svg>
  );
}

/** Demand patterns stay hidden: a faded bar chart and a magnifier that finds nothing yet. */
export function HiddenDemandGlyph() {
  return (
    <svg className="problem-glyph-svg" viewBox="0 0 140 140" aria-hidden="true" focusable="false" role="presentation">
      <rect x="20" y="88" width="16" height="30" rx="5" style={fill("plum", 0.22)} />
      <rect x="44" y="64" width="16" height="54" rx="5" style={fill("plum", 0.22)} />
      <rect x="68" y="78" width="16" height="40" rx="5" style={fill("plum", 0.22)} />
      <rect x="92" y="98" width="16" height="20" rx="5" style={fill("plum", 0.22)} />
      <circle cx="82" cy="60" r="26" style={{ ...fill("white", 0.9), ...stroke("plum", 0.55), strokeWidth: 5 }} />
      <path d="M100 80 L118 98" style={{ ...stroke("plum", 0.55), strokeWidth: 8, strokeLinecap: "round" }} />
      <path d="M72 60 L92 60" style={{ ...stroke("teal"), strokeWidth: 4, strokeLinecap: "round", strokeDasharray: "2 7" }} />
    </svg>
  );
}
