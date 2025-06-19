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
@Table(name = "route_stops")
public class RouteStop {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id")
    private Stop stop;

    private int stopOrder;
    private Integer estimatedTravelTime;
    private Double distanceFromPrevious;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public Stop getStop() { return stop; }
    public void setStop(Stop stop) { this.stop = stop; }
    public int getStopOrder() { return stopOrder; }
    public void setStopOrder(int stopOrder) { this.stopOrder = stopOrder; }
    public Integer getEstimatedTravelTime() { return estimatedTravelTime; }
    public void setEstimatedTravelTime(Integer estimatedTravelTime) { this.estimatedTravelTime = estimatedTravelTime; }
    public Double getDistanceFromPrevious() { return distanceFromPrevious; }
    public void setDistanceFromPrevious(Double distanceFromPrevious) { this.distanceFromPrevious = distanceFromPrevious; }
}
