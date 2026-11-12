package com.example.backend.dto.requests.users;

public record SignupRequest(
        String email,
        String username,
        String password
) {}
