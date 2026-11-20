import { useEffect, useId, useRef, useState } from "react";
import { Bell } from "lucide-react";
import EmptyState from "./EmptyState.jsx";

/**
 * Header bell button that opens a small notifications panel.
 *
 * No notification source exists yet, so the panel shows only the empty state. No count or
 * unread marker is shown, because there is nothing to count.
 *
 * Closes on Escape, on a click outside, or when keyboard focus leaves the component.
 * Escape also returns focus to the bell button.
 */
function NotificationPopover() {
  const [isOpen, setIsOpen] = useState(false);
  const wrapperRef = useRef(null);
  const buttonRef = useRef(null);
  const panelRef = useRef(null);
  const panelId = useId();

  useEffect(() => {
    if (!isOpen) return undefined;

    panelRef.current?.focus();

    function handlePointerDown(event) {
      if (!wrapperRef.current?.contains(event.target)) setIsOpen(false);
    }

    function handleKeyDown(event) {
      if (event.key === "Escape") {
        setIsOpen(false);
        buttonRef.current?.focus();
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen]);

  function handleBlur(event) {
    // Close when focus moves to an element outside the bell and its panel.
    if (!wrapperRef.current?.contains(event.relatedTarget)) setIsOpen(false);
  }

  return (
    <div className="notification-wrapper" ref={wrapperRef} onBlur={handleBlur}>
      <button
        ref={buttonRef}
        className="icon-button"
        type="button"
        aria-label="Notifications"
        aria-haspopup="dialog"
        aria-expanded={isOpen}
        aria-controls={isOpen ? panelId : undefined}
        onClick={() => setIsOpen((previous) => !previous)}
      >
        <Bell size={19} />
      </button>

      {isOpen && (
        <div
          id={panelId}
          ref={panelRef}
          className="notification-panel"
          role="dialog"
          aria-label="Notifications"
          tabIndex={-1}
        >
          <h3>Notifications</h3>
          <EmptyState
            icon={Bell}
            title="No notifications"
            description="You're all caught up for now."
          />
        </div>
      )}
    </div>
  );
}

export default NotificationPopover;
