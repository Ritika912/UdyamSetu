import { useCallback, useState } from "react";
import { Package, Plus } from "lucide-react";
import Panel from "../Panel.jsx";
import LoadingState from "../LoadingState.jsx";
import ErrorMessage from "../ErrorMessage.jsx";
import EmptyState from "../EmptyState.jsx";
import ServiceFormModal from "./ServiceFormModal.jsx";
import ServiceStatusModal from "./ServiceStatusModal.jsx";
import useApiData from "../../hooks/useApiData.js";
import { getServices } from "../../api/servicesApi.js";
import { formatRupees } from "../../utils/formatting.js";

const VIEW_COPY = Object.freeze({
  active: {
    title: "Services you offer",
    description: "Customers can be recorded against these services.",
    emptyTitle: "No services yet",
    emptyDescription: "Add the services your business offers, so enquiries can be linked to them.",
  },
  inactive: {
    title: "Inactive services",
    description: "Hidden from new enquiries. Past enquiries still show them.",
    emptyTitle: "No inactive services",
    emptyDescription: "Services you deactivate appear here as Inactive, and you can reactivate them.",
  },
});

/**
 * Service catalogue for one view, "active" or "inactive". The parent gives this component a `key`
 * equal to the view, so switching views loads fresh data.
 *
 * Dialog state is { mode, service }: "add", "edit", "deactivate" or "reactivate". Only one dialog is open.
 *
 * @param {object} props
 * @param {"active"|"inactive"} props.view
 */
function ServiceList({ view }) {
  const isActiveView = view === "active";
  const copy = VIEW_COPY[view];

  // Memoised on the view so useApiData reloads when the view changes.
  const loadServices = useCallback(
    ({ signal }) => getServices({ active: isActiveView, signal }),
    [isActiveView]
  );
  const services = useApiData(loadServices);
  const [dialog, setDialog] = useState(null);
  const [notice, setNotice] = useState("");

  function closeDialog() {
    setDialog(null);
  }

  // Called only after the backend confirms the change. Refreshes the catalogue.
  function finishChange(message) {
    setDialog(null);
    setNotice(message);
    services.reload();
  }

  function openAddDialog() {
    setNotice("");
    setDialog({ mode: "add", service: null });
  }

  let content;

  if (services.isLoading) {
    content = <LoadingState message="Loading services…" />;
  } else if (services.error) {
    content = <ErrorMessage error={services.error} onRetry={services.reload} />;
  } else if (services.data.length === 0) {
    content = (
      <EmptyState
        icon={Package}
        title={copy.emptyTitle}
        description={copy.emptyDescription}
        action={
          isActiveView ? (
            <button className="primary-button" type="button" onClick={openAddDialog}>
              <Plus size={19} />
              Add Service
            </button>
          ) : undefined
        }
      />
    );
  } else {
    content = (
      <div className="table-container">
        <table className="enquiries-table">
          <thead>
            <tr>
              <th>Service</th>
              <th>Description</th>
              <th>Price</th>
              <th>Status</th>
              <th>
                <span className="visually-hidden">Actions</span>
              </th>
            </tr>
          </thead>

          <tbody>
            {services.data.map((service) => (
              <tr key={service.id}>
                <td>
                  <div className="service-name-cell">
                    <strong>{service.name}</strong>
                    {service.category && <span>{service.category}</span>}
                  </div>
                </td>
                <td>
                  <span className="service-description">{service.description || "No description"}</span>
                </td>
                <td>{service.price === null ? "Price not set" : formatRupees(service.price)}</td>
                <td>
                  <span className={`service-status ${service.isActive ? "active" : "inactive"}`}>
                    {service.isActive ? "Active" : "Inactive"}
                  </span>
                </td>
                <td>
                  <div className="row-actions">
                    <button
                      className="text-button"
                      type="button"
                      onClick={() => setDialog({ mode: "edit", service })}
                      aria-label={`Edit ${service.name}`}
                    >
                      Edit
                    </button>
                    {service.isActive ? (
                      <button
                        className="text-button"
                        type="button"
                        onClick={() => setDialog({ mode: "deactivate", service })}
                        aria-label={`Deactivate ${service.name}`}
                      >
                        Deactivate
                      </button>
                    ) : (
                      <button
                        className="text-button"
                        type="button"
                        onClick={() => setDialog({ mode: "reactivate", service })}
                        aria-label={`Reactivate ${service.name}`}
                      >
                        Reactivate
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <>
      {notice && (
        <p className="save-notice" role="status">
          {notice}
        </p>
      )}

      <Panel
        title={copy.title}
        description={copy.description}
        action={
          isActiveView ? (
            <button className="primary-button" type="button" onClick={openAddDialog}>
              <Plus size={19} />
              Add Service
            </button>
          ) : undefined
        }
      >
        {content}
      </Panel>

      {dialog?.mode === "add" && (
        <ServiceFormModal
          onClose={closeDialog}
          onSaved={(saved) => finishChange(`Service "${saved.name}" added.`)}
        />
      )}

      {dialog?.mode === "edit" && (
        <ServiceFormModal
          service={dialog.service}
          onClose={closeDialog}
          onSaved={(saved) => finishChange(`Service "${saved.name}" updated.`)}
        />
      )}

      {dialog?.mode === "deactivate" && (
        <ServiceStatusModal
          service={dialog.service}
          mode="deactivate"
          onClose={closeDialog}
          onDone={() =>
            finishChange(
              `"${dialog.service.name}" is now Inactive. It is hidden from new enquiries; past enquiries are unchanged.`
            )
          }
        />
      )}

      {dialog?.mode === "reactivate" && (
        <ServiceStatusModal
          service={dialog.service}
          mode="reactivate"
          onClose={closeDialog}
          onDone={() => finishChange(`"${dialog.service.name}" is now Active.`)}
        />
      )}
    </>
  );
}

export default ServiceList;
