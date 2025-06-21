package com.shuttle.shuttlesystem.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shuttle.shuttlesystem.dto.ShuttleDTO;
import com.shuttle.shuttlesystem.model.Shuttle;
import com.shuttle.shuttlesystem.repository.ShuttleRepository;
import com.shuttle.shuttlesystem.service.ShuttleService;

@Service
public class ShuttleServiceImpl implements ShuttleService {
    
    @Autowired
    private ShuttleRepository shuttleRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<ShuttleDTO> getAllShuttles() {
        List<Shuttle> shuttles = shuttleRepository.findAll();
        return shuttles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ShuttleDTO getShuttleById(UUID id) {
        Shuttle shuttle = shuttleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shuttle not found with id: " + id));
        return convertToDTO(shuttle);
    }

    @Override
    @Transactional
    public ShuttleDTO createShuttle(ShuttleDTO dto) {
        Shuttle shuttle = new Shuttle();
        shuttle.setShuttleNo(dto.shuttleNo);
        shuttle.setCapacity(dto.capacity);
        
        Shuttle savedShuttle = shuttleRepository.save(shuttle);
        return convertToDTO(savedShuttle);
    }

    @Override
    @Transactional
    public ShuttleDTO updateShuttle(UUID id, ShuttleDTO dto) {
        Shuttle shuttle = shuttleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shuttle not found with id: " + id));
        
        shuttle.setShuttleNo(dto.shuttleNo);
        shuttle.setCapacity(dto.capacity);
        
        Shuttle savedShuttle = shuttleRepository.save(shuttle);
        return convertToDTO(savedShuttle);
    }

    @Override
    @Transactional
    public void deleteShuttle(UUID id) {
        // First remove any route assignments
        jdbcTemplate.update("DELETE FROM shuttle_routes WHERE shuttle_id = ?", id);
        
        // Then delete the shuttle
        shuttleRepository.deleteById(id);
    }

    @Override
    public List<ShuttleDTO> getAvailableShuttles() {
        String sql = """
            SELECT s.id, s.shuttle_no, s.capacity 
            FROM shuttles s 
            WHERE s.id NOT IN (
                SELECT DISTINCT shuttle_id 
                FROM shuttle_routes 
                WHERE shuttle_id IS NOT NULL
            )
            ORDER BY s.shuttle_no
            """;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ShuttleDTO dto = new ShuttleDTO();
            dto.id = UUID.fromString(rs.getString("id"));
            dto.shuttleNo = rs.getString("shuttle_no");
            dto.capacity = rs.getInt("capacity");
            dto.routeName = null;
            dto.isAssigned = false;
            return dto;
        });
    }

    @Override
    public List<ShuttleDTO> getAssignedShuttles() {
        String sql = """
            SELECT s.id, s.shuttle_no, s.capacity, r.name as route_name
            FROM shuttles s 
            JOIN shuttle_routes sr ON s.id = sr.shuttle_id
            JOIN routes r ON sr.route_id = r.id
            ORDER BY s.shuttle_no
            """;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ShuttleDTO dto = new ShuttleDTO();
            dto.id = UUID.fromString(rs.getString("id"));
            dto.shuttleNo = rs.getString("shuttle_no");
            dto.capacity = rs.getInt("capacity");
            dto.routeName = rs.getString("route_name");
            dto.isAssigned = true;
            return dto;
        });
    }

    private ShuttleDTO convertToDTO(Shuttle shuttle) {
        ShuttleDTO dto = new ShuttleDTO();
        dto.id = shuttle.getId();
        dto.shuttleNo = shuttle.getShuttleNo();
        dto.capacity = shuttle.getCapacity();
        
        // Check if shuttle is assigned to a route
        try {
            String routeName = jdbcTemplate.queryForObject(
                "SELECT r.name FROM routes r JOIN shuttle_routes sr ON r.id = sr.route_id WHERE sr.shuttle_id = ?",
                new Object[]{shuttle.getId()},
                String.class
            );
            dto.routeName = routeName;
            dto.isAssigned = routeName != null;
        } catch (Exception e) {
            dto.routeName = null;
            dto.isAssigned = false;
        }
        
        return dto;
    }
} 