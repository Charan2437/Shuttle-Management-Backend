package com.shuttle.shuttlesystem.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shuttle.shuttlesystem.model.Stop;
import com.shuttle.shuttlesystem.repository.StopRepository;
import com.shuttle.shuttlesystem.service.StopService;

@Service
public class StopServiceImpl implements StopService {
    @Autowired
    private StopRepository stopRepository;

    @Override
    public List<Stop> getAllStops() {
        return stopRepository.findAll();
    }

    @Override
    public Stop getStopById(UUID id) {
        return stopRepository.findById(id).orElseThrow();
    }

    @Override
    @Transactional
    public Stop createStop(Stop stop) {
        return stopRepository.save(stop);
    }

    @Override
    @Transactional
    public Stop updateStop(UUID id, Stop stop) {
        Stop existing = stopRepository.findById(id).orElseThrow();
        existing.setName(stop.getName());
        existing.setDescription(stop.getDescription());
        existing.setLatitude(stop.getLatitude());
        existing.setLongitude(stop.getLongitude());
        existing.setAddress(stop.getAddress());
        return stopRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteStop(UUID id) {
        stopRepository.deleteById(id);
    }
}
