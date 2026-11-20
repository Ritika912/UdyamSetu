import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import PageHeader from "../components/PageHeader.jsx";
import AddEnquiryModal from "../components/enquiries/AddEnquiryModal.jsx";
import DashboardStats from "../components/dashboard/DashboardStats.jsx";
import RecentEnquiriesPanel from "../components/dashboard/RecentEnquiriesPanel.jsx";
import BusinessAssistantPanel from "../components/dashboard/BusinessAssistantPanel.jsx";
import useApiData from "../hooks/useApiData.js";
import { getDashboard } from "../api/dashboardApi.js";
import { getEnquiries } from "../api/enquiriesApi.js";
import { getOpportunities, INSIGHT_PERIODS } from "../api/insightsApi.js";

const RECENT_ENQUIRY_COUNT = 5;

// Fetch functions live outside the component so useApiData receives a stable reference.
function loadDashboard({ signal }) {
  return getDashboard({ signal });
}

function loadRecentEnquiries({ signal }) {
  // GET /api/enquiries returns all enquiries, newest first. The dashboard shows only the first few.
  return getEnquiries({ signal }).then((enquiries) => enquiries.slice(0, RECENT_ENQUIRY_COUNT));
}

function loadOpportunities({ signal }) {
  return getOpportunities({ period: INSIGHT_PERIODS.LAST_30_DAYS, signal });
}

function getIstGreeting() {
  const currentHour = Number(
    new Intl.DateTimeFormat("en-IN", {
      timeZone: "Asia/Kolkata",
      hour: "2-digit",
      hourCycle: "h23",
    }).format(new Date())
  );

  if (currentHour < 12) return "Good morning!";
  if (currentHour < 17) return "Good afternoon!";
  return "Good evening!";
}

function DashboardPage() {
  const [greeting, setGreeting] = useState(getIstGreeting);

  useEffect(() => {
    const timer = setInterval(() => {
      setGreeting(getIstGreeting());
    }, 60000);

    return () => clearInterval(timer);
  }, []);

  const [isEnquiryModalOpen, setIsEnquiryModalOpen] = useState(false);
  const [savedNotice, setSavedNotice] = useState("");

  const dashboard = useApiData(loadDashboard);
  const recentEnquiries = useApiData(loadRecentEnquiries);
  const opportunities = useApiData(loadOpportunities);

  function openEnquiryModal() {
    setSavedNotice("");
    setIsEnquiryModalOpen(true);
  }

  // Called only after the backend has confirmed the enquiry was created (HTTP 201).
  function handleEnquirySaved(savedEnquiry) {
    setIsEnquiryModalOpen(false);
    setSavedNotice(`Enquiry from ${savedEnquiry.customerName} saved.`);
    dashboard.reload();
    recentEnquiries.reload();
    opportunities.reload();
  }

  return (
    <>
      <div className="dashboard-content">
        <PageHeader
          eyebrow="YOUR BUSINESS AT A GLANCE"
          title={`${greeting} 👋`}
          description="Here's what's happening with your business today."
          action={
            <button className="primary-button" type="button" onClick={openEnquiryModal}>
              <Plus size={19} />
              Add Enquiry
            </button>
          }
        />

        {savedNotice && (
          <p className="save-notice" role="status">
            {savedNotice}
          </p>
        )}

        <DashboardStats result={dashboard} />

        <section className="dashboard-grid">
          <RecentEnquiriesPanel result={recentEnquiries} />
          <BusinessAssistantPanel result={opportunities} />
        </section>

        <footer className="dashboard-footer">
          <span>Made for growing businesses</span>
          <span>UdyamSetu © 2026</span>
        </footer>
      </div>

      {isEnquiryModalOpen && (
        <AddEnquiryModal
          onClose={() => setIsEnquiryModalOpen(false)}
          onSaved={handleEnquirySaved}
        />
      )}
    </>
  );
}

export default DashboardPage;
