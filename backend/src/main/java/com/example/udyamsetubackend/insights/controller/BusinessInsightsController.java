package com.example.udyamsetubackend.insights.controller;

import com.example.udyamsetubackend.insights.dto.BusinessInsightsResponse;
import com.example.udyamsetubackend.insights.enums.InsightPeriod;
import com.example.udyamsetubackend.insights.service.BusinessInsightsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
public class BusinessInsightsController {

    private final BusinessInsightsService businessInsightsService;

    public BusinessInsightsController(BusinessInsightsService businessInsightsService) {
        this.businessInsightsService = businessInsightsService;
    }

    /** {@code period} is LAST_30_DAYS (default) or ALL_TIME. Any other value returns 400. */
    @GetMapping("/business")
    public BusinessInsightsResponse getBusinessInsights(
            @RequestParam(defaultValue = "LAST_30_DAYS") InsightPeriod period) {
        return businessInsightsService.getInsights(period);
    }
}
