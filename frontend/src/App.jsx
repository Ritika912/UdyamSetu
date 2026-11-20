import { Navigate, Route, Routes } from "react-router";
import "./App.css";
import AppLayout from "./layout/AppLayout.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import LandingPage from "./pages/LandingPage.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import DashboardPage from "./pages/DashboardPage.jsx";
import EnquiriesPage from "./pages/EnquiriesPage.jsx";
import CustomersPage from "./pages/CustomersPage.jsx";
import ServicesPage from "./pages/ServicesPage.jsx";
import InsightsPage from "./pages/InsightsPage.jsx";

function App() {
  return (
    <Routes>
      {/* Public: landing page and owner sign-in. */}
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />

      {/* Protected: every business module, reached only after sign-in. */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/enquiries" element={<EnquiriesPage />} />
          <Route path="/customers" element={<CustomersPage />} />
          <Route path="/services" element={<ServicesPage />} />
          <Route path="/insights" element={<InsightsPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
