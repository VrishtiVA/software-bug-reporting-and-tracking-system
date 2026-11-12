package com.example.backend.dto.requests.workspaces;

import java.util.List;

public record CreateWorkspaceRequest(
        String name,
        Boolean visibility,
        String description,
        String colour,
        List<CustomLabelDTO> labels, //Label "name", label "colour"
        List<MemberRoleDTO> members //Member "username", member "role"
) {}
