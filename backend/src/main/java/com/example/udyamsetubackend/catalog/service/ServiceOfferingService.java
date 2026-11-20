package com.example.udyamsetubackend.catalog.service;

import com.example.udyamsetubackend.catalog.dto.ServiceRequest;
import com.example.udyamsetubackend.catalog.dto.ServiceResponse;
import com.example.udyamsetubackend.catalog.entity.ServiceOffering;
import com.example.udyamsetubackend.catalog.repository.ServiceOfferingRepository;
import com.example.udyamsetubackend.common.exception.ConflictException;
import com.example.udyamsetubackend.common.exception.FieldValidationException;
import com.example.udyamsetubackend.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Business rules for the service catalogue.
 *
 * <p>Deactivation is soft: services are never physically deleted, so enquiries that reference a service
 * keep their history. Price and length rules are enforced by {@link ServiceRequest} (HTTP 400) and by the
 * database; this class handles trimming, defaults, duplicate names and existence checks.
 */
@Service
public class ServiceOfferingService {

    private final ServiceOfferingRepository serviceOfferingRepository;

    public ServiceOfferingService(ServiceOfferingRepository serviceOfferingRepository) {
        this.serviceOfferingRepository = serviceOfferingRepository;
    }

    @Transactional
    public ServiceResponse createService(ServiceRequest request) {
        String name = request.name().trim();
        ensureNameAvailable(name, null);

        ServiceOffering service = new ServiceOffering(name);
        service.setDescription(normalizeOptionalText(request.description()));
        service.setCategory(normalizeOptionalText(request.category()));
        service.setPrice(request.price());
        // A new service is active unless the caller explicitly asks otherwise.
        service.setActive(request.isActive() == null || request.isActive());

        return ServiceResponse.from(serviceOfferingRepository.save(service));
    }

    /**
     * Resolves a service that an enquiry wants to reference. Unknown and inactive services are rejected
     * with a field-level 400 on {@code serviceId}, so only currently offered services can be selected.
     */
    @Transactional(readOnly = true)
    public ServiceOffering getActiveServiceForEnquiry(Long serviceId) {
        ServiceOffering service = serviceOfferingRepository.findById(serviceId)
                .orElseThrow(() -> new FieldValidationException(
                        "serviceId", "serviceId does not refer to an existing service"));
        if (!service.isActive()) {
            throw new FieldValidationException("serviceId", "serviceId refers to an inactive service");
        }
        return service;
    }

    @Transactional(readOnly = true)
    public ServiceResponse getService(Long serviceId) {
        return ServiceResponse.from(findServiceOrThrow(serviceId));
    }

    /**
     * Replaces the editable details of a service. A null {@code isActive} leaves the current status
     * unchanged; {@code isActive=true} reactivates a deactivated service.
     */
    @Transactional
    public ServiceResponse updateService(Long serviceId, ServiceRequest request) {
        ServiceOffering service = findServiceOrThrow(serviceId);
        String name = request.name().trim();
        ensureNameAvailable(name, serviceId);

        service.setName(name);
        service.setDescription(normalizeOptionalText(request.description()));
        service.setCategory(normalizeOptionalText(request.category()));
        service.setPrice(request.price());
        if (request.isActive() != null) {
            service.setActive(request.isActive());
        }

        return ServiceResponse.from(serviceOfferingRepository.saveAndFlush(service));
    }

    /**
     * Lists services, optionally filtered by active status and by a partial, case-insensitive name.
     * Results are ordered by name.
     */
    @Transactional(readOnly = true)
    public List<ServiceResponse> listServices(Boolean active, String nameFragment) {
        return serviceOfferingRepository
                .findAll(ServiceOfferingSpecifications.matching(active, nameFragment), Sort.by("name").ascending())
                .stream()
                .map(ServiceResponse::from)
                .toList();
    }

    /**
     * Soft-deactivates a service by setting {@code isActive=false}. Deactivating an inactive service is a
     * no-op, so repeated calls are safe. Nothing is deleted and enquiries are not changed.
     */
    @Transactional
    public void deactivateService(Long serviceId) {
        ServiceOffering service = findServiceOrThrow(serviceId);
        if (service.isActive()) {
            service.setActive(false);
            serviceOfferingRepository.saveAndFlush(service);
        }
    }

    /**
     * Rejects the name if another service already uses it, ignoring case. {@code ownerId} is the service
     * being updated, or null when creating, so a service never clashes with itself.
     */
    private void ensureNameAvailable(String name, Long ownerId) {
        serviceOfferingRepository.findByNameIgnoreCase(name)
                .filter(existing -> !Objects.equals(existing.getId(), ownerId))
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "A service with this name already exists (serviceId=" + existing.getId() + ")");
                });
    }

    private ServiceOffering findServiceOrThrow(Long serviceId) {
        return serviceOfferingRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id " + serviceId));
    }

    /** Trims optional text and converts blank values to null. */
    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
