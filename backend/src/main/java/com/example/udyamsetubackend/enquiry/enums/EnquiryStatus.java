package com.example.udyamsetubackend.enquiry.enums;

/**
 * Lifecycle state of an enquiry.
 *
 * <ul>
 *   <li>{@link #UNFULFILLED}: the business could not provide the requested service.</li>
 *   <li>{@link #LOST}: the customer did not proceed for any other reason.</li>
 * </ul>
 */
public enum EnquiryStatus {
    NEW,
    IN_PROGRESS,
    PENDING,
    CONVERTED,
    UNFULFILLED,
    LOST
}
