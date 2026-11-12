package com.example.backend.services.users;

import com.example.backend.model.users.User;
import com.example.backend.repositories.users.UserRepository;
import com.example.backend.services.security.UserDetailsServiceImplementation;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserDetailsServiceImplementation userDetailsService;

    public static final Map<String, String> AVATAR_COLOURS = Map.of(
        "red", "#e66262",
        "orange", "#ec9a60",
        "yellow", "#ead63f",
        "lime", "#aee35f",
        "green", "#24d665",
        "sky", "#53c8e5",
        "blue", "#7ba9f2",
        "indigo", "#9076f1",
        "violet", "#c078ed",
        "pink", "#f97db3"
    );
    public static final List<String> AVATAR_COLOURS_LIST = AVATAR_COLOURS.keySet().stream().toList();

    /**
     * Method to check if user repository is empty.
     * Useful for loading entities during development.
     * @return true if is, otherwise false.
     */
    public boolean isUserRepositoryEmpty() {
        return userRepository.count() == 0;
    }

    /**
     * Method to register a new user.
     * @param username The input username.
     * @param email The input email.
     * @param password The input password.
     */
    @Transactional
    public void registerUser(String email, String username, String password) {

        //Encrypt password to be held securely in database.
        password = passwordEncoder.encode(password);

        //Generate random colour for user profile.
        String avatarColour = selectRandomAvatarColour();

        //Create and save new user object.
        User user = new User(
            username.trim().replaceAll("\\s+", ""),
            email,
            password,
            avatarColour,
            "USER"
        );
        userRepository.save(user);
    }

    /**
     * Method to select a random avatar colour
     * Useful for registering a user.
     * @return a random avatar colour hex code from the avatar colour set.
     */
    public String selectRandomAvatarColour() {
        int randomNumber = (int) (Math.random() * AVATAR_COLOURS.size());
        return AVATAR_COLOURS_LIST.get(randomNumber);
    }

    /**
     * Method to authenticate the user,
     * or provide a more precise error message for why the user could not be authenticated.
     * @param username The username input.
     * @param password The password input.
     * @return Authentication object if user can successfully be authenticated.
     * @throws BadCredentialsException exception with a more precise error message.
     */
    public Authentication authenticateUser(String username, String password) throws BadCredentialsException {
        UserDetails userDetails;

        //If user couldn't be found, throw error
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            throw new BadCredentialsException("Incorrect username or password.");
        }

        //If password doesn't match stored password, throw error
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Incorrect password.");
        }

        //Build and return the authentication object, otherwise throw error if failed.
        try {
            return new UsernamePasswordAuthenticationToken(username, password);
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Incorrect username or password.");
        }
    }

    /**
     * Method to find user by authentication object
     * Usually called in controller to fetch user object.
     * @param authentication The authentication object which is user details.
     * @return The user object
     * @throws ResponseStatusException If the user wasn't found, allow throw an unauthorised HttpStatus response.
     */
    public User findUserByAuthentication(Authentication authentication) throws ResponseStatusException {
        User currentUser = findUserByUsername(authentication.getName());
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found.");
        }
        return currentUser;
    }

    /**
     * Method to validate username format.
     * @param username The username input
     * @return true if valid, otherwise false
     */
    public boolean isValidUsername(String username) {
        //Username presence check
        if (username == null || username.isBlank()) { return false; }
        //Username format and length check
        return username.matches("^[A-Za-z\\d_]{3,20}$");
    }

    /**
     * Method to validate email format.
     * @param email The email input
     * @return true if valid, otherwise false.
     */
    public boolean isEmailValid(String email) {
        //Email presence check
        if (email == null || email.isBlank()) { return false; }
        //Email format check
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]+$");
    }

    /**
     * Method to validate password format. Checks that:
     * <ul>
     *     <li> at least 8 characters </li>
     *     <li> within a maximum of 64 characters </li>
     *     <li> no unsupported characters </li>
     * </ul>
     * @param password The password input
     * @return true if valid, otherwise false.
     */
    public boolean isPasswordValid(String password) {
        //Password presence check
        if (password == null || password.isBlank()) { return false; }
        //Password format and length check
        return password.matches("^[A-Za-z\\d!@#$%^&*_-]{8,64}$");
    }

    /**
     * Method to check if username is available, or has already been used.
     * @param username The username to check for.
     * @return true if available, otherwise false.
     */
    public boolean isUsernameAvailable(String username) {
        return userRepository.findByUsername(username).isEmpty();
    }

    /**
     * Method to find user by username.
     * @param username The username to find by.
     * @return The user object or null if not found.
     */
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

}
