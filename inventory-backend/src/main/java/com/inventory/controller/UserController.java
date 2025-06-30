package com.inventory.controller;

import com.inventory.model.User;
import com.inventory.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityNotFoundException;


import java.util.List;

/**
 * REST Controller for managing User entities.
 * Accessible only by users with the OWNER role.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get all users.
     * Accessible by OWNER and CASHIER (for dropdowns, e.g., in sales history).
     * @return List of all User entities.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_CASHIER')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Get a user by ID.
     * Accessible only by OWNER.
     * @param id The ID of the user.
     * @return The User if found, else 404.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new user.
     * Accessible only by OWNER.
     * @param user The user object to create.
     * @return The created user with HTTP status 201.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User savedUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Update an existing user.
     * Accessible only by OWNER.
     * @param id The ID of the user to update.
     * @param user The updated user details.
     * @return The updated user, else 404 or 400.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            User updatedUser = userService.updateUser(id, user);
            return ResponseEntity.ok(updatedUser);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Delete a user by ID.
     * Accessible only by OWNER.
     * @param id The ID of the user to delete.
     * @return 204 No Content on success, or 404 if not found.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
