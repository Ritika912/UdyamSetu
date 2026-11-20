import { Bell, ClipboardList, Sparkles, TrendingUp } from "lucide-react";

/** Brand-colour helpers: SVG presentation attributes cannot read CSS variables, so styles are set inline. */
const fill = (name, opacity) => (opacity === undefined ? { fill: `var(--${name})` } : { fill: `var(--${name})`, fillOpacity: opacity });
const stroke = (name) => ({ stroke: `var(--${name})` });

/**
 * Owner illustration: a small-business owner at a counter with a phone, with floating interface cards.
 * The cards use generic labels and no figures, so they show no business data. The artwork is decorative and
 * hidden from screen readers; the brand message next to it carries the meaning.
 */
function OwnerArtwork() {
  return (
    <svg className="login-art-svg" viewBox="0 0 420 380" aria-hidden="true" focusable="false" role="presentation">
      <circle cx="210" cy="206" r="164" style={fill("mint")} />
      <path d="M82 330 V186 Q82 70 210 70 Q338 70 338 186 V330 Z" style={fill("white", 0.7)} />

      {/* owner */}
      <path d="M150 336 Q146 236 210 226 Q274 236 270 336 Z" style={fill("teal")} />
      <circle cx="210" cy="186" r="32" style={fill("peach")} />
      <path d="M178 184 Q176 150 210 148 Q244 150 242 184 Q232 162 210 162 Q188 164 178 184 Z" style={fill("plum")} />
      <path d="M252 262 Q270 252 286 232" style={{ ...stroke("peach"), strokeWidth: 15, strokeLinecap: "round", fill: "none" }} />
      <rect x="282" y="186" width="30" height="50" rx="7" style={fill("plum")} />
      <rect x="287" y="193" width="20" height="30" rx="3" style={fill("teal")} />

      {/* counter */}
      <rect x="112" y="330" width="216" height="18" rx="9" style={fill("peach")} />
      <rect x="132" y="348" width="176" height="14" rx="7" style={fill("plum", 0.8)} />
      <rect x="170" y="292" width="34" height="38" rx="6" style={fill("yellow")} />
      <rect x="216" y="300" width="30" height="30" rx="6" style={fill("lavender")} />
    </svg>
  );
}

/** Mini bar chart for the demand card. Pure shape, no values. */
function MiniBars() {
  return (
    <svg className="login-float-bars" viewBox="0 0 96 40" aria-hidden="true" focusable="false" role="presentation">
      <rect x="4" y="22" width="14" height="16" rx="4" style={fill("mint")} />
      <rect x="26" y="14" width="14" height="24" rx="4" style={fill("teal")} />
      <rect x="48" y="18" width="14" height="20" rx="4" style={fill("yellow")} />
      <rect x="70" y="6" width="14" height="32" rx="4" style={fill("teal-dark")} />
    </svg>
  );
}

/**
 * Left-hand brand panel on desktop, and a compact header above the form on mobile.
 * Every floating card is decorative and hidden from screen readers.
 */
function LoginShowcase() {
  return (
    <aside className="login-showcase">
      <div className="login-showcase-brand">
        <span className="brand-icon" aria-hidden="true">
          <Sparkles size={20} />
        </span>
        <span className="login-showcase-name">UdyamSetu</span>
      </div>

      <div className="login-showcase-message">
        <p className="login-showcase-title">Your business enquiries, all in one place.</p>
        <p>Record every enquiry, follow up with customers, and see which services they ask for.</p>
      </div>

      <div className="login-art">
        <OwnerArtwork />

        <div className="login-float login-float-enquiry" aria-hidden="true">
          <span className="login-float-icon tone-lavender">
            <ClipboardList size={16} />
          </span>
          <span className="login-float-text">
            <strong>New enquiry</strong>
            <small>Saved to customer history</small>
          </span>
        </div>

        <div className="login-float login-float-followup" aria-hidden="true">
          <span className="login-float-icon tone-yellow">
            <Bell size={16} />
          </span>
          <span className="login-float-text">
            <strong>Follow-up reminder</strong>
            <small>Call back today</small>
          </span>
        </div>

        <div className="login-float login-float-demand" aria-hidden="true">
          <span className="login-float-icon tone-mint">
            <TrendingUp size={16} />
          </span>
          <span className="login-float-text">
            <strong>Demand insight</strong>
            <MiniBars />
          </span>
        </div>
      </div>
    </aside>
  );
}

export default LoginShowcase;
