package com.example.udyamsetubackend.customer.service;

import com.example.udyamsetubackend.common.contact.ContactNumberNormalizer;
import com.example.udyamsetubackend.common.exception.ConflictException;
import com.example.udyamsetubackend.common.exception.ResourceNotFoundException;
import com.example.udyamsetubackend.customer.dto.CustomerRequest;
import com.example.udyamsetubackend.customer.dto.CustomerResponse;
import com.example.udyamsetubackend.customer.entity.Customer;
import com.example.udyamsetubackend.customer.repository.CustomerRepository;
import com.example.udyamsetubackend.enquiry.dto.EnquiryResponse;
import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final EnquiryRepository enquiryRepository;

    public CustomerService(CustomerRepository customerRepository, EnquiryRepository enquiryRepository) {
        this.customerRepository = customerRepository;
        this.enquiryRepository = enquiryRepository;
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        String contactKey = ContactNumberNormalizer.normalize(request.contactNumber());
        ensureContactKeyAvailable(contactKey, null);

        Customer saved = customerRepository.save(new Customer(
                request.customerName().trim(),
                request.contactNumber().trim(),
                contactKey,
                normalizeOptionalText(request.notes())));
        return CustomerResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> listCustomers() {
        return customerRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(CustomerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long customerId) {
        return CustomerResponse.from(findCustomerOrThrow(customerId));
    }

    @Transactional
    public CustomerResponse updateCustomer(Long customerId, CustomerRequest request) {
        Customer customer = findCustomerOrThrow(customerId);
        String contactKey = ContactNumberNormalizer.normalize(request.contactNumber());
        ensureContactKeyAvailable(contactKey, customerId);

        customer.update(
                request.customerName().trim(),
                request.contactNumber().trim(),
                contactKey,
                normalizeOptionalText(request.notes()));
        return CustomerResponse.from(customerRepository.saveAndFlush(customer));
    }

    /**
     * Deletes a customer only when no enquiry refers to it, so enquiry history is never lost.
     */
    @Transactional
    public void deleteCustomer(Long customerId) {
        Customer customer = findCustomerOrThrow(customerId);
        if (enquiryRepository.existsByCustomerId(customerId)) {
            throw new ConflictException(
                    "Customer " + customerId + " has enquiries and cannot be deleted");
        }
        try {
            customerRepository.delete(customer);
            customerRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // An enquiry was linked concurrently after the check above.
            throw new ConflictException("Customer " + customerId + " has enquiries and cannot be deleted");
        }
    }

    @Transactional(readOnly = true)
    public List<EnquiryResponse> getEnquiryHistory(Long customerId) {
        findCustomerOrThrow(customerId);
        return enquiryRepository.findByCustomerIdOrderByCreatedAtDescIdDesc(customerId).stream()
                .map(EnquiryResponse::from)
                .toList();
    }

    /**
     * Returns the customer with this contact number, creating one from the given details if none exists.
     * An existing customer's name is never overwritten by an enquiry.
     */
    @Transactional
    public Customer resolveCustomer(String customerName, String contactNumber) {
        String contactKey = ContactNumberNormalizer.normalize(contactNumber);
        return customerRepository.findByContactKey(contactKey)
                .orElseGet(() -> customerRepository.save(new Customer(
                        customerName.trim(), contactNumber.trim(), contactKey, null)));
    }

    /**
     * Rejects the key if another customer already owns it. {@code ownerId} is the customer being updated,
     * or null when creating.
     */
    private void ensureContactKeyAvailable(String contactKey, Long ownerId) {
        customerRepository.findByContactKey(contactKey)
                .filter(existing -> !Objects.equals(existing.getId(), ownerId))
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "A customer with this contact number already exists (customerId="
                                    + existing.getId() + ")");
                });
    }

    private Customer findCustomerOrThrow(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + customerId));
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
