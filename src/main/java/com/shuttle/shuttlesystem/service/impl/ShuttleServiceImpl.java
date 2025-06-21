package com.shuttle.shuttlesystem.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shuttle.shuttlesystem.dto.ShuttleDTO;
import com.shuttle.shuttlesystem.model.Shuttle;
import com.shuttle.shuttlesystem.repository.ShuttleRepository;
import com.shuttle.shuttlesystem.service.CacheService;
import com.shuttle.shuttlesystem.service.ShuttleService;

@Service
public class ShuttleServiceImpl implements ShuttleService {
    
    @Autowired
    private ShuttleRepository shuttleRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private CacheService cacheService;

    @Override
    @Cacheable(value = "shuttles", key = "'all'")
    public List<ShuttleDTO> getAllShuttles() {
        List<Shuttle> shuttles = shuttleRepository.findAll();
        return shuttles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "shuttles", key = "#id.toString()")
    public ShuttleDTO getShuttleById(UUID id) {
        Shuttle shuttle = shuttleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shuttle not found with id: " + id));
        return convertToDTO(shuttle);
    }

    @Override
    @Transactional
    @CacheEvict(value = "shuttles", allEntries = true)
    public ShuttleDTO createShuttle(ShuttleDTO dto) {
        Shuttle shuttle = new Shuttle();
        shuttle.setShuttleNo(dto.shuttleNo);
        shuttle.setCapacity(dto.capacity);
        
        Shuttle savedShuttle = shuttleRepository.save(shuttle);
        
        // Invalidate related caches
        cacheService.invalidateShuttleCaches();
        
        return convertToDTO(savedShuttle);
    }

    @Override
    @Transactional
    @CacheEvict(value = "shuttles", key = "#id.toString()")
    public ShuttleDTO updateShuttle(UUID id, ShuttleDTO dto) {
        Shuttle shuttle = shuttleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shuttle not found with id: " + id));
        
        shuttle.setShuttleNo(dto.shuttleNo);
        shuttle.setCapacity(dto.capacity);
        
        Shuttle updatedShuttle = shuttleRepository.save(shuttle);
        
        // Invalidate related caches
        cacheService.invalidateShuttleCaches();
        
        return convertToDTO(updatedShuttle);
    }

    @Override
    @Transactional
    @CacheEvict(value = "shuttles", allEntries = true)
    public void deleteShuttle(UUID id) {
        shuttleRepository.deleteById(id);
        // Invalidate related caches
        cacheService.invalidateShuttleCaches();
    }

    @Override
    @Cacheable(value = "shuttles", key = "'available'")
    public List<ShuttleDTO> getAvailableShuttles() {
        String sql = "SELECT s.* FROM shuttles s WHERE s.id NOT IN (SELECT DISTINCT sr.shuttle_id FROM shuttle_routes sr)";
        List<Shuttle> shuttles = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Shuttle shuttle = new Shuttle();
            shuttle.setId(UUID.fromString(rs.getString("id")));
            shuttle.setShuttleNo(rs.getString("shuttle_no"));
            shuttle.setCapacity(rs.getInt("capacity"));
            return shuttle;
        });
        
        return shuttles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "shuttles", key = "'assigned'")
    public List<ShuttleDTO> getAssignedShuttles() {
        String sql = "SELECT DISTINCT s.* FROM shuttles s INNER JOIN shuttle_routes sr ON s.id = sr.shuttle_id";
        List<Shuttle> shuttles = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Shuttle shuttle = new Shuttle();
            shuttle.setId(UUID.fromString(rs.getString("id")));
            shuttle.setShuttleNo(rs.getString("shuttle_no"));
            shuttle.setCapacity(rs.getInt("capacity"));
            return shuttle;
        });
        
        return shuttles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ShuttleDTO convertToDTO(Shuttle shuttle) {
        ShuttleDTO dto = new ShuttleDTO();
        dto.id = shuttle.getId();
        dto.shuttleNo = shuttle.getShuttleNo();
        dto.capacity = shuttle.getCapacity();
        return dto;
    }
} 