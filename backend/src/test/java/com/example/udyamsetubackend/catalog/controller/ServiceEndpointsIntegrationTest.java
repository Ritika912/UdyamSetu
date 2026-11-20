package com.example.udyamsetubackend.catalog.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks of the service endpoints through the real stack and database (udyamsetu_test,
 * enforced by the test guard). Each test is rolled back, so nothing persists.
 */
@SpringBootTest
// Endpoint behaviour only. Security rules are covered by AuthSecurityIntegrationTest.
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ServiceEndpointsIntegrationTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    private long createService(String name, Boolean isActive) throws Exception {
        String body = "{\"name\":\"" + name + "\"" + (isActive == null ? "" : ",\"isActive\":" + isActive) + "}";
        MvcResult result = mockMvc.perform(post("/api/services").contentType(JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return Long.parseLong(json.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Test
    void createReturns201WithDefaultsAndTimestamps() throws Exception {
        mockMvc.perform(post("/api/services").contentType(JSON)
                        .content("{\"name\":\"ZZIT Haircut\",\"price\":300.00,\"category\":\"  Hair  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("ZZIT Haircut"))
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.category").value("Hair"))
                .andExpect(jsonPath("$.price").value(300.00))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void createDuplicateNameIgnoringCaseReturns409() throws Exception {
        createService("ZZIT Facial", null);

        mockMvc.perform(post("/api/services").contentType(JSON).content("{\"name\":\"zzit facial\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("serviceId=")));
    }

    @Test
    void createWithNegativePriceReturns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/services").contentType(JSON)
                        .content("{\"name\":\"ZZIT Bad\",\"price\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").value("price must not be negative"));
    }

    @Test
    void getReturnsServiceAndMissingReturns404() throws Exception {
        long id = createService("ZZIT Manicure", null);

        mockMvc.perform(get("/api/services/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ZZIT Manicure"));

        mockMvc.perform(get("/api/services/999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listFiltersByActiveAndPartialNameCaseInsensitively() throws Exception {
        createService("ZZIT Nail Extensions", true);
        createService("ZZIT Hair Spa", false);

        mockMvc.perform(get("/api/services").param("name", "ZZIT NAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("ZZIT Nail Extensions"));

        mockMvc.perform(get("/api/services").param("active", "false").param("name", "ZZIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("ZZIT Hair Spa"));
    }

    @Test
    void listWithInvalidActiveValueReturns400() throws Exception {
        mockMvc.perform(get("/api/services").param("active", "maybe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("'active'")));
    }

    @Test
    void updateReplacesDetailsAndReactivates() throws Exception {
        long id = createService("ZZIT Pedicure", false);

        mockMvc.perform(put("/api/services/" + id).contentType(JSON)
                        .content("{\"name\":\"ZZIT Pedicure Deluxe\",\"price\":700,\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ZZIT Pedicure Deluxe"))
                .andExpect(jsonPath("$.price").value(700))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void updateWithAnotherServicesNameReturns409() throws Exception {
        createService("ZZIT Threading", null);
        long id = createService("ZZIT Waxing", null);

        mockMvc.perform(put("/api/services/" + id).contentType(JSON)
                        .content("{\"name\":\"zzit threading\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateMissingServiceReturns404() throws Exception {
        mockMvc.perform(put("/api/services/999999999").contentType(JSON)
                        .content("{\"name\":\"ZZIT Ghost\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDeactivatesWithoutDeletingAndIsRepeatable() throws Exception {
        long id = createService("ZZIT Bridal Makeup", null);

        mockMvc.perform(delete("/api/services/" + id))
                .andExpect(status().isNoContent());

        // Still retrievable, now inactive: nothing was physically deleted.
        mockMvc.perform(get("/api/services/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // Repeating the deactivation is still a 204.
        mockMvc.perform(delete("/api/services/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMissingServiceReturns404() throws Exception {
        mockMvc.perform(delete("/api/services/999999999"))
                .andExpect(status().isNotFound());
    }
}
