package com.example.backend.repositories.bugs.projections;

/**
 * Projection Interface for Priority Category Distribution.
 * Defines priority category distribution with a string label and bug count.
 */
public interface PriorityCategoryDistributionProjection {
    String getPriorityLabel();
    Integer getBugCount();
}
