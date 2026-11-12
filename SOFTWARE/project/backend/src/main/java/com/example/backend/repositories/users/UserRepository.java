package com.example.backend.repositories.users;

import com.example.backend.model.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Find a user by their username,
     * Useful for checking if their username is unique
     * @param username String username of user to fetch.
     * @return Optional user object
     */
    Optional<User> findByUsername(String username);

}
