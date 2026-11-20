package com.example.udyamsetubackend.insights.enums;

/**
 * Broad business area an opportunity belongs to.
 */
public enum OpportunityCategory {
    /** Demand the business could not meet. */
    SUPPLY_GAP,
    /** Demand that is frequent and worth promoting. */
    DEMAND_PROMOTION,
    /** Customers who stopped engaging. This is not unmet service demand. */
    CONVERSION_RISK,
    /** Demand that is rising. */
    GROWTH
}
