package com.example.udyamsetubackend.enquiry.controller;

import com.example.udyamsetubackend.catalog.repository.ServiceOfferingRepository;
import com.example.udyamsetubackend.catalog.service.ServiceOfferingService;
import com.example.udyamsetubackend.customer.entity.Customer;
import com.example.udyamsetubackend.customer.repository.CustomerRepository;
import com.example.udyamsetubackend.customer.service.CustomerService;
import com.example.udyamsetubackend.enquiry.entity.Enquiry;
import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import com.example.udyamsetubackend.enquiry.service.EnquiryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract for the enquiry list, detail and status endpoints. Repositories are mocked.
 */
@WebMvcTest(EnquiryController.class)
// Controller behaviour only. Security rules are covered by AuthSecurityIntegrationTest.
@AutoConfigureMockMvc(addFilters = false)
@Import({EnquiryService.class, CustomerService.class, ServiceOfferingService.class})
class EnquiryQueryControllerTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnquiryRepository enquiryRepository;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private ServiceOfferingRepository serviceOfferingRepository;

    private Enquiry enquiry(EnquiryStatus status) {
        Enquiry enquiry = new Enquiry(new Customer("Priya Sharma", "9876543210", "9876543210", null),
                "Priya Sharma", "9876543210");
        enquiry.setStatus(status);
        return enquiry;
    }

    @Test
    void listReturnsEnquiriesAsJsonArray() throws Exception {
        when(enquiryRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(enquiry(EnquiryStatus.NEW)));

        mockMvc.perform(get("/api/enquiries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    void listAcceptsStatusAndCustomerIdFilters() throws Exception {
        when(enquiryRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/enquiries").param("status", "UNFULFILLED").param("customerId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));

        verify(enquiryRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void listWithUnknownStatusReturns400() throws Exception {
        mockMvc.perform(get("/api/enquiries").param("status", "CLOSED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("status")));
    }

    @Test
    void getReturnsEnquiryById() throws Exception {
        when(enquiryRepository.findById(5L)).thenReturn(Optional.of(enquiry(EnquiryStatus.IN_PROGRESS)));

        mockMvc.perform(get("/api/enquiries/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.customerName").value("Priya Sharma"));
    }

    @Test
    void getUnknownIdReturns404() throws Exception {
        when(enquiryRepository.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/enquiries/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Enquiry not found with id 404"));
    }

    @Test
    void patchStatusUpdatesAndReturnsEnquiry() throws Exception {
        when(enquiryRepository.findById(5L)).thenReturn(Optional.of(enquiry(EnquiryStatus.NEW)));
        when(enquiryRepository.saveAndFlush(any(Enquiry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/api/enquiries/5/status").contentType(JSON)
                        .content("{\"status\":\"CONVERTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONVERTED"));
    }

    @Test
    void patchWithoutStatusReturns400() throws Exception {
        mockMvc.perform(patch("/api/enquiries/5/status").contentType(JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").value("status is required"));
    }

    @Test
    void patchWithUnknownStatusReturns400() throws Exception {
        mockMvc.perform(patch("/api/enquiries/5/status").contentType(JSON).content("{\"status\":\"DONE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchUnknownIdReturns404() throws Exception {
        when(enquiryRepository.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/enquiries/404/status").contentType(JSON).content("{\"status\":\"LOST\"}"))
                .andExpect(status().isNotFound());
    }
}
