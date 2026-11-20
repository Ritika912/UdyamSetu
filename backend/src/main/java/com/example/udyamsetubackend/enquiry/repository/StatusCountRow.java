package com.example.udyamsetubackend.enquiry.repository;

import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;

/**
 * Enquiry count for one status.
 */
public interface StatusCountRow {

    EnquiryStatus getStatus();

    Long getTotal();
}
