package com.example.backend.dto.requests.workspaces;

public record UpdateWorkspaceDetailsRequest(
    String name,
    Boolean visibility,
    String description,
    String colour
) {}
