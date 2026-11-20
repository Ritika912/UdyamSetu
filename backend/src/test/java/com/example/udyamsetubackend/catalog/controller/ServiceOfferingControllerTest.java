package com.example.udyamsetubackend.catalog.controller;

import com.example.udyamsetubackend.catalog.dto.ServiceRequest;
import com.example.udyamsetubackend.catalog.dto.ServiceResponse;
import com.example.udyamsetubackend.catalog.service.ServiceOfferingService;
import com.example.udyamsetubackend.common.exception.ConflictException;
import com.example.udyamsetubackend.common.exception.ResourceNotFoundException;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceOfferingController.class)
// Controller behaviour only. Security rules are covered by AuthSecurityIntegrationTest.
@AutoConfigureMockMvc(addFilters = false)
class ServiceOfferingControllerTest {

    private static final String VALID_PAYLOAD = """
            { "name": "Bridal Makeup", "description": "Full look", "category": "Makeup",
              "price": 4500.00, "isActive": true }
            """;

    private static final ServiceResponse SAVED = new ServiceResponse(
            1L, "Bridal Makeup", "Full look", "Makeup", new java.math.BigDecimal("4500.00"), true,
            LocalDateTime.of(2026, 11, 17, 18, 0), LocalDateTime.of(2026, 11, 17, 18, 0));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceOfferingService serviceOfferingService;

    // ---------- POST ----------

    @Test
    void createReturns201WithServiceBody() throws Exception {
        when(serviceOfferingService.createService(any(ServiceRequest.class))).thenReturn(SAVED);

        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content(VALID_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Bridal Makeup"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void createWithMissingNameReturns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"price\": 100 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("name is required"));

        verify(serviceOfferingService, never()).createService(any(ServiceRequest.class));
    }

    @Test
    void createWithNegativePriceReturns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Facial\", \"price\": -5 }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").value("price must not be negative"));
    }

    @Test
    void createWithDuplicateNameReturns409() throws Exception {
        when(serviceOfferingService.createService(any(ServiceRequest.class)))
                .thenThrow(new ConflictException("A service with this name already exists (serviceId=7)"));

        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content(VALID_PAYLOAD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("serviceId=7")));
    }

    // ---------- GET list ----------

    @Test
    void listWithoutFiltersPassesNulls() throws Exception {
        when(serviceOfferingService.listServices(isNull(), isNull())).thenReturn(List.of(SAVED));

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bridal Makeup"));
    }

    @Test
    void listPassesActiveAndNameFilters() throws Exception {
        when(serviceOfferingService.listServices(true, "bridal")).thenReturn(List.of(SAVED));

        mockMvc.perform(get("/api/services").param("active", "true").param("name", "bridal"))
                .andExpect(status().isOk());

        verify(serviceOfferingService).listServices(true, "bridal");
    }

    @Test
    void listWithInvalidActiveValueReturns400() throws Exception {
        mockMvc.perform(get("/api/services").param("active", "maybe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("'active'")));

        verify(serviceOfferingService, never()).listServices(any(), any());
    }

    // ---------- GET by id ----------

    @Test
    void getReturns200() throws Exception {
        when(serviceOfferingService.getService(1L)).thenReturn(SAVED);

        mockMvc.perform(get("/api/services/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getMissingReturns404() throws Exception {
        when(serviceOfferingService.getService(anyLong()))
                .thenThrow(new ResourceNotFoundException("Service not found with id 99"));

        mockMvc.perform(get("/api/services/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Service not found with id 99"));
    }

    @Test
    void getWithNonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/services/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("'id'")));
    }

    // ---------- PUT ----------

    @Test
    void updateReturns200() throws Exception {
        when(serviceOfferingService.updateService(anyLong(), any(ServiceRequest.class))).thenReturn(SAVED);

        mockMvc.perform(put("/api/services/1").contentType(MediaType.APPLICATION_JSON).content(VALID_PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateWithDuplicateNameReturns409() throws Exception {
        when(serviceOfferingService.updateService(anyLong(), any(ServiceRequest.class)))
                .thenThrow(new ConflictException("A service with this name already exists (serviceId=9)"));

        mockMvc.perform(put("/api/services/1").contentType(MediaType.APPLICATION_JSON).content(VALID_PAYLOAD))
                .andExpect(status().isConflict());
    }

    @Test
    void updateMissingReturns404() throws Exception {
        when(serviceOfferingService.updateService(anyLong(), any(ServiceRequest.class)))
                .thenThrow(new ResourceNotFoundException("Service not found with id 99"));

        mockMvc.perform(put("/api/services/99").contentType(MediaType.APPLICATION_JSON).content(VALID_PAYLOAD))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateWithInvalidBodyReturns400() throws Exception {
        mockMvc.perform(put("/api/services/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void updateWithNonNumericIdReturns400() throws Exception {
        mockMvc.perform(put("/api/services/abc").contentType(MediaType.APPLICATION_JSON).content(VALID_PAYLOAD))
                .andExpect(status().isBadRequest());
    }

    // ---------- DELETE ----------

    @Test
    void deleteReturns204AndDeactivates() throws Exception {
        mockMvc.perform(delete("/api/services/1"))
                .andExpect(status().isNoContent());

        verify(serviceOfferingService).deactivateService(1L);
    }

    @Test
    void deleteMissingReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("Service not found with id 99"))
                .when(serviceOfferingService).deactivateService(99L);

        mockMvc.perform(delete("/api/services/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteWithNonNumericIdReturns400() throws Exception {
        mockMvc.perform(delete("/api/services/abc"))
                .andExpect(status().isBadRequest());
    }
}
