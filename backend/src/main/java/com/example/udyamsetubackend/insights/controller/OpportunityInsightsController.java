package com.example.udyamsetubackend.insights.controller;

import com.example.udyamsetubackend.insights.dto.OpportunityInsightsResponse;
import com.example.udyamsetubackend.insights.enums.InsightPeriod;
import com.example.udyamsetubackend.insights.service.OpportunityInsightsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
public class OpportunityInsightsController {

    private final OpportunityInsightsService opportunityInsightsService;

    public OpportunityInsightsController(OpportunityInsightsService opportunityInsightsService) {
        this.opportunityInsightsService = opportunityInsightsService;
    }

    /** {@code period} is LAST_30_DAYS (default) or ALL_TIME. Any other value returns 400. */
    @GetMapping("/opportunities")
    public OpportunityInsightsResponse getOpportunities(
            @RequestParam(defaultValue = "LAST_30_DAYS") InsightPeriod period) {
        return opportunityInsightsService.getOpportunities(period);
    }
}
