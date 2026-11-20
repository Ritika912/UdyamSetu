package com.example.udyamsetubackend.common.exception;

import com.example.udyamsetubackend.customer.controller.CustomerController;
import com.example.udyamsetubackend.customer.service.CustomerService;
import com.example.udyamsetubackend.enquiry.controller.EnquiryController;
import com.example.udyamsetubackend.enquiry.service.EnquiryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Non-numeric path ids on existing endpoints must return 400 in the standard error format, not 500.
 * Enquiry endpoints take no path or query parameters, so their existing body-parsing 400 is checked
 * to be unchanged.
 */
@WebMvcTest({CustomerController.class, EnquiryController.class})
// Controller behaviour only. Security rules are covered by AuthSecurityIntegrationTest.
@AutoConfigureMockMvc(addFilters = false)
class TypeMismatchHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private EnquiryService enquiryService;

    @Test
    void customerGetWithNonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/customers/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("'id'")));
    }

    @Test
    void customerPutWithNonNumericIdReturns400() throws Exception {
        mockMvc.perform(put("/api/customers/abc").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"X\",\"contactNumber\":\"9876543210\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerDeleteWithNonNumericIdReturns400() throws Exception {
        mockMvc.perform(delete("/api/customers/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerHistoryWithNonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/customers/abc/enquiries"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enquiryMalformedBodyStillReturns400WithItsExistingMessage() throws Exception {
        mockMvc.perform(post("/api/enquiries").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"X\",\"contactNumber\":\"9876543210\",\"source\":\"FAX\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("WHATSAPP")));
    }
}
