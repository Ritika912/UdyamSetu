import { useState } from "react";
import { Plus } from "lucide-react";
import PageHeader from "../components/PageHeader.jsx";
import FormField from "../components/FormField.jsx";
import AddEnquiryModal from "../components/enquiries/AddEnquiryModal.jsx";
import EnquiryList from "../components/enquiries/EnquiryList.jsx";
import { ENQUIRY_STATUS_VALUES, getEnquiryStatusDisplay } from "../utils/enquiryStatus.js";

function EnquiriesPage() {
  const [statusFilter, setStatusFilter] = useState("");
  // Changing this remounts the list, so it reloads after an enquiry is saved.
  const [listVersion, setListVersion] = useState(0);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [savedNotice, setSavedNotice] = useState("");

  function openAddModal() {
    setSavedNotice("");
    setIsAddModalOpen(true);
  }

  // Called only after the backend has confirmed the enquiry was created (HTTP 201).
  function handleEnquirySaved(savedEnquiry) {
    setIsAddModalOpen(false);
    setSavedNotice(`Enquiry from ${savedEnquiry.customerName} saved.`);
    setListVersion((version) => version + 1);
  }

  return (
    <div className="dashboard-content">
      <PageHeader
        eyebrow="ENQUIRY MANAGEMENT"
        title="Enquiries"
        description="Capture customer enquiries, track their status and follow-ups."
        action={
          <button className="primary-button" type="button" onClick={openAddModal}>
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

      <div className="enquiry-toolbar">
        <FormField label="Show enquiries with status">
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
            <option value="">All statuses</option>
            {ENQUIRY_STATUS_VALUES.map((status) => (
              <option key={status} value={status}>
                {getEnquiryStatusDisplay(status).label}
              </option>
            ))}
          </select>
        </FormField>
      </div>

      <EnquiryList
        key={`${statusFilter}:${listVersion}`}
        statusFilter={statusFilter}
        onAddEnquiry={openAddModal}
      />

      {isAddModalOpen && (
        <AddEnquiryModal onClose={() => setIsAddModalOpen(false)} onSaved={handleEnquirySaved} />
      )}
    </div>
  );
}

export default EnquiriesPage;
