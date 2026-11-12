package com.example.backend.dto.responses.workspaces.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PriorityFrequencyBugCountBucketsDTO(
        @JsonProperty("priority_bucket_lb") Integer priorityBucketLB,
        @JsonProperty("frequency_bucket_lb") Integer frequencyBucketLB,
        @JsonProperty("bug_count") Integer bugCount
) {
    public static PriorityFrequencyBugCountBucketsDTO fromData(Integer priorityBucketLB, Integer frequencyBucketLB, Integer bugCount) {
        return new PriorityFrequencyBugCountBucketsDTO(priorityBucketLB, frequencyBucketLB, bugCount);
    }
}
