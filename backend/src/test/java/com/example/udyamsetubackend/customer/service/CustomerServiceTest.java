package com.example.udyamsetubackend.customer.service;

import com.example.udyamsetubackend.common.exception.ConflictException;
import com.example.udyamsetubackend.common.exception.ResourceNotFoundException;
import com.example.udyamsetubackend.customer.dto.CustomerRequest;
import com.example.udyamsetubackend.customer.dto.CustomerResponse;
import com.example.udyamsetubackend.customer.entity.Customer;
import com.example.udyamsetubackend.customer.repository.CustomerRepository;
import com.example.udyamsetubackend.enquiry.dto.EnquiryResponse;
import com.example.udyamsetubackend.enquiry.entity.Enquiry;
import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EnquiryRepository enquiryRepository;

    @InjectMocks
    private CustomerService customerService;

    @BeforeEach
    void saveReturnsEntityAsIs() {
        // Lenient: not every test saves, so this is only used where the code under test calls save.
        org.mockito.Mockito.lenient().when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(customerRepository.saveAndFlush(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static Customer persisted(long id, String name, String number, String key) {
        Customer customer = new Customer(name, number, key, null);
        ReflectionTestUtils.setField(customer, "id", id);
        return customer;
    }

    @Test
    void createStoresTrimmedDetailsWithNormalizedContactKey() {
        when(customerRepository.findByContactKey("9876543210")).thenReturn(Optional.empty());

        CustomerResponse response = customerService.createCustomer(
                new CustomerRequest("  Priya Sharma  ", " +91 98765-43210 ", "  Prefers evenings  "));

        assertEquals("Priya Sharma", response.customerName());
        assertEquals("+91 98765-43210", response.contactNumber());
        assertEquals("Prefers evenings", response.notes());
    }

    @Test
    void createWithBlankNotesStoresNull() {
        when(customerRepository.findByContactKey("9876543210")).thenReturn(Optional.empty());

        CustomerResponse response = customerService.createCustomer(
                new CustomerRequest("Priya Sharma", "9876543210", "   "));

        assertNull(response.notes());
    }

    @Test
    void createWithExistingContactKeyIsRejectedAsConflict() {
        when(customerRepository.findByContactKey("9876543210"))
                .thenReturn(Optional.of(persisted(7L, "Priya", "9876543210", "9876543210")));

        ConflictException ex = assertThrows(ConflictException.class, () ->
                customerService.createCustomer(new CustomerRequest("Someone Else", "+919876543210", null)));

        assertEquals("A customer with this contact number already exists (customerId=7)", ex.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void updateToOwnContactKeyIsAllowed() {
        Customer existing = persisted(3L, "Priya", "9876543210", "9876543210");
        when(customerRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(customerRepository.findByContactKey("9876543210")).thenReturn(Optional.of(existing));

        CustomerResponse response = customerService.updateCustomer(3L,
                new CustomerRequest("Priya S.", "9876543210", "Updated note"));

        assertEquals("Priya S.", response.customerName());
        assertEquals("Updated note", response.notes());
    }

    @Test
    void updateToContactKeyOwnedByAnotherCustomerIsConflict() {
        Customer target = persisted(3L, "Priya", "9876543210", "9876543210");
        Customer other = persisted(9L, "Ravi", "9123456789", "9123456789");
        when(customerRepository.findById(3L)).thenReturn(Optional.of(target));
        when(customerRepository.findByContactKey("9123456789")).thenReturn(Optional.of(other));

        assertThrows(ConflictException.class, () ->
                customerService.updateCustomer(3L, new CustomerRequest("Priya", "9123456789", null)));
        verify(customerRepository, never()).saveAndFlush(any(Customer.class));
    }

    @Test
    void updateMissingCustomerIsNotFound() {
        when(customerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                customerService.updateCustomer(404L, new CustomerRequest("Priya", "9876543210", null)));
    }

    @Test
    void getMissingCustomerIsNotFound() {
        when(customerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.getCustomer(404L));
    }

    @Test
    void deleteIsBlockedWhenCustomerHasEnquiries() {
        Customer customer = persisted(3L, "Priya", "9876543210", "9876543210");
        when(customerRepository.findById(3L)).thenReturn(Optional.of(customer));
        when(enquiryRepository.existsByCustomerId(3L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> customerService.deleteCustomer(3L));
        verify(customerRepository, never()).delete(any(Customer.class));
    }

    @Test
    void deleteRemovesCustomerWithoutEnquiries() {
        Customer customer = persisted(3L, "Priya", "9876543210", "9876543210");
        when(customerRepository.findById(3L)).thenReturn(Optional.of(customer));
        when(enquiryRepository.existsByCustomerId(3L)).thenReturn(false);

        customerService.deleteCustomer(3L);

        verify(customerRepository).delete(customer);
    }

    @Test
    void deleteMissingCustomerIsNotFound() {
        when(customerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.deleteCustomer(404L));
    }

    @Test
    void enquiryHistoryReturnsEnquiriesForCustomer() {
        Customer customer = persisted(3L, "Priya", "9876543210", "9876543210");
        when(customerRepository.findById(3L)).thenReturn(Optional.of(customer));
        Enquiry enquiry = new Enquiry(customer, "Priya", "9876543210");
        when(enquiryRepository.findByCustomerIdOrderByCreatedAtDescIdDesc(3L)).thenReturn(List.of(enquiry));

        List<EnquiryResponse> history = customerService.getEnquiryHistory(3L);

        assertEquals(1, history.size());
        assertEquals("Priya", history.get(0).customerName());
    }

    @Test
    void enquiryHistoryForMissingCustomerIsNotFound() {
        when(customerRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.getEnquiryHistory(404L));
        verify(enquiryRepository, never()).findByCustomerIdOrderByCreatedAtDescIdDesc(any());
    }

    @Test
    void resolveReturnsExistingCustomerAndDoesNotOverwriteItsName() {
        Customer existing = persisted(3L, "Priya Sharma", "9876543210", "9876543210");
        when(customerRepository.findByContactKey("9876543210")).thenReturn(Optional.of(existing));

        Customer resolved = customerService.resolveCustomer("Priya S", "+91 98765 43210");

        assertEquals(existing, resolved);
        assertEquals("Priya Sharma", resolved.getCustomerName());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void resolveCreatesCustomerWhenContactIsNew() {
        when(customerRepository.findByContactKey("+15551234567")).thenReturn(Optional.empty());

        Customer resolved = customerService.resolveCustomer("Jane Doe", "+1 555-123-4567");

        assertEquals("Jane Doe", resolved.getCustomerName());
        assertEquals("+15551234567", resolved.getContactKey());
        verify(customerRepository).save(resolved);
    }
}
