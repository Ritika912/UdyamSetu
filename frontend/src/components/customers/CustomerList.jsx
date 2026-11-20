import { useState } from "react";
import { Plus, Users } from "lucide-react";
import Panel from "../Panel.jsx";
import LoadingState from "../LoadingState.jsx";
import ErrorMessage from "../ErrorMessage.jsx";
import EmptyState from "../EmptyState.jsx";
import CustomerDetailsModal from "./CustomerDetailsModal.jsx";
import CustomerFormModal from "./CustomerFormModal.jsx";
import DeleteCustomerModal from "./DeleteCustomerModal.jsx";
import useApiData from "../../hooks/useApiData.js";
import { getCustomers } from "../../api/customersApi.js";
import { formatDateTime, getInitials } from "../../utils/formatting.js";

// Defined outside the component so useApiData receives a stable reference.
function loadCustomers({ signal }) {
  return getCustomers({ signal });
}

/**
 * Customer table with the add, view, edit and delete flows. Only one dialog is open at a time.
 * The dialog state is { mode, customer }: "add", "view", "edit" or "delete".
 */
function CustomerList() {
  const customers = useApiData(loadCustomers);
  const [dialog, setDialog] = useState(null);
  const [notice, setNotice] = useState("");

  function openAddDialog() {
    setNotice("");
    setDialog({ mode: "add", customer: null });
  }

  function closeDialog() {
    setDialog(null);
  }

  // Called only after the backend has confirmed the change. Refreshes the list.
  function finishChange(message) {
    setDialog(null);
    setNotice(message);
    customers.reload();
  }

  let content;

  if (customers.isLoading) {
    content = <LoadingState message="Loading customers…" />;
  } else if (customers.error) {
    content = <ErrorMessage error={customers.error} onRetry={customers.reload} />;
  } else if (customers.data.length === 0) {
    content = (
      <EmptyState
        icon={Users}
        title="No customers yet"
        description="Customers are added when you record their enquiry, or you can add one now."
        action={
          <button className="primary-button" type="button" onClick={openAddDialog}>
            <Plus size={19} />
            Add Customer
          </button>
        }
      />
    );
  } else {
    content = (
      <div className="table-container">
        <table className="enquiries-table">
          <thead>
            <tr>
              <th>Customer</th>
              <th>Phone number</th>
              <th>Added</th>
              <th>
                <span className="visually-hidden">Actions</span>
              </th>
            </tr>
          </thead>

          <tbody>
            {customers.data.map((customer) => (
              <tr key={customer.id}>
                <td>
                  <div className="customer-cell">
                    <div className="customer-avatar">{getInitials(customer.customerName)}</div>
                    <div>
                      <strong>{customer.customerName}</strong>
                    </div>
                  </div>
                </td>
                <td>{customer.contactNumber}</td>
                <td>{formatDateTime(customer.createdAt)}</td>
                <td>
                  <button
                    className="text-button"
                    type="button"
                    onClick={() => setDialog({ mode: "view", customer })}
                    aria-label={`View customer ${customer.customerName}`}
                  >
                    View
                  </button>
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
        title="All customers"
        description="Everyone who has contacted your business, newest first."
        action={
          <button className="primary-button" type="button" onClick={openAddDialog}>
            <Plus size={19} />
            Add Customer
          </button>
        }
      >
        {content}
      </Panel>

      {dialog?.mode === "add" && (
        <CustomerFormModal
          onClose={closeDialog}
          onSaved={(saved) => finishChange(`Customer ${saved.customerName} added.`)}
        />
      )}

      {dialog?.mode === "view" && (
        <CustomerDetailsModal
          key={dialog.customer.id}
          customer={dialog.customer}
          onClose={closeDialog}
          onEdit={() => setDialog({ mode: "edit", customer: dialog.customer })}
          onDelete={() => setDialog({ mode: "delete", customer: dialog.customer })}
        />
      )}

      {dialog?.mode === "edit" && (
        <CustomerFormModal
          customer={dialog.customer}
          onClose={() => setDialog({ mode: "view", customer: dialog.customer })}
          onSaved={(saved) => finishChange(`Customer ${saved.customerName} updated.`)}
        />
      )}

      {dialog?.mode === "delete" && (
        <DeleteCustomerModal
          customer={dialog.customer}
          onClose={() => setDialog({ mode: "view", customer: dialog.customer })}
          onDeleted={() => finishChange(`Customer ${dialog.customer.customerName} deleted.`)}
        />
      )}
    </>
  );
}

export default CustomerList;
