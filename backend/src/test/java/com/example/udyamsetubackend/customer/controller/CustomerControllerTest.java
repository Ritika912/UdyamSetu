package com.example.udyamsetubackend.customer.controller;

import com.example.udyamsetubackend.common.exception.ConflictException;
import com.example.udyamsetubackend.common.exception.ResourceNotFoundException;
import com.example.udyamsetubackend.customer.dto.CustomerRequest;
import com.example.udyamsetubackend.customer.dto.CustomerResponse;
import com.example.udyamsetubackend.customer.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
// Controller behaviour only. Security rules are covered by AuthSecurityIntegrationTest.
@AutoConfigureMockMvc(addFilters = false)
class CustomerControllerTest {

    private static final String VALID_PAYLOAD = """
            { "customerName": "Priya Sharma", "contactNumber": "9876543210", "notes": "Prefers evenings" }
            """;

    private static final CustomerResponse SAVED = new CustomerResponse(
            1L, "Priya Sharma", "9876543210", "Prefers evenings",
            LocalDateTime.of(2026, 11, 17, 18, 0), LocalDateTime.of(2026, 11, 17, 18, 0));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void createReturns201WithCustomer() throws Exception {
        when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(SAVED);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerName").value("Priya Sharma"))
                .andExpect(jsonPath("$.notes").value("Prefers evenings"));
    }

    @Test
    void createWithExistingContactReturns409() throws Exception {
        when(customerService.createCustomer(any(CustomerRequest.class)))
                .thenThrow(new ConflictException("A customer with this contact number already exists (customerId=1)"));

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("customerId=1")));
    }

    @Test
    void createWithInvalidContactNumberReturns400WithoutCallingService() throws Exception {
        String payload = """
                { "customerName": "Priya Sharma", "contactNumber": "12345" }
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.contactNumber", containsString("Indian mobile")));

        verify(customerService, never()).createCustomer(any(CustomerRequest.class));
    }

    @Test
    void createWithMissingNameReturns400() throws Exception {
        String payload = """
                { "contactNumber": "9876543210" }
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.customerName").value("customerName is required"));
    }

    @Test
    void listReturns200() throws Exception {
        when(customerService.listCustomers()).thenReturn(List.of(SAVED));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contactNumber").value("9876543210"));
    }

    @Test
    void getMissingCustomerReturns404() throws Exception {
        when(customerService.getCustomer(anyLong()))
                .thenThrow(new ResourceNotFoundException("Customer not found with id 99"));

        mockMvc.perform(get("/api/customers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found with id 99"));
    }

    @Test
    void updateReturns200() throws Exception {
        when(customerService.updateCustomer(anyLong(), any(CustomerRequest.class))).thenReturn(SAVED);

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/customers/1"))
                .andExpect(status().isNoContent());

        verify(customerService).deleteCustomer(1L);
    }

    @Test
    void deleteWithEnquiriesReturns409() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictException("Customer 1 has enquiries and cannot be deleted"))
                .when(customerService).deleteCustomer(1L);

        mockMvc.perform(delete("/api/customers/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("has enquiries")));
    }

    @Test
    void enquiryHistoryReturns200() throws Exception {
        when(customerService.getEnquiryHistory(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/customers/1/enquiries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
