package com.example.backend.services.security;

import com.example.backend.model.users.User;
import com.example.backend.repositories.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImplementation implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Method to load the user details object to authenticate a user.
     * @param username The unique username by which to find the user object.
     * @return The user details object.
     * @throws UsernameNotFoundException If the user couldn't be found, this error will be thrown.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        //Try to fetch user, or return null
        User user = userRepository.findByUsername(username).orElse(null);

        //If user couldn't be found, throw error
        if (user == null) {
            throw new UsernameNotFoundException("Couldn't find user by username: " + username);
        }

        //Otherwise create user details service with users credentials
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(user.getRole())
                .build();
    }

}
