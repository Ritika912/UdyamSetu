/**
 * White content panel. The header is optional; when given a title, description or action,
 * it uses the existing .panel-header layout.
 *
 * @param {object} props
 * @param {React.ReactNode} [props.title]
 * @param {React.ReactNode} [props.description]
 * @param {React.ReactNode} [props.action]     e.g. a "View all" button, shown at the right
 * @param {string} [props.className]           extra class, e.g. "enquiries-panel"
 * @param {React.ReactNode} props.children
 */
function Panel({ title, description, action, className = "", children }) {
  const hasHeader = title || description || action;

  return (
    <section className={`panel ${className}`.trim()}>
      {hasHeader && (
        <div className="panel-header">
          <div>
            {title && <h3>{title}</h3>}
            {description && <p>{description}</p>}
          </div>

          {action}
        </div>
      )}

      {children}
    </section>
  );
}

export default Panel;
