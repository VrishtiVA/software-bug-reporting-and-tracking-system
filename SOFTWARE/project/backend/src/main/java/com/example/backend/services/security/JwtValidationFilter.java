package com.example.backend.services.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter to validate the JWT token and ensure security context is ready for the request.
 * This filter should not be applied during automated testing, as
 * JWTs will not be provided in the tests, @WithUserDetails() will be used there instead for more simplicity.
 */
@Profile("!test")
@Component
public class JwtValidationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    public JwtValidationFilter(final JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /** Overriding method for OncePerRequestFilter, to validate token from HTTP request before using the endpoint. */
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        //Check token
        jwtService.tokenFilterLogic(request);
        //Pass on control to the next filter, in order to eventually reach the controller.
        filterChain.doFilter(request, response);
    }
}
