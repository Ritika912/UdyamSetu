import Modal from "./Modal.jsx";

/**
 * Plain-language guide for business owners. Describes only what the app does today,
 * and says so where a section is still being built.
 *
 * @param {object} props
 * @param {boolean} props.isOpen
 * @param {() => void} props.onClose
 */
function HelpDialog({ isOpen, onClose }) {
  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="How UdyamSetu works"
      description="A short guide for your business."
    >
      <div className="help-content">
        <section className="help-section">
          <h3>What UdyamSetu does</h3>
          <p>
            UdyamSetu helps you record customer enquiries, keep track of your customers and services,
            and see which services customers ask for most, so you can grow your business.
          </p>
        </section>

        <section className="help-section">
          <h3>Enquiries</h3>
          <p>
            The dashboard shows your five most recent enquiries and how many are new, due for follow-up
            today, or overdue. Each enquiry has a status:
          </p>
          <ul>
            <li><strong>New</strong>, <strong>In Progress</strong>, <strong>Pending</strong>, <strong>Converted</strong></li>
            <li><strong>Service unavailable:</strong> you could not provide the service the customer asked for.</li>
            <li><strong>Customer did not proceed:</strong> the customer did not go ahead for another reason.</li>
          </ul>
          <p>The Enquiries page, where you will add and update enquiries, is still being built.</p>
        </section>

        <section className="help-section">
          <h3>Customers and services</h3>
          <p>
            Customers keeps each customer's contact details and their enquiry history. Services lists
            what your business offers. Both pages are still being built.
          </p>
        </section>

        <section className="help-section">
          <h3>Insights and opportunities</h3>
          <p>
            The Business Assistant on the dashboard suggests what to do next, based on the last 30 days.
            Each suggestion shows the number of enquiries behind it, so you can check it yourself. Suggestions
            come from fixed rules, not guesses. The full Business Insights page is still being built.
          </p>
        </section>

        <div className="modal-actions">
          <button className="primary-button" type="button" onClick={onClose}>
            Got it
          </button>
        </div>
      </div>
    </Modal>
  );
}

export default HelpDialog;
