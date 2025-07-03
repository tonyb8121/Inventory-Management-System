package com.inventory.model;

import java.util.Set; // For roles

/**
 * Represents a user in the system for the frontend application.
 * This is a simplified POJO (Plain Old Java Object) mirroring the backend User entity,
 * but without JPA annotations, as it's used purely for data transfer and UI display.
 */
public class User {

    private Long id;
    private String username;
    private String password; // Raw password for sending to backend (will be hashed there)
    private Set<Role> roles;

    // Enum for user roles (must be public)
    public enum Role {
        OWNER,
        CASHIER
    }

    // Constructors
    public User() {
    }

    public User(Long id, String username, String password, Set<Role> roles) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

    // Constructor for creating a new user (without ID initially)
    public User(String username, String password, Set<Role> roles) {
        this(null, username, password, roles);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", roles=" + roles +
                '}';
    }
}
