package com.shuttle.shuttlesystem.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stops")
public class Stop {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;
    private String description;
    private double latitude;
    private double longitude;
    private String address;
    private boolean isActive;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public boolean isIsActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }
}
