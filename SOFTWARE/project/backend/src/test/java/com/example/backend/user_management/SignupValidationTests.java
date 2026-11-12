package com.example.backend.user_management;

import com.example.backend.services.users.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
public class SignupValidationTests {

    private final UserService userService = new UserService();

    /* ---------- Testing userService.isValidEmail(String email) ---------- */

    @Test
    void testIsValidEmailEmpty() {
        assertFalse(userService.isEmailValid(""));
        assertFalse(userService.isEmailValid(" "));
        assertFalse(userService.isEmailValid(null));
    }

    @Test
    void testIsValidEmailFormat() {
        assertTrue(userService.isEmailValid("example@example.com"));
        assertTrue(userService.isEmailValid("xx123@student.le.ac.uk"));
        assertTrue(userService.isEmailValid("example@example.c"));
        assertFalse(userService.isEmailValid("example@example."));
        assertFalse(userService.isEmailValid("@example.com"));
        assertFalse(userService.isEmailValid("example@example@a.com"));
    }

    /* ---------- Testing userService.isValidUsername(String username) ---------- */

    @Test
    void testIsValidUsernameEmpty() {
        assertFalse(userService.isValidUsername(""));
        assertFalse(userService.isValidUsername(" "));
        assertFalse(userService.isValidUsername(null));
    }

    @Test
    void testIsValidUsernameFormat() {
        assertTrue(userService.isValidUsername("va112"));
        assertTrue(userService.isValidUsername("user_name"));
        assertFalse(userService.isValidUsername("example@example.com"));
        assertFalse(userService.isValidUsername("spa ce"));
        assertFalse(userService.isValidUsername("&user"));
    }

    @Test
    void testIsValidUsernameLength() {
        assertTrue(userService.isValidUsername("abc"));
        assertTrue(userService.isValidUsername("abcdefghijklmnopqrst"));
        //Boundary tests
        assertFalse(userService.isValidUsername("ab"));
        assertFalse(userService.isValidUsername("abcdefghijklmnopqrstu"));
    }

    /* ---------- Testing userService.isValidPassword(String password) ---------- */

    @Test
    void testIsValidPasswordEmpty() {
        assertFalse(userService.isPasswordValid(""));
        assertFalse(userService.isPasswordValid(" "));
        assertFalse(userService.isPasswordValid(null));
    }

    @Test
    void testIsValidPasswordFormat() {
        assertTrue(userService.isPasswordValid("abcABC123!@#$%^&*_-"));
        assertFalse(userService.isPasswordValid("abcABC123 !@#$%^&*_-"));
        assertFalse(userService.isPasswordValid("abcABC123'!@#$%^&*_-"));
        assertFalse(userService.isPasswordValid("abcABC123\"!@#$%^&*_-"));
        assertFalse(userService.isPasswordValid("abcABC123\\!@#$%^&*_-"));
        assertFalse(userService.isPasswordValid("abcABC123/!@#$%^&*_-"));
    }

    @Test
    void testIsValidPasswordLength() {
        assertTrue(userService.isPasswordValid("12345678"));
        assertTrue(userService.isPasswordValid("1234567812345678123456781234567812345678123456781234567812345678"));
        //Boundary tests
        assertFalse(userService.isPasswordValid("1234567"));
        assertFalse(userService.isPasswordValid("12345678123456781234567812345678123456781234567812345678123456789"));
    }

}
