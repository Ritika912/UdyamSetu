import PageHeader from "../components/PageHeader.jsx";
import CustomerList from "../components/customers/CustomerList.jsx";

function CustomersPage() {
  return (
    <div className="dashboard-content">
      <PageHeader
        eyebrow="CUSTOMERS & SERVICES"
        title="Customers"
        description="Keep customer records and see each customer's enquiry history."
      />

      <CustomerList />
    </div>
  );
}

export default CustomersPage;
