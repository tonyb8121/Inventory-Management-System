package com.inventory.model;

/**
 * Represents a simplified Category data transfer object (DTO) for the frontend.
 * This class mirrors the backend Category entity structure but does NOT contain
 * JPA annotations, as it's purely for data exchange and display in the UI.
 */
public class Category {
    private Long id;
    private String name;
    private String description;

    public Category() {
        // Default constructor for Gson
    }

    public Category(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
