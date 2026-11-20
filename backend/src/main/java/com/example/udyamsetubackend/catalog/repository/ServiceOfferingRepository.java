package com.example.udyamsetubackend.catalog.repository;

import com.example.udyamsetubackend.catalog.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long>,
        JpaSpecificationExecutor<ServiceOffering> {

    /** Case-insensitive, matching the case-insensitive unique index on services.name. */
    boolean existsByNameIgnoreCase(String name);

    /** Duplicate check for updates: any other service with this name, ignoring case. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Optional<ServiceOffering> findByNameIgnoreCase(String name);
}
