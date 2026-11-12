package com.example.backend.dto.responses.users;

import com.example.backend.model.users.User;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UserOverviewDTO(
    @JsonProperty("user_id") long userId,
    @JsonProperty("username") String username,
    @JsonProperty("user_colour") String userColour
) {
    public static UserOverviewDTO fromEntity(User user) {
        return new UserOverviewDTO(
            user.getId(),
            user.getUsername(),
            user.getColour()
        );
    }
}
