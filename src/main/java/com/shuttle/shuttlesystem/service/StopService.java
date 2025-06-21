package com.shuttle.shuttlesystem.service;

import java.util.List;
import java.util.UUID;

import com.shuttle.shuttlesystem.model.Stop;

public interface StopService {
    List<Stop> getAllStops();
    Stop getStopById(UUID id);
    Stop createStop(Stop stop);
    Stop updateStop(UUID id, Stop stop);
    void deleteStop(UUID id);
}
