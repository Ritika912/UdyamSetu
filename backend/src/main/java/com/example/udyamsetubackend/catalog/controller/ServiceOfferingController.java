package com.example.udyamsetubackend.catalog.controller;

import com.example.udyamsetubackend.catalog.dto.ServiceRequest;
import com.example.udyamsetubackend.catalog.dto.ServiceResponse;
import com.example.udyamsetubackend.catalog.service.ServiceOfferingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceOfferingController {

    private final ServiceOfferingService serviceOfferingService;

    public ServiceOfferingController(ServiceOfferingService serviceOfferingService) {
        this.serviceOfferingService = serviceOfferingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse createService(@Valid @RequestBody ServiceRequest request) {
        return serviceOfferingService.createService(request);
    }

    @GetMapping
    public List<ServiceResponse> listServices(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String name) {
        return serviceOfferingService.listServices(active, name);
    }

    @GetMapping("/{id}")
    public ServiceResponse getService(@PathVariable Long id) {
        return serviceOfferingService.getService(id);
    }

    @PutMapping("/{id}")
    public ServiceResponse updateService(@PathVariable Long id, @Valid @RequestBody ServiceRequest request) {
        return serviceOfferingService.updateService(id, request);
    }

    /** Soft deactivation: sets isActive=false. Nothing is deleted. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateService(@PathVariable Long id) {
        serviceOfferingService.deactivateService(id);
    }
}
