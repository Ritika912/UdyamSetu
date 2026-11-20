import { useEffect, useId, useRef } from "react";

const FOCUSABLE_SELECTOR = [
  "a[href]",
  "button:not([disabled])",
  "input:not([disabled]):not([type='hidden'])",
  "select:not([disabled])",
  "textarea:not([disabled])",
  "[tabindex]:not([tabindex='-1'])",
].join(",");

/**
 * Accessible dialog. Closes on the × button, the Cancel-style buttons inside children,
 * a click on the dark backdrop, or the Escape key. Focus moves into the dialog when it opens
 * and returns to the previously focused element when it closes. Tab stays inside the dialog.
 *
 * Render it conditionally through `isOpen`; nothing is rendered while closed.
 *
 * @param {object} props
 * @param {boolean} props.isOpen
 * @param {() => void} props.onClose
 * @param {string} props.title
 * @param {string} [props.description]
 * @param {React.ReactNode} props.children
 */
function Modal({ isOpen, onClose, title, description, children }) {
  const dialogRef = useRef(null);
  const titleId = useId();
  const descriptionId = useId();

  // Move focus into the dialog on open, and restore it on close.
  useEffect(() => {
    if (!isOpen) return undefined;

    const previouslyFocused = document.activeElement;
    const dialog = dialogRef.current;
    // Prefer the first form field; otherwise the first focusable element (the close button).
    const firstField = dialog?.querySelector("input:not([disabled]), textarea:not([disabled]), select:not([disabled])");
    const firstFocusable = dialog?.querySelector(FOCUSABLE_SELECTOR);
    (firstField ?? firstFocusable ?? dialog)?.focus();

    return () => {
      if (previouslyFocused instanceof HTMLElement) previouslyFocused.focus();
    };
  }, [isOpen]);

  // Escape closes the dialog; Tab and Shift+Tab cycle within it.
  useEffect(() => {
    if (!isOpen) return undefined;

    function handleKeyDown(event) {
      if (event.key === "Escape") {
        onClose();
        return;
      }

      if (event.key !== "Tab" || !dialogRef.current) return;

      const focusable = Array.from(dialogRef.current.querySelectorAll(FOCUSABLE_SELECTOR));
      if (focusable.length === 0) return;

      const first = focusable[0];
      const last = focusable[focusable.length - 1];

      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  function handleBackdropMouseDown(event) {
    // Only close when the press starts on the backdrop itself, not inside the dialog.
    if (event.target === event.currentTarget) onClose();
  }

  return (
    <div className="modal-overlay" onMouseDown={handleBackdropMouseDown}>
      <div
        className="modal-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descriptionId : undefined}
        ref={dialogRef}
        tabIndex={-1}
      >
        <div className="modal-header">
          <div>
            <h2 id={titleId}>{title}</h2>
            {description && <p id={descriptionId}>{description}</p>}
          </div>

          <button className="modal-close" type="button" aria-label="Close" onClick={onClose}>
            ×
          </button>
        </div>

        {children}
      </div>
    </div>
  );
}

export default Modal;
