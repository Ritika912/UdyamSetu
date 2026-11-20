/** Brand-colour helpers: SVG presentation attributes cannot read CSS variables, so styles are set inline. */
const fill = (name, opacity) => (opacity === undefined ? { fill: `var(--${name})` } : { fill: `var(--${name})`, fillOpacity: opacity });
const stroke = (name) => ({ stroke: `var(--${name})` });

/**
 * Hero artwork: a small-business owner working at a desk in a bright room. Decorative only, so the whole
 * SVG is hidden from screen readers. The floating enquiry notifications are separate HTML elements.
 */
function HeroIllustration() {
  return (
    <svg className="hero-illustration" viewBox="0 0 520 460" aria-hidden="true" focusable="false" role="presentation">
      {/* room */}
      <rect x="18" y="28" width="484" height="410" rx="52" style={fill("lavender")} />
      <circle cx="470" cy="118" r="20" style={fill("yellow")} />
      <circle cx="54" cy="120" r="12" style={fill("mint")} />

      {/* window with a simple rising line */}
      <rect x="312" y="62" width="150" height="142" rx="20" style={fill("white")} />
      <path d="M387 62 V204 M312 133 H462" style={{ ...stroke("lavender"), strokeWidth: 5 }} />
      <polyline points="330,176 352,154 374,162 404,126 436,108" style={{ ...stroke("teal"), strokeWidth: 4, strokeLinecap: "round", strokeLinejoin: "round", fill: "none" }} />
      <circle cx="436" cy="108" r="5.5" style={fill("teal")} />

      {/* desk */}
      <rect x="34" y="342" width="452" height="18" rx="9" style={fill("peach")} />
      <rect x="66" y="360" width="11" height="62" rx="4" style={fill("plum", 0.85)} />
      <rect x="443" y="360" width="11" height="62" rx="4" style={fill("plum", 0.85)} />

      {/* owner, seated */}
      <path d="M92 344 Q88 238 146 226 Q204 238 200 344 Z" style={fill("teal")} />
      <path d="M116 230 L146 262 L176 230 Z" style={fill("white")} />
      <circle cx="146" cy="188" r="31" style={fill("peach")} />
      <path d="M115 186 Q112 150 146 148 Q180 150 177 186 Q168 162 146 162 Q124 164 115 186 Z" style={fill("plum")} />
      <path d="M188 268 Q236 264 262 284" style={{ ...stroke("peach"), strokeWidth: 17, strokeLinecap: "round", fill: "none" }} />

      {/* laptop */}
      <rect x="262" y="214" width="176" height="110" rx="12" style={{ fill: "var(--white)", stroke: "var(--plum)", strokeWidth: 3 }} />
      <rect x="286" y="282" width="18" height="24" rx="4" style={fill("teal")} />
      <rect x="312" y="262" width="18" height="44" rx="4" style={fill("mint")} />
      <rect x="338" y="244" width="18" height="62" rx="4" style={fill("yellow")} />
      <rect x="250" y="322" width="200" height="14" rx="7" style={fill("plum")} />

      {/* mug */}
      <rect x="216" y="304" width="26" height="38" rx="7" style={fill("white")} />
      <path d="M242 314 Q254 314 254 326 Q254 336 242 336" style={{ ...stroke("white"), strokeWidth: 5, fill: "none" }} />

      {/* plant */}
      <rect x="452" y="290" width="40" height="52" rx="10" style={fill("yellow")} />
      <circle cx="466" cy="268" r="24" style={fill("teal", 0.85)} />
      <circle cx="446" cy="282" r="15" style={fill("mint")} />
      <circle cx="488" cy="278" r="17" style={fill("teal")} />
    </svg>
  );
}

export default HeroIllustration;
