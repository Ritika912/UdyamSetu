import { useState } from "react";
import PageHeader from "../components/PageHeader.jsx";
import ServiceList from "../components/services/ServiceList.jsx";

const VIEWS = Object.freeze([
  { value: "active", label: "Active services" },
  { value: "inactive", label: "Inactive" },
]);

function ServicesPage() {
  const [view, setView] = useState("active");

  return (
    <div className="dashboard-content">
      <PageHeader
        eyebrow="CUSTOMERS & SERVICES"
        title="Services"
        description="Manage the services your business offers."
      />

      <div className="segmented-control" role="group" aria-label="Choose which services to show">
        {VIEWS.map((option) => (
          <button
            key={option.value}
            type="button"
            aria-pressed={view === option.value}
            onClick={() => setView(option.value)}
          >
            {option.label}
          </button>
        ))}
      </div>

      <ServiceList key={view} view={view} />
    </div>
  );
}

export default ServicesPage;
