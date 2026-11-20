package com.example.udyamsetubackend.catalog.service;

import com.example.udyamsetubackend.catalog.entity.ServiceOffering;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the optional filters used when listing services.
 */
public final class ServiceOfferingSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private ServiceOfferingSpecifications() {
    }

    /**
     * @param active       when non-null, filters on the active flag
     * @param nameFragment when non-blank, a case-insensitive partial match on the name;
     *                     {@code %}, {@code _} and the escape character are matched literally
     */
    public static Specification<ServiceOffering> matching(Boolean active, String nameFragment) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (active != null) {
                predicates.add(builder.equal(root.get("active"), active));
            }

            if (nameFragment != null && !nameFragment.isBlank()) {
                String pattern = "%" + escapeLike(nameFragment.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(builder.like(builder.lower(root.get("name")), pattern, LIKE_ESCAPE));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
