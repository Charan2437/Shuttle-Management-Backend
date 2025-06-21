package com.shuttle.shuttlesystem.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "booking_status_types")
public class BookingStatusType {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;
    private String description;
    private String color;
    private boolean isActive;
    private java.time.Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
}
