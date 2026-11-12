package com.example.backend.dto.requests.users;

public record SigninRequest(
        String username,
        String password
) {}
