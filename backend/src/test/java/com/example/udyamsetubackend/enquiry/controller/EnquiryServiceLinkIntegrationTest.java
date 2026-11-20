package com.example.udyamsetubackend.enquiry.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Enquiry-to-catalogue integration on the real stack and udyamsetu_test (enforced by the test guard).
 * Each test is rolled back.
 */
@SpringBootTest
// Endpoint behaviour only. Security rules are covered by AuthSecurityIntegrationTest.
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class EnquiryServiceLinkIntegrationTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    private long createCatalogueService(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/services").contentType(JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private MvcResult createEnquiry(String body) throws Exception {
        return mockMvc.perform(post("/api/enquiries").contentType(JSON).content(body)).andReturn();
    }

    @Test
    void enquiryForCatalogueServiceStoresLinkAndNameSnapshot() throws Exception {
        long serviceId = createCatalogueService("ZZLINK Nail Extensions");

        mockMvc.perform(post("/api/enquiries").contentType(JSON).content(
                        "{\"customerName\":\"Priya Sharma\",\"contactNumber\":\"9876543210\",\"serviceId\":" + serviceId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceId").value(serviceId))
                .andExpect(jsonPath("$.requestedService").value("ZZLINK Nail Extensions"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void enquiryWithoutServiceIdBehavesAsBefore() throws Exception {
        mockMvc.perform(post("/api/enquiries").contentType(JSON).content(
                        "{\"customerName\":\"Priya Sharma\",\"contactNumber\":\"9876543210\",\"requestedService\":\"Custom gown\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceId").value(nullValue()))
                .andExpect(jsonPath("$.requestedService").value("Custom gown"));
    }

    @Test
    void customRequestedTextIsKeptWhenServiceIdIsAlsoGiven() throws Exception {
        long serviceId = createCatalogueService("ZZLINK Haircut");

        mockMvc.perform(post("/api/enquiries").contentType(JSON).content(
                        "{\"customerName\":\"Priya Sharma\",\"contactNumber\":\"9876543210\","
                                + "\"requestedService\":\"Haircut with colour\",\"serviceId\":" + serviceId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceId").value(serviceId))
                .andExpect(jsonPath("$.requestedService").value("Haircut with colour"));
    }

    @Test
    void unknownServiceIdReturns400AndCreatesNoCustomer() throws Exception {
        mockMvc.perform(post("/api/enquiries").contentType(JSON).content(
                        "{\"customerName\":\"ZZLINK Ghost\",\"contactNumber\":\"9000000001\",\"serviceId\":999999999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.serviceId").value("serviceId does not refer to an existing service"));

        // The rejected request must not leave a customer behind.
        mockMvc.perform(get("/api/customers"))
                .andExpect(jsonPath("$[?(@.contactNumber == '9000000001')]", hasSize(0)));
    }

    @Test
    void inactiveServiceIsRejectedButHistoricalEnquiriesKeepTheirLink() throws Exception {
        long serviceId = createCatalogueService("ZZLINK Seasonal Pack");

        MvcResult historical = createEnquiry(
                "{\"customerName\":\"Priya Sharma\",\"contactNumber\":\"9876543210\",\"serviceId\":" + serviceId + "}");
        long customerId = ((Number) JsonPath.read(historical.getResponse().getContentAsString(), "$.customerId")).longValue();

        mockMvc.perform(delete("/api/services/" + serviceId)).andExpect(status().isNoContent());

        // New enquiries cannot select the deactivated service.
        mockMvc.perform(post("/api/enquiries").contentType(JSON).content(
                        "{\"customerName\":\"Priya Sharma\",\"contactNumber\":\"9876543210\",\"serviceId\":" + serviceId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.serviceId").value("serviceId refers to an inactive service"));

        // The historical enquiry keeps its service link and name snapshot.
        mockMvc.perform(get("/api/customers/" + customerId + "/enquiries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].serviceId").value(serviceId))
                .andExpect(jsonPath("$[0].requestedService").value("ZZLINK Seasonal Pack"));
    }

    @Test
    void nonPositiveServiceIdReturns400() throws Exception {
        mockMvc.perform(post("/api/enquiries").contentType(JSON).content(
                        "{\"customerName\":\"Priya Sharma\",\"contactNumber\":\"9876543210\",\"serviceId\":-3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.serviceId", containsString("positive")));
    }
}
