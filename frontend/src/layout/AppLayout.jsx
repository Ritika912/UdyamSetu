import { useState } from "react";
import { NavLink, Outlet, useLocation } from "react-router";
import {
  LayoutDashboard,
  ClipboardList,
  Users,
  Package,
  TrendingUp,
  Sparkles,
  CircleQuestionMark,
  LogOut,
  MessageSquare,
  MoreHorizontal,
} from "lucide-react";
import NotificationPopover from "../components/NotificationPopover.jsx";
import HelpDialog from "../components/HelpDialog.jsx";
import ErrorMessage from "../components/ErrorMessage.jsx";
import { useAuth } from "../auth/AuthContext.js";

/** Main sections shown in the sidebar. `end` makes "/" match only the dashboard itself. */
const NAV_ITEMS = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { to: "/enquiries", label: "Enquiries", icon: ClipboardList },
  { to: "/customers", label: "Customers", icon: Users },
  { to: "/services", label: "Services", icon: Package },
  { to: "/insights", label: "Business Insights & Opportunities", icon: TrendingUp },
];

function AppLayout() {
  const { pathname } = useLocation();
  const { logout } = useAuth();
  const [isHelpOpen, setIsHelpOpen] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState("");
  const currentItem =
    NAV_ITEMS.find((item) => (item.end ? pathname === item.to : pathname.startsWith(item.to))) ??
    NAV_ITEMS[0];

  async function handleLogout() {
    if (isLoggingOut) return;
    setIsLoggingOut(true);
    setLogoutError("");
    try {
      // On success the provider sends the owner to /login, which unmounts this layout.
      await logout();
    } catch {
      setLogoutError("We could not sign you out. Check your connection and try again.");
      setIsLoggingOut(false);
    }
  }

  return (
    <div className="app-container">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon">
            <Sparkles size={22} />
          </div>
          <div>
            <h2>UdyamSetu</h2>
            <p>Your bridge to growth</p>
          </div>
        </div>

        <div className="sidebar-section">
          <p className="sidebar-heading">WORKSPACE</p>

          <nav className="sidebar-nav">
            {NAV_ITEMS.map((item) => {
              const Icon = item.icon;

              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) => `nav-item ${isActive ? "active" : ""}`}
                >
                  <Icon size={19} />
                  <span>{item.label}</span>
                </NavLink>
              );
            })}
          </nav>
        </div>

        <div className="sidebar-bottom">
          <div className="help-card">
            <div className="help-icon">
              <MessageSquare size={19} />
            </div>
            <h4>Need a little help?</h4>
            <p>Your business assistant is always here for you.</p>
            <button type="button" onClick={() => setIsHelpOpen(true)}>
              Explore help
            </button>
          </div>

          <div className="profile">
            <div className="profile-avatar">S</div>
            <div className="profile-info">
              <strong>Small Business</strong>
              <span>Business Owner</span>
            </div>
            <MoreHorizontal size={20} />
          </div>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div className="breadcrumb">
            <span>Workspace</span>
            <span>/</span>
            <strong>{currentItem.label}</strong>
          </div>

          <div className="topbar-actions">
            <NotificationPopover />

            <button
              className="icon-button"
              type="button"
              aria-label="Help"
              onClick={() => setIsHelpOpen(true)}
            >
              <CircleQuestionMark size={19} />
            </button>

            <button
              className="logout-button"
              type="button"
              aria-label="Log out"
              onClick={handleLogout}
              disabled={isLoggingOut}
            >
              <LogOut size={17} aria-hidden="true" />
              <span>{isLoggingOut ? "Signing out…" : "Log out"}</span>
            </button>

            <div className="topbar-avatar">S</div>
          </div>
        </header>

        {logoutError && <ErrorMessage error={logoutError} />}

        <Outlet />
      </main>

      <HelpDialog isOpen={isHelpOpen} onClose={() => setIsHelpOpen(false)} />
    </div>
  );
}

export default AppLayout;
