import { Link } from "react-router";
import {
  ArrowRight,
  Bell,
  Check,
  ClipboardList,
  Lightbulb,
  Mail,
  Package,
  TrendingUp,
  Users,
} from "lucide-react";
import HeroIllustration from "../illustrations/HeroIllustration.jsx";
import {
  CaptureIllustration,
  DemandIllustration,
  OpportunityIllustration,
} from "../illustrations/StepIllustrations.jsx";
import { ForgottenGlyph, HiddenDemandGlyph, ScatteredGlyph } from "../illustrations/ProblemGlyphs.jsx";
import { EnquiryFormVisual } from "../illustrations/FeatureVisuals.jsx";
import ContactForm from "./ContactForm.jsx";
import { SUPPORT_EMAIL } from "./contactDetails.js";
import { submitContactMessage } from "../../api/contactApi.js";

/*
 * Copy is limited to what the application does today. It contains no testimonials, customer numbers,
 * prices, integrations or capabilities that are not built. Sign-in is the only entry point: there is no
 * registration.
 */

const PROBLEMS = [
  {
    glyph: ScatteredGlyph,
    tone: "peach",
    title: "Enquiries get scattered",
    text: "A phone call, a WhatsApp message, a walk-in and a website form all arrive in different places. Details get lost between them.",
  },
  {
    glyph: ForgottenGlyph,
    tone: "mint",
    title: "Follow-ups are forgotten",
    text: "A customer who said “call me next week” can slip through when there is no list of who to call back.",
  },
  {
    glyph: HiddenDemandGlyph,
    tone: "yellow",
    title: "Demand patterns stay hidden",
    text: "When the same service is asked for again and again, it is hard to notice unless every request is written down and counted.",
  },
];

const FEATURES = [
  {
    key: "enquiry",
    icon: ClipboardList,
    title: "Enquiry Management",
    text: "Record each enquiry with where it came from (website, phone, WhatsApp, walk-in or social media), what the customer needs, the budget and a follow-up date. Track its status as it moves on.",
  },
  {
    key: "customer",
    icon: Users,
    title: "Customer Management",
    text: "Keep one record for each customer, with every enquiry they have made, so you can see their history in one place.",
  },
  {
    key: "service",
    icon: Package,
    title: "Service Management",
    text: "Maintain the services you offer, with prices and categories. Deactivate a service you no longer offer without losing the history of past enquiries.",
  },
  {
    key: "insights",
    icon: TrendingUp,
    title: "Business Insights",
    text: "See how your enquiries are progressing, which services customers ask for most, and how demand changes over time.",
  },
  {
    key: "opportunity",
    icon: Lightbulb,
    title: "Growth Opportunities",
    text: "Get plain-language recommendations based on your own enquiries. Each one shows the numbers behind it, so you can check them yourself.",
  },
];

const STEPS = [
  {
    title: "Capture enquiries",
    text: "Record each enquiry in a minute with the customer's name and phone number. Everything else is optional.",
    Illustration: CaptureIllustration,
  },
  {
    title: "Understand customer demand",
    text: "See which services are requested, which ones you could not provide, and how customers respond to your follow-ups.",
    Illustration: DemandIllustration,
  },
  {
    title: "Discover opportunities",
    text: "Review recommendations built from your enquiries, and decide what to do next for your business.",
    Illustration: OpportunityIllustration,
  },
];

/** Hero: headline, supporting text, two calls to action, the illustration and floating activity elements. */
export function HeroSection() {
  return (
    <section className="landing-hero" aria-labelledby="hero-title">
      <div className="landing-hero-copy">
        <p className="landing-tagline">Every enquiry tells a story. Turn it into an opportunity.</p>
        <h1 id="hero-title">Turn every customer enquiry into business growth.</h1>
        <p className="landing-lead">
          UdyamSetu keeps your enquiries, customer records, follow-ups and service requests in one place. It shows
          which services customers ask for, and what you could do next.
        </p>
        <div className="landing-actions">
          <Link className="primary-button landing-cta" to="/login">
            Sign In to Get Started
            <ArrowRight size={18} aria-hidden="true" />
          </Link>
          <a className="landing-secondary" href="#features">
            Explore Features
          </a>
        </div>
      </div>

      <div className="landing-hero-art">
        <HeroIllustration />
        <div className="hero-float hero-float-notification" aria-hidden="true">
          <span className="hero-float-icon tone-mint">
            <Bell size={15} />
          </span>
          <span>New enquiry received</span>
        </div>
        <div className="hero-float hero-float-followup" aria-hidden="true">
          <span className="hero-float-icon tone-teal">
            <Check size={15} />
          </span>
          <span>Follow-up saved</span>
        </div>
        <div className="hero-float hero-float-insight" aria-hidden="true">
          <span className="hero-float-icon tone-yellow">
            <TrendingUp size={15} />
          </span>
          <span>Insights updated</span>
        </div>
      </div>
    </section>
  );
}

/** Problem: three chapters, each with its own panel, glyph and layout. */
export function ProblemSection() {
  return (
    <section className="landing-section" aria-labelledby="problem-title">
      <div className="landing-container">
        <div className="landing-section-heading">
          <p className="landing-eyebrow">THE PROBLEM</p>
          <h2 id="problem-title">Good enquiries are easy to lose</h2>
        </div>
        <ol className="problem-list">
          {PROBLEMS.map((problem, index) => {
            const Glyph = problem.glyph;
            return (
              <li key={problem.title} className={`problem-row tone-${problem.tone}`}>
                <div className="problem-glyph" aria-hidden="true">
                  <Glyph />
                </div>
                <div className="problem-text">
                  <span className="problem-index" aria-hidden="true">
                    0{index + 1}
                  </span>
                  <h3>{problem.title}</h3>
                  <p>{problem.text}</p>
                </div>
              </li>
            );
          })}
        </ol>
      </div>
    </section>
  );
}

/** Features: an asymmetric bento grid. Enquiry Management carries the most visual weight. */
export function FeaturesSection() {
  return (
    <section id="features" className="landing-section landing-section-alt" aria-labelledby="features-title">
      <div className="landing-container">
        <div className="landing-section-heading">
          <p className="landing-eyebrow">FEATURES</p>
          <h2 id="features-title">Everything you need to manage enquiries and grow</h2>
        </div>
        <div className="bento">
          {FEATURES.map((feature) => {
            const Icon = feature.icon;
            return (
              <article key={feature.key} className={`bento-item bento-${feature.key}`}>
                <div className="bento-icon" aria-hidden="true">
                  <Icon size={21} />
                </div>
                <h3>{feature.title}</h3>
                <p>{feature.text}</p>
                {feature.key === "enquiry" && (
                  <div className="bento-visual" aria-hidden="true">
                    <EnquiryFormVisual />
                  </div>
                )}
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
}

/** How it works: three steps in a connected horizontal process on desktop, stacked on mobile. */
export function HowItWorksSection() {
  return (
    <section id="how-it-works" className="landing-section" aria-labelledby="how-title">
      <div className="landing-container">
        <div className="landing-section-heading">
          <p className="landing-eyebrow">HOW IT WORKS</p>
          <h2 id="how-title">From first enquiry to next decision</h2>
        </div>
        <ol className="process">
          {STEPS.map((step, index) => {
            const { Illustration } = step;
            return (
              <li key={step.title} className="process-step">
                <span className="process-number" aria-hidden="true">
                  {index + 1}
                </span>
                <div className="process-art">
                  <Illustration />
                </div>
                <h3>{step.title}</h3>
                <p>{step.text}</p>
              </li>
            );
          })}
        </ol>
      </div>
    </section>
  );
}

/** Closing call to action. It leads to sign-in only, and does not imply that registration exists. */
export function FinalCtaSection() {
  return (
    <section className="landing-final" aria-labelledby="final-title">
      <div className="landing-final-inner">
        <div>
          <p className="landing-final-eyebrow">GET STARTED</p>
          <h2 id="final-title">Turn your next enquiry into an opportunity.</h2>
          <p>
            Sign in to your UdyamSetu workspace to record enquiries, keep customer records, manage your services and
            review your business insights.
          </p>
        </div>
        <Link className="landing-final-cta" to="/login">
          Sign In to Your Workspace
          <ArrowRight size={18} aria-hidden="true" />
        </Link>
      </div>
    </section>
  );
}

/** Contact: the official support email as a mailto link, beside the contact form. */
export function ContactSection() {
  return (
    <section id="contact" className="landing-section" aria-labelledby="contact-title">
      <div className="landing-container landing-contact">
        <div className="contact-intro">
          <div className="landing-section-heading landing-section-heading-left">
            <p className="landing-eyebrow">CONTACT</p>
            <h2 id="contact-title">Get in touch</h2>
            <p className="landing-lead">Questions about UdyamSetu? Email our support team or send us a message.</p>
          </div>
          <div className="contact-card">
            <span className="contact-icon" aria-hidden="true">
              <Mail size={20} />
            </span>
            <div>
              <strong>Support email</strong>
              <a className="contact-email" href={`mailto:${SUPPORT_EMAIL}`}>
                {SUPPORT_EMAIL}
              </a>
            </div>
          </div>
        </div>
        <ContactForm onSubmit={submitContactMessage} />
      </div>
    </section>
  );
}

/** Footer: branding and navigation. */
export function LandingFooter() {
  return (
    <footer className="landing-footer">
      <div className="landing-footer-inner">
        <div className="landing-footer-brand">
          <strong>UdyamSetu</strong>
          <span>Every enquiry tells a story. Turn it into an opportunity.</span>
        </div>
        <nav className="landing-footer-nav" aria-label="Footer">
          <a href="#features">Features</a>
          <a href="#how-it-works">How It Works</a>
          <a href="#contact">Contact</a>
          <Link to="/login">Sign In</Link>
        </nav>
      </div>
      <p className="landing-copyright">UdyamSetu © 2026</p>
    </footer>
  );
}
