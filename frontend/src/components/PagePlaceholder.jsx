import PageHeader from "./PageHeader.jsx";
import Panel from "./Panel.jsx";

/**
 * Temporary content for a section whose screens are not built yet.
 */
function PagePlaceholder({ eyebrow, title, description }) {
  return (
    <div className="dashboard-content">
      <PageHeader eyebrow={eyebrow} title={title} description={description} />

      <Panel title="Coming soon" description="This section is being built. It will appear here once it is ready." />
    </div>
  );
}

export default PagePlaceholder;
