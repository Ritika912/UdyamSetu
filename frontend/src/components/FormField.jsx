import { Children, cloneElement, useId } from "react";

/**
 * Label, input and validation message as one unit. The single child is the control
 * (input, textarea or select); FormField adds the id, class and accessibility attributes to it.
 *
 * Backend field errors: pass `error={fieldErrors.customerName}` from an ApiError's fieldErrors,
 * or a local validation message. The error is linked to the control with aria-describedby.
 *
 * @param {object} props
 * @param {string} props.label
 * @param {boolean} [props.optional]  appends a grey "(Optional)" marker to the label
 * @param {string} [props.error]      validation message; empty or undefined means no error
 * @param {React.ReactElement} props.children  a single input, textarea or select element
 */
function FormField({ label, optional = false, error, children }) {
  const generatedId = useId();
  const control = Children.only(children);

  const controlId = control.props.id ?? generatedId;
  const errorId = `${controlId}-error`;
  const className = ["form-control", control.props.className].filter(Boolean).join(" ");

  return (
    <div className="form-field">
      <label className="form-label" htmlFor={controlId}>
        {label}
        {optional && <span className="form-optional"> (Optional)</span>}
      </label>

      {cloneElement(control, {
        id: controlId,
        className,
        "aria-invalid": error ? true : undefined,
        "aria-describedby": error ? errorId : undefined,
      })}

      {error && (
        <p className="form-error" id={errorId}>
          {error}
        </p>
      )}
    </div>
  );
}

export default FormField;
