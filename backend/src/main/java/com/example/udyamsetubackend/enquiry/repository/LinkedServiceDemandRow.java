package com.example.udyamsetubackend.enquiry.repository;

import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;

/**
 * Demand for one catalogue service and status within a period. {@code recentTotal} counts enquiries in the
 * recent half of the period.
 */
public interface LinkedServiceDemandRow {

    Long getServiceId();

    String getServiceName();

    Boolean getActive();

    EnquiryStatus getStatus();

    Long getTotal();

    Long getRecentTotal();
}
