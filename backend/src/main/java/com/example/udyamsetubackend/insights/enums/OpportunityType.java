package com.example.udyamsetubackend.insights.enums;

/**
 * Kind of opportunity. Each type belongs to one {@link OpportunityCategory}.
 */
public enum OpportunityType {
    REPEATED_UNFULFILLED(OpportunityCategory.SUPPLY_GAP),
    FREQUENTLY_REQUESTED(OpportunityCategory.DEMAND_PROMOTION),
    HIGH_LOST_RATE(OpportunityCategory.CONVERSION_RISK),
    INCREASING_DEMAND(OpportunityCategory.GROWTH);

    private final OpportunityCategory category;

    OpportunityType(OpportunityCategory category) {
        this.category = category;
    }

    public OpportunityCategory category() {
        return category;
    }
}
