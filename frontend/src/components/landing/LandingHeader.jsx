import { Link } from "react-router";
import { Sparkles } from "lucide-react";

/**
 * Public header: logo, section links and Sign In. Section links are plain anchors on the same page.
 * On narrow screens the section links are hidden, and the footer still provides them.
 */
function LandingHeader() {
  return (
    <header className="landing-header">
      <Link to="/" className="landing-logo" aria-label="UdyamSetu home">
        <span className="brand-icon">
          <Sparkles size={20} aria-hidden="true" />
        </span>
        <span className="landing-wordmark">UdyamSetu</span>
      </Link>

      <nav className="landing-nav" aria-label="Main">
        <a className="landing-nav-link" href="#features">
          Features
        </a>
        <a className="landing-nav-link" href="#how-it-works">
          How It Works
        </a>
        <a className="landing-nav-link" href="#contact">
          Contact
        </a>
        <Link className="landing-signin" to="/login">
          Sign In
        </Link>
      </nav>
    </header>
  );
}

export default LandingHeader;
