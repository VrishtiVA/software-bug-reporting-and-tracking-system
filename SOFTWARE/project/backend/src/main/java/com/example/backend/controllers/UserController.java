package com.example.backend.controllers;

import com.example.backend.exceptions.BadRequestException;
import com.example.backend.model.users.User;
import com.example.backend.dto.requests.users.SigninRequest;
import com.example.backend.dto.requests.users.SignupRequest;
import com.example.backend.dto.responses.users.AuthenticationResponse;
import com.example.backend.dto.responses.ErrorResponse;
import com.example.backend.services.security.JwtService;
import com.example.backend.services.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    /**
     * Endpoint to handle sign in.
     * @see SigninRequest
     * @return
     * <ul>
     *     <li> {@code (200) OK} if successful sign in. </li>
     *     <li> {@code (400) BAD REQUEST} if no credentials provided. </li>
     *     <li> {@code (401) UNAUTHORISED} if sign in fails due to invalid credentials. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @PostMapping("/auth/signin")
    public ResponseEntity<?> signin(@RequestBody SigninRequest signinRequest) {
        try {
            String username = signinRequest.username();
            String password = signinRequest.password();

            //Presence check validation
            if (username == null || password == null || username.isBlank() || password.isBlank()) {
                throw new BadRequestException("Credentials were missing.");
            }

            //Sanitise inputs
            username = username.trim();
            password = password.trim();

            //Authenticate User
            Authentication authentication = userService.authenticateUser(username, password);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            User currentUser = userService.findUserByUsername(username);
            if (currentUser == null) {
                throw new Exception("Unable to find user.");
            }

            //Provide JWT Authentication Token
            String token = jwtService.generateToken(authentication);

            //Create and return success response
            AuthenticationResponse authenticationResponse = new AuthenticationResponse("Successful sign in", token, currentUser.getUsername(), currentUser.getColour());
            return new ResponseEntity<>(authenticationResponse, HttpStatus.OK);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Sign in failed", e.getMessage(), Instant.now());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (BadCredentialsException e) {
            ErrorResponse errorResponse = new ErrorResponse("Sign in failed", e.getMessage(), Instant.now());
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("Failed in POST /auth/signin", e);
            ErrorResponse errorResponse = new ErrorResponse("Sign in failed", "Something went wrong.", Instant.now());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint to handle sign up.
     * @see SignupRequest
     * @return
     * <ul>
     *     <li> {@code (201) CREATED} if successful sign up. </li>
     *     <li> {@code (400) BAD REQUEST} if no/invalid inputs provided. </li>
     *     <li> {@code (409) CONFLICT} if username is already taken. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @PostMapping("/auth/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest signupRequest) {
        try {
            String email = signupRequest.email();
            String username = signupRequest.username();
            String password = signupRequest.password();

            //Presence check validation
            if (email == null || username == null || password == null || username.isBlank() || password.isBlank()) {
                throw new BadRequestException("No credentials provided."); //Change to missing inputs.
            }

            //Sanitise inputs
            email = email.trim();
            username = username.trim();
            password = password.trim();

            //Validate email format
            if (!userService.isEmailValid(email)) {
                throw new BadRequestException("Email is invalid.");
            }
            //Validate username format
            if (!userService.isValidUsername(username)) {
                throw new BadRequestException("Username is invalid.");
            }
            //Validate username is unique
            if (!userService.isUsernameAvailable(username)) {
                ErrorResponse errorResponse = new ErrorResponse("Sign up failed", "Username is already taken.", Instant.now());
                return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
            }
            //Validate password
            if (!userService.isPasswordValid(password)) {
                throw new BadRequestException("Password is invalid.");
            }

            //Register user
            userService.registerUser(email, username, password);

            // -------------------- Sign In after Sign Up -------------------- //

            //Authenticate User
            Authentication authentication = userService.authenticateUser(username, password);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            User currentUser = userService.findUserByUsername(username);
            if (currentUser == null) {
                throw new Exception("Unable to find user.");
            }

            //Provide JWT Authentication Token
            String token = jwtService.generateToken(authentication);

            //Create and return success response
            AuthenticationResponse authenticationResponse = new AuthenticationResponse("Successful sign up", token, currentUser.getUsername(), currentUser.getColour());
            return new ResponseEntity<>(authenticationResponse, HttpStatus.CREATED);

            // ----------------------------------------------------------------- //

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Sign up failed", e.getMessage(), Instant.now());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in POST /auth/signup", e);
            ErrorResponse errorResponse = new ErrorResponse("Sign up failed", "Something went wrong.", Instant.now());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Method to check if username is available
     * To be used on the frontend.
     * @param username The username input
     * @return true if it is, otherwise false.
     */
    @GetMapping("/auth/validate/is-username-available")
    @ResponseBody
    public boolean isUsernameAvailable(@RequestParam String username) {
        if (userService.isValidUsername(username)) {
            return userService.isUsernameAvailable(username.trim());
        } else {
            return false;
        }
    }

}
