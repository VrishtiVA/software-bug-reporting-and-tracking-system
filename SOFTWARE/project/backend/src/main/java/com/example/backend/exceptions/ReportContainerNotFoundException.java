package com.example.backend.exceptions;

public class ReportContainerNotFoundException extends RuntimeException {
    public ReportContainerNotFoundException(String message) {
        super(message);
    }
}
