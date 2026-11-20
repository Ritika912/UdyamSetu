package com.example.udyamsetubackend.dashboard.controller;

import com.example.udyamsetubackend.dashboard.dto.DashboardResponse;
import com.example.udyamsetubackend.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
// Controller behaviour only. Security rules are covered by AuthSecurityIntegrationTest.
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void getDashboardReturns200WithMetrics() throws Exception {
        when(dashboardService.getDashboard())
                .thenReturn(new DashboardResponse(10L, 3L, 2L, 5L));

        mockMvc.perform(get("/api/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEnquiries").value(10))
                .andExpect(jsonPath("$.newEnquiries").value(3))
                .andExpect(jsonPath("$.followUpsDueToday").value(2))
                .andExpect(jsonPath("$.overdueFollowUps").value(5));
    }

    @Test
    void getDashboardReturnsZeroCountsWhenEmpty() throws Exception {
        when(dashboardService.getDashboard())
                .thenReturn(new DashboardResponse(0L, 0L, 0L, 0L));

        mockMvc.perform(get("/api/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEnquiries").value(0))
                .andExpect(jsonPath("$.newEnquiries").value(0))
                .andExpect(jsonPath("$.followUpsDueToday").value(0))
                .andExpect(jsonPath("$.overdueFollowUps").value(0));
    }
}
