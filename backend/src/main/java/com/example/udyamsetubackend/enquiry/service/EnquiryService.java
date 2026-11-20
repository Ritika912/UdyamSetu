package com.example.udyamsetubackend.enquiry.service;

import com.example.udyamsetubackend.catalog.entity.ServiceOffering;
import com.example.udyamsetubackend.catalog.service.ServiceOfferingService;
import com.example.udyamsetubackend.common.exception.ResourceNotFoundException;
import com.example.udyamsetubackend.customer.entity.Customer;
import com.example.udyamsetubackend.customer.service.CustomerService;
import com.example.udyamsetubackend.enquiry.dto.EnquiryRequest;
import com.example.udyamsetubackend.enquiry.dto.EnquiryResponse;
import com.example.udyamsetubackend.enquiry.entity.Enquiry;
import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final CustomerService customerService;
    private final ServiceOfferingService serviceOfferingService;

    public EnquiryService(EnquiryRepository enquiryRepository,
                          CustomerService customerService,
                          ServiceOfferingService serviceOfferingService) {
        this.enquiryRepository = enquiryRepository;
        this.customerService = customerService;
        this.serviceOfferingService = serviceOfferingService;
    }

    @Transactional
    public EnquiryResponse createEnquiry(EnquiryRequest request) {
        // Validate the catalogue reference before any customer is created, so a rejected request
        // leaves no partial data behind.
        ServiceOffering catalogueService = request.serviceId() == null
                ? null
                : serviceOfferingService.getActiveServiceForEnquiry(request.serviceId());

        String customerName = request.customerName().trim();
        String contactNumber = request.contactNumber().trim();

        Customer customer = customerService.resolveCustomer(customerName, contactNumber);
        Enquiry enquiry = new Enquiry(customer, customerName, contactNumber);

        enquiry.setService(catalogueService);
        enquiry.setRequestedService(resolveRequestedService(request.requestedService(), catalogueService));
        enquiry.setSource(request.source());
        enquiry.setRequirements(normalizeOptionalText(request.requirements()));
        enquiry.setOccasion(normalizeOptionalText(request.occasion()));
        enquiry.setBudget(request.budget());
        enquiry.setExpectedDate(request.expectedDate());
        enquiry.setFollowUpDate(request.followUpDate());
        enquiry.setFollowUpTime(request.followUpTime());
        enquiry.setFollowUpNotes(normalizeOptionalText(request.followUpNotes()));

        return EnquiryResponse.from(enquiryRepository.save(enquiry));
    }

    /**
     * Lists enquiries, newest first. Both filters are optional; a null filter matches everything.
     */
    @Transactional(readOnly = true)
    public List<EnquiryResponse> listEnquiries(EnquiryStatus status, Long customerId) {
        return enquiryRepository.findAll(
                        EnquirySpecifications.matching(status, customerId),
                        Sort.by(Sort.Direction.DESC, "createdAt", "id"))
                .stream()
                .map(EnquiryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnquiryResponse getEnquiry(Long enquiryId) {
        return EnquiryResponse.from(findEnquiryOrThrow(enquiryId));
    }

    /**
     * Sets the status of an enquiry. Any status may be set; no transition rules are enforced yet.
     */
    @Transactional
    public EnquiryResponse updateStatus(Long enquiryId, EnquiryStatus status) {
        Enquiry enquiry = findEnquiryOrThrow(enquiryId);
        enquiry.setStatus(status);
        return EnquiryResponse.from(enquiryRepository.saveAndFlush(enquiry));
    }

    private Enquiry findEnquiryOrThrow(Long enquiryId) {
        return enquiryRepository.findById(enquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found with id " + enquiryId));
    }

    /**
     * The requested-service text is a historical snapshot. Custom text supplied by the owner is kept as
     * typed. If none is supplied, the selected catalogue service's name is used. Otherwise it stays null.
     */
    private static String resolveRequestedService(String customText, ServiceOffering catalogueService) {
        String text = normalizeOptionalText(customText);
        if (text != null) {
            return text;
        }
        return catalogueService == null ? null : catalogueService.getName();
    }

    /**
     * Trims optional text and converts blank values to null so that "" and "   " are stored as absent.
     */
    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
