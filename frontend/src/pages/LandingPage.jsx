import LandingHeader from "../components/landing/LandingHeader.jsx";
import {
  ContactSection,
  FeaturesSection,
  FinalCtaSection,
  HeroSection,
  HowItWorksSection,
  LandingFooter,
  ProblemSection,
} from "../components/landing/LandingSections.jsx";
import "../components/landing/landing.css";

/** Public landing page at "/". No sign-in is needed to view it. */
function LandingPage() {
  return (
    <div className="landing-page">
      <LandingHeader />
      <main>
        <HeroSection />
        <ProblemSection />
        <FeaturesSection />
        <HowItWorksSection />
        <FinalCtaSection />
        <ContactSection />
      </main>
      <LandingFooter />
    </div>
  );
}

export default LandingPage;
