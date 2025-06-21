package com.shuttle.shuttlesystem.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shuttle.shuttlesystem.model.Stop;
import com.shuttle.shuttlesystem.repository.StopRepository;
import com.shuttle.shuttlesystem.service.CacheService;
import com.shuttle.shuttlesystem.service.StopService;

@Service
public class StopServiceImpl implements StopService {
    @Autowired
    private StopRepository stopRepository;
    
    @Autowired
    private CacheService cacheService;

    @Override
    @Cacheable(value = "stops", key = "'all'")
    public List<Stop> getAllStops() {
        return stopRepository.findAll();
    }

    @Override
    @Cacheable(value = "stops", key = "#id.toString()")
    public Stop getStopById(UUID id) {
        return stopRepository.findById(id).orElseThrow();
    }

    @Override
    @Transactional
    @CacheEvict(value = "stops", allEntries = true)
    public Stop createStop(Stop stop) {
        Stop savedStop = stopRepository.save(stop);
        // Invalidate related caches
        cacheService.invalidateRouteCaches();
        return savedStop;
    }

    @Override
    @Transactional
    @CacheEvict(value = "stops", key = "#id.toString()")
    public Stop updateStop(UUID id, Stop stop) {
        Stop existing = stopRepository.findById(id).orElseThrow();
        existing.setName(stop.getName());
        existing.setDescription(stop.getDescription());
        existing.setLatitude(stop.getLatitude());
        existing.setLongitude(stop.getLongitude());
        existing.setAddress(stop.getAddress());
        Stop updatedStop = stopRepository.save(existing);
        
        // Invalidate related caches
        cacheService.invalidateRouteCaches();
        return updatedStop;
    }

    @Override
    @Transactional
    @CacheEvict(value = "stops", allEntries = true)
    public void deleteStop(UUID id) {
        stopRepository.deleteById(id);
        // Invalidate related caches
        cacheService.invalidateRouteCaches();
    }
}
