package com.inventory.model;

import jakarta.persistence.*;
import java.util.Set; // For roles
import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // Import for JsonIgnoreProperties

/**
 * Represents a user in the system with authentication details and roles.
 */
@Entity
@Table(name = "app_users") // Renamed to avoid potential conflict with SQL 'USER' keyword
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // ADDED: Ignore Hibernate proxy fields for JSON serialization
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // Hashed password

    @ElementCollection(fetch = FetchType.EAGER) // Roles are eagerly fetched
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_name", nullable = false)
    private Set<Role> roles; // Using a Set to store multiple roles

    // Enum for user roles
    public enum Role {
        OWNER,
        CASHIER
    }

    // Constructors
    public User() {
    }

    public User(String username, String password, Set<Role> roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
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
