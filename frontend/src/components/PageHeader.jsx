/**
 * Page heading used at the top of each section. Uses the existing welcome-section styling
 * so it matches the dashboard header.
 *
 * @param {object} props
 * @param {string} [props.eyebrow]           small uppercase label above the title
 * @param {React.ReactNode} props.title
 * @param {string} [props.description]
 * @param {React.ReactNode} [props.action]   e.g. a primary button, shown at the right
 */
function PageHeader({ eyebrow, title, description, action }) {
  return (
    <section className="welcome-section">
      <div>
        {eyebrow && <p className="welcome-label">{eyebrow}</p>}
        <h1>{title}</h1>
        {description && <p className="welcome-description">{description}</p>}
      </div>

      {action}
    </section>
  );
}

export default PageHeader;
