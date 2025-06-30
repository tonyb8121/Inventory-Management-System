package com.inventory.repository;

import com.inventory.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entities.
 * Extends JpaRepository to provide standard CRUD operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a User by their username. Used for authentication.
     * @param username The username to search for.
     * @return An Optional containing the User if found, or empty otherwise.
     */
    Optional<User> findByUsername(String username);
}
