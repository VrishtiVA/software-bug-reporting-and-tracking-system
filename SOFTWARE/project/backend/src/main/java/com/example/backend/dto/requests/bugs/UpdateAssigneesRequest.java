package com.example.backend.dto.requests.bugs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record UpdateAssigneesRequest(
    @JsonProperty("assign") List<Long> usersToAssign,
    @JsonProperty("unassign") List<Long> usersToUnassign
) {}
