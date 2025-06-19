package com.shuttle.shuttlesystem.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "route_operating_hours")
public class RouteOperatingHour {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    private int dayOfWeek;
    private java.time.LocalTime startTime;
    private java.time.LocalTime endTime;
    private boolean isActive;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public java.time.LocalTime getStartTime() { return startTime; }
    public void setStartTime(java.time.LocalTime startTime) { this.startTime = startTime; }
    public java.time.LocalTime getEndTime() { return endTime; }
    public void setEndTime(java.time.LocalTime endTime) { this.endTime = endTime; }
    public boolean isIsActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }
}
