package com.example.udyamsetubackend.enquiry.service;

import com.example.udyamsetubackend.enquiry.entity.Enquiry;
import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the optional filters used when listing enquiries.
 */
public final class EnquirySpecifications {

    private EnquirySpecifications() {
    }

    /**
     * @param status     when non-null, only enquiries with this status
     * @param customerId when non-null, only enquiries belonging to this customer
     */
    public static Specification<Enquiry> matching(EnquiryStatus status, Long customerId) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (customerId != null) {
                predicates.add(builder.equal(root.get("customer").get("id"), customerId));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
