/**
 * Summary card used in the dashboard stats row.
 *
 * @param {object} props
 * @param {string} props.title
 * @param {string|number} props.value
 * @param {string} [props.change]       short badge text, e.g. "3 urgent"; omitted when not provided
 * @param {string} [props.description]
 * @param {React.ComponentType} props.icon  lucide-react icon component
 * @param {"lavender"|"peach"|"yellow"|"mint"} props.color
 */
function StatCard({ title, value, change, description, icon: Icon, color }) {
  return (
    <div className="stat-card">
      <div className="stat-top">
        <div className={`stat-icon ${color}`}>
          <Icon size={21} />
        </div>
        {change && <span className="stat-change">{change}</span>}
      </div>

      <div className="stat-details">
        <h2>{value}</h2>
        <p className="stat-title">{title}</p>
        {description && <span className="stat-description">{description}</span>}
      </div>
    </div>
  );
}

export default StatCard;
