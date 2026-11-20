package com.example.udyamsetubackend.enquiry.controller;

import com.example.udyamsetubackend.catalog.repository.ServiceOfferingRepository;
import com.example.udyamsetubackend.catalog.service.ServiceOfferingService;
import com.example.udyamsetubackend.customer.repository.CustomerRepository;
import com.example.udyamsetubackend.customer.entity.Customer;
import com.example.udyamsetubackend.customer.service.CustomerService;
import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import com.example.udyamsetubackend.enquiry.entity.Enquiry;
import com.example.udyamsetubackend.enquiry.service.EnquiryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnquiryController.class)
// Controller behaviour only. Security rules are covered by AuthSecurityIntegrationTest.
@AutoConfigureMockMvc(addFilters = false)
@Import({EnquiryService.class, CustomerService.class, ServiceOfferingService.class})
class EnquiryControllerTest {

    private static final String FULL_PAYLOAD = """
            {
              "customerName": "Priya Sharma",
              "contactNumber": "9876543210",
              "source": "WHATSAPP",
              "requestedService": "Nail Extensions",
              "requirements": "Interested in gel nail extensions",
              "occasion": "Wedding",
              "budget": 2500.00,
              "expectedDate": "2026-12-15",
              "followUpDate": "2026-11-20",
              "followUpTime": "15:30:00",
              "followUpNotes": "Call after 3 PM"
            }
            """;

    private static final String MINIMAL_PAYLOAD = """
            {
              "customerName": "Priya Sharma",
              "contactNumber": "9876543210"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnquiryRepository enquiryRepository;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private ServiceOfferingRepository serviceOfferingRepository;

    @BeforeEach
    void savesReturnTheEntityAsIs() {
        // New customers are not found by contact, so the enquiry flow creates one.
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsEnquiryWithAllFieldsAndReturns201() throws Exception {
        when(enquiryRepository.save(any(Enquiry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FULL_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Priya Sharma"))
                .andExpect(jsonPath("$.contactNumber").value("9876543210"))
                .andExpect(jsonPath("$.source").value("WHATSAPP"))
                .andExpect(jsonPath("$.budget").value(2500.00))
                .andExpect(jsonPath("$.expectedDate").value("2026-12-15"))
                .andExpect(jsonPath("$.followUpTime").value("15:30:00"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void createsEnquiryWithMinimalPayloadAndNullOptionalFieldsReturns201() throws Exception {
        when(enquiryRepository.save(any(Enquiry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MINIMAL_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Priya Sharma"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.source").value(nullValue()))
                .andExpect(jsonPath("$.budget").value(nullValue()))
                .andExpect(jsonPath("$.followUpNotes").value(nullValue()));
    }

    @Test
    void rejectsMissingCustomerNameWith400() throws Exception {
        String payload = """
                { "contactNumber": "9876543210" }
                """;

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.customerName").value("customerName is required"));

        verify(enquiryRepository, never()).save(any(Enquiry.class));
    }

    @Test
    void rejectsMissingContactNumberWith400() throws Exception {
        String payload = """
                { "customerName": "Priya Sharma" }
                """;

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.contactNumber").value("contactNumber is required"));

        verify(enquiryRepository, never()).save(any(Enquiry.class));
    }

    @Test
    void rejectsBlankRequiredFieldsWith400() throws Exception {
        String payload = """
                { "customerName": "   ", "contactNumber": "" }
                """;

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.customerName").exists())
                .andExpect(jsonPath("$.fieldErrors.contactNumber").exists());
    }

    @Test
    void rejectsContactNumberWithLettersOrTooManyDigitsWith400() throws Exception {
        String payload = """
                { "customerName": "Priya Sharma", "contactNumber": "98765-ABCDE12345678" }
                """;

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.contactNumber",
                        containsString("7 to 15 digits")));
    }

    @Test
    void rejectsNegativeBudgetWith400() throws Exception {
        String payload = """
                { "customerName": "Priya Sharma", "contactNumber": "9876543210", "budget": -10 }
                """;

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.budget").value("budget must not be negative"));
    }

    @Test
    void rejectsUnknownSourceValueWith400WithAllowedValues() throws Exception {
        String payload = """
                { "customerName": "Priya Sharma", "contactNumber": "9876543210", "source": "FAX" }
                """;

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("WHATSAPP")));

        verify(enquiryRepository, never()).save(any(Enquiry.class));
    }

    @Test
    void unknownServiceIdReturns400WithFieldErrorOnServiceId() throws Exception {
        when(serviceOfferingRepository.findById(404L)).thenReturn(java.util.Optional.empty());
        String payload = """
                { "customerName": "Priya Sharma", "contactNumber": "9876543210", "serviceId": 404 }
                """;

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.serviceId")
                        .value("serviceId does not refer to an existing service"));

        verify(enquiryRepository, never()).save(any(Enquiry.class));
    }

    @Test
    void inactiveServiceIdReturns400WithFieldErrorOnServiceId() throws Exception {
        com.example.udyamsetubackend.catalog.entity.ServiceOffering inactive =
                new com.example.udyamsetubackend.catalog.entity.ServiceOffering("Seasonal Pack");
        inactive.setActive(false);
        when(serviceOfferingRepository.findById(7L)).thenReturn(java.util.Optional.of(inactive));
        String payload = """
                { "customerName": "Priya Sharma", "contactNumber": "9876543210", "serviceId": 7 }
                """;

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.serviceId").value("serviceId refers to an inactive service"));

        verify(enquiryRepository, never()).save(any(Enquiry.class));
    }

    @Test
    void nonPositiveServiceIdReturns400() throws Exception {
        String payload = """
                { "customerName": "Priya Sharma", "contactNumber": "9876543210", "serviceId": 0 }
                """;

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.serviceId").value("serviceId must be a positive number"));
    }

    @Test
    void rejectsMalformedDateWith400() throws Exception {
        String payload = """
                { "customerName": "Priya Sharma", "contactNumber": "9876543210", "expectedDate": "15-12-2026" }
                """;

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(enquiryRepository, never()).save(any(Enquiry.class));
    }
}
