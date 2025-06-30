package com.inventory.service;

import com.inventory.model.User;
import com.inventory.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing User entities.
 * Provides methods for retrieving users and now includes CRUD operations for user management.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Retrieves all users from the database.
     * @return A list of all User entities.
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Finds a user by their ID.
     * @param id The ID of the user to find.
     * @return An Optional containing the User if found, or empty otherwise.
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * NEW: Creates a new user.
     * @param user The User object to save. Password will be encoded.
     * @return The saved User entity.
     * @throws IllegalArgumentException if username already exists.
     */
    @Transactional
    public User createUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }
        user.setPassword(passwordEncoder.encode(user.getPassword())); // Encode password before saving
        return userRepository.save(user);
    }

    /**
     * NEW: Updates an existing user.
     * @param id The ID of the user to update.
     * @param updatedUser The User object with updated details.
     * @return The updated User entity.
     * @throws EntityNotFoundException if the user with the given ID is not found.
     * @throws IllegalArgumentException if trying to change username to an existing one, or if password is null for update.
     */
    @Transactional
    public User updateUser(Long id, User updatedUser) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        // Allow updating password only if provided and not empty
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        } else if (updatedUser.getPassword() == null && existingUser.getPassword() == null) {
            // If both are null, this is likely an error or a case where password isn't being set
            // For updates, password should typically be handled explicitly.
            // For now, if provided as null and existing is null, we can skip, but often it indicates an issue
        }

        // Check if username is being changed to an existing one
        if (!existingUser.getUsername().equals(updatedUser.getUsername())) {
            if (userRepository.findByUsername(updatedUser.getUsername()).isPresent()) {
                throw new IllegalArgumentException("New username '" + updatedUser.getUsername() + "' already exists.");
            }
            existingUser.setUsername(updatedUser.getUsername());
        }

        // CORRECTED: Use getRoles() and setRoles() for the Set<Role>
        existingUser.setRoles(updatedUser.getRoles());

        return userRepository.save(existingUser);
    }

    /**
     * NEW: Deletes a user by their ID.
     * @param id The ID of the user to delete.
     * @throws EntityNotFoundException if the user is not found.
     */
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }
}
