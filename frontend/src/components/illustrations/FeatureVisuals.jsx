/** Brand-colour helpers: SVG presentation attributes cannot read CSS variables, so styles are set inline. */
const fill = (name, opacity) => (opacity === undefined ? { fill: `var(--${name})` } : { fill: `var(--${name})`, fillOpacity: opacity });
const field = { ...fill("white"), stroke: "var(--lavender)", strokeWidth: 2 };

/**
 * Abstract enquiry form for the main feature tile. It shows the shape of a form, not any real customer or
 * data, so it is decorative and hidden from screen readers.
 */
export function EnquiryFormVisual() {
  return (
    <svg className="feature-visual-svg" viewBox="0 0 360 220" aria-hidden="true" focusable="false" role="presentation">
      <rect x="14" y="18" width="332" height="186" rx="22" style={fill("white")} />
      <rect x="34" y="40" width="130" height="10" rx="5" style={fill("plum", 0.75)} />
      <rect x="236" y="30" width="100" height="26" rx="13" style={fill("mint")} />
      <circle cx="252" cy="43" r="5" style={fill("teal")} />
      <rect x="262" y="40" width="60" height="6" rx="3" style={fill("teal-dark", 0.7)} />
      <rect x="34" y="62" width="180" height="26" rx="8" style={field} />
      <rect x="34" y="102" width="132" height="26" rx="8" style={field} />
      <rect x="180" y="102" width="134" height="26" rx="8" style={field} />
      <rect x="34" y="142" width="200" height="26" rx="8" style={field} />
      <rect x="250" y="142" width="64" height="26" rx="13" style={fill("teal")} />
    </svg>
  );
}
