/**
 * Friendly message for a list or section with no content yet.
 *
 * @param {object} props
 * @param {React.ComponentType} [props.icon]    lucide-react icon component
 * @param {string} props.title
 * @param {string} [props.description]
 * @param {React.ReactNode} [props.action]      e.g. an "Add Enquiry" button
 */
function EmptyState({ icon: Icon, title, description, action }) {
  return (
    <div className="empty-state">
      {Icon && (
        <div className="empty-state-icon" aria-hidden="true">
          <Icon size={22} />
        </div>
      )}
      <h3>{title}</h3>
      {description && <p>{description}</p>}
      {action}
    </div>
  );
}

export default EmptyState;
