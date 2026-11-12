package com.example.backend.dto.responses;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

public class ErrorResponse {

    private String message;
    private String details;
    private Instant timestamp;
    private boolean redirect;

    public ErrorResponse(String message, String details, Instant timestamp) {
        this.message = message;
        this.details = details;
        this.timestamp = timestamp;
        this.redirect = false;
    }
    public ErrorResponse(String message, String details, Instant timestamp, boolean redirect) {
        this.message = message;
        this.details = details;
        this.timestamp = timestamp;
        this.redirect = redirect;
    }

    public String getMessage() {return message;}
    public void setMessage(String message) {this.message = message;}
    public String getDetails() {return details;}
    public void setDetails(String details) {this.details = details;}
    public Instant getTimestamp() {return timestamp;}
    public void setTimestamp(Instant timestamp) {this.timestamp = timestamp;}
    public boolean isRedirect() {return redirect;}
    public void setRedirect(boolean redirect) {this.redirect = redirect;}

//    /** Method to return a default something went wrong error response object. */
//    public static ErrorResponse somethingWentWrong() {
//        return new ErrorResponse(
//            "Oops!",
//            "Sorry, something went wrong",
//            Instant.now(),
//            false
//        );
//    }

    public static final ResponseEntity<?> SOMETHING_WENT_WRONG_SERVER_ERROR = new ResponseEntity<>(
            new ErrorResponse(
                "Oops!",
                "Sorry, something went wrong",
                Instant.now(),
                false
            ),
            HttpStatus.INTERNAL_SERVER_ERROR
    );

    public static final ResponseEntity<?> RESOURCE_NOT_FOUND = new ResponseEntity<>(
        new ErrorResponse(
            "Resource not found",
            "Sorry, we couldn't find what you were looking for.",
            Instant.now()
        ),
        HttpStatus.NOT_FOUND
    );

    public static final ResponseEntity<?> REQUEST_FORBIDDEN = new ResponseEntity<>(
        new ErrorResponse(
            "Forbidden Request",
            "Sorry, you are not allowed to complete this action.",
            Instant.now()
        ),
        HttpStatus.FORBIDDEN
    );

    /* ---------- Workspace Related Error Response Objects ---------- */

    public static final ResponseEntity<?> WORKSPACE_NOT_FOUND = new ResponseEntity<>(
        new ErrorResponse(
            "Resource not found",
            "Sorry, the workspace you are looking for does not exist.",
            Instant.now(),
            true
        ),
        HttpStatus.NOT_FOUND
    );

    public static final ResponseEntity<?> WORKSPACE_FORBIDDEN_ACCESS = new ResponseEntity<>(
        new ErrorResponse(
            "Forbidden Page",
            "Sorry, you aren't allowed to access this workspace.",
            Instant.now(),
            true
        ),
        HttpStatus.FORBIDDEN
    );

    public static final ResponseEntity<?> WORKSPACE_FORBIDDEN_CONFIGURE = new ResponseEntity<>(
        new ErrorResponse(
            "Forbidden Page",
            "Sorry, you don't have permission to configure this workspace.",
            Instant.now(),
            true
        ),
        HttpStatus.FORBIDDEN
    );

    /* ---------- Report Related Error Response Objects ---------- */

    public static final ResponseEntity<?> RESOLVE_DUPLICATES_REPORT_NOT_FOUND = new ResponseEntity<>(
        new ErrorResponse(
            "Resource not found",
            "Sorry, we couldn't find the reports you're trying to resolve.",
            Instant.now(),
            true
        ),
        HttpStatus.NOT_FOUND
    );

    public static final ResponseEntity<?> BUG_NOT_FOUND = new ResponseEntity<>(
        new ErrorResponse(
            "Resource not found",
            "Sorry, we couldn't find what you were looking for.",
            Instant.now(),
            true
        ),
        HttpStatus.NOT_FOUND
    );

    public static final ResponseEntity<?> BUG_FORBIDDEN_ACCESS = new ResponseEntity<>(
        new ErrorResponse(
            "Forbidden Page",
            "Sorry, you aren't allowed to access these bug resources.",
            Instant.now(),
            true
        ),
        HttpStatus.FORBIDDEN
    );


}
