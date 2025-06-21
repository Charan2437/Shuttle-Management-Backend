package com.shuttle.shuttlesystem.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "shuttles")
public class Shuttle {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private String shuttleNo;
    private int capacity;
    // ...getters/setters...
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getShuttleNo() { return shuttleNo; }
    public void setShuttleNo(String shuttleNo) { this.shuttleNo = shuttleNo; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
}
