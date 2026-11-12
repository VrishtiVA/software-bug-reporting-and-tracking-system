package com.example.backend.repositories.bugs.projections;

public interface PriorityFrequencyBugCountBucketProjection {
    Integer getPriorityBucketLB();
    Integer getFrequencyBucketLB();
    Integer getBugCount();
}
