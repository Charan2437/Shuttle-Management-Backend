package com.shuttle.shuttlesystem.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "routes")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;
    private String description;
    private String color;
    private int estimatedDuration;
    private int baseFare;
    private boolean isActive;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RouteStop> stops = new ArrayList<>();

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RouteOperatingHour> operatingHours = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public int getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(int estimatedDuration) { this.estimatedDuration = estimatedDuration; }
    public int getBaseFare() { return baseFare; }
    public void setBaseFare(int baseFare) { this.baseFare = baseFare; }
    public boolean isIsActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }
    public List<RouteStop> getStops() { return stops; }
    public void setStops(List<RouteStop> stops) { this.stops = stops; }
    public List<RouteOperatingHour> getOperatingHours() { return operatingHours; }
    public void setOperatingHours(List<RouteOperatingHour> operatingHours) { this.operatingHours = operatingHours; }
}
