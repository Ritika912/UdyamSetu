package com.example.udyamsetubackend.insights.controller;

import com.example.udyamsetubackend.insights.enums.InsightPeriod;
import com.example.udyamsetubackend.insights.service.BusinessInsightsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BusinessInsightsController.class)
// Controller behaviour only. Security rules are covered by AuthSecurityIntegrationTest.
@AutoConfigureMockMvc(addFilters = false)
class BusinessInsightsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BusinessInsightsService businessInsightsService;

    @Test
    void defaultPeriodIsLast30Days() throws Exception {
        mockMvc.perform(get("/api/insights/business")).andExpect(status().isOk());

        verify(businessInsightsService).getInsights(eq(InsightPeriod.LAST_30_DAYS));
    }

    @Test
    void allTimePeriodIsPassedThrough() throws Exception {
        mockMvc.perform(get("/api/insights/business").param("period", "ALL_TIME")).andExpect(status().isOk());

        verify(businessInsightsService).getInsights(eq(InsightPeriod.ALL_TIME));
    }

    @Test
    void unknownPeriodReturns400AndDoesNotCallService() throws Exception {
        mockMvc.perform(get("/api/insights/business").param("period", "LAST_WEEK"))
                .andExpect(status().isBadRequest());

        verify(businessInsightsService, never()).getInsights(any());
    }
}
