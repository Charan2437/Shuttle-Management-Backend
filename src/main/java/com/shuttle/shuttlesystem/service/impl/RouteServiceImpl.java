package com.shuttle.shuttlesystem.service.impl;

import com.shuttle.shuttlesystem.dto.RouteWithStopsAndHoursDTO;
import com.shuttle.shuttlesystem.model.*;
import com.shuttle.shuttlesystem.repository.*;
import com.shuttle.shuttlesystem.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RouteServiceImpl implements RouteService {
    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private RouteStopRepository routeStopRepository;
    @Autowired
    private StopRepository stopRepository;
    @Autowired
    private RouteOperatingHourRepository routeOperatingHourRepository;

    @Override
    public List<RouteWithStopsAndHoursDTO> getAllRoutes() {
        List<Route> routes = routeRepository.findAll();
        List<RouteWithStopsAndHoursDTO> result = new ArrayList<>();
        for (Route route : routes) {
            result.add(assembleRouteDTO(route));
        }
        return result;
    }

    @Override
    public RouteWithStopsAndHoursDTO getRouteById(UUID id) {
        Route route = routeRepository.findById(id).orElseThrow();
        return assembleRouteDTO(route);
    }

    @Override
    @Transactional
    public RouteWithStopsAndHoursDTO createRoute(RouteWithStopsAndHoursDTO dto) {
        Route route = new Route();
        route.setName(dto.name);
        route.setDescription(dto.description);
        route.setColor(dto.color);
        route.setEstimatedDuration(dto.estimatedDuration);
        route.setBaseFare(dto.baseFare);
        route.setIsActive(true);
        route = routeRepository.save(route);
        if (dto.stops != null) {
            for (RouteWithStopsAndHoursDTO.StopDTO stopDto : dto.stops) {
                RouteStop rs = new RouteStop();
                rs.setRoute(route);
                rs.setStop(stopRepository.findById(stopDto.id).orElseThrow());
                rs.setStopOrder(stopDto.stopOrder);
                rs.setEstimatedTravelTime(stopDto.estimatedTravelTime);
                rs.setDistanceFromPrevious(stopDto.distanceFromPrevious);
                routeStopRepository.save(rs);
            }
        }
        if (dto.operatingHours != null) {
            for (RouteWithStopsAndHoursDTO.RouteOperatingHourDTO hourDto : dto.operatingHours) {
                RouteOperatingHour roh = new RouteOperatingHour();
                roh.setRoute(route);
                roh.setDayOfWeek(hourDto.dayOfWeek);
                roh.setStartTime(java.time.LocalTime.parse(hourDto.startTime));
                roh.setEndTime(java.time.LocalTime.parse(hourDto.endTime));
                roh.setIsActive(true);
                routeOperatingHourRepository.save(roh);
            }
        }
        return assembleRouteDTO(route);
    }

    @Override
    @Transactional
    public RouteWithStopsAndHoursDTO updateRoute(UUID id, RouteWithStopsAndHoursDTO dto) {
        Route route = routeRepository.findById(id).orElseThrow();
        route.setName(dto.name);
        route.setDescription(dto.description);
        route.setColor(dto.color);
        route.setEstimatedDuration(dto.estimatedDuration);
        route.setBaseFare(dto.baseFare);
        routeRepository.save(route);
        routeStopRepository.deleteByRouteId(id);
        routeStopRepository.flush(); // Ensure deletes are committed
        routeOperatingHourRepository.deleteByRouteId(id);
        routeOperatingHourRepository.flush(); // Ensure deletes are committed
        if (dto.stops != null) {
            for (RouteWithStopsAndHoursDTO.StopDTO stopDto : dto.stops) {
                RouteStop rs = new RouteStop();
                rs.setRoute(route);
                rs.setStop(stopRepository.findById(stopDto.id).orElseThrow());
                rs.setStopOrder(stopDto.stopOrder);
                rs.setEstimatedTravelTime(stopDto.estimatedTravelTime);
                rs.setDistanceFromPrevious(stopDto.distanceFromPrevious);
                routeStopRepository.save(rs);
            }
        }
        if (dto.operatingHours != null) {
            for (RouteWithStopsAndHoursDTO.RouteOperatingHourDTO hourDto : dto.operatingHours) {
                RouteOperatingHour roh = new RouteOperatingHour();
                roh.setRoute(route);
                roh.setDayOfWeek(hourDto.dayOfWeek);
                roh.setStartTime(java.time.LocalTime.parse(hourDto.startTime));
                roh.setEndTime(java.time.LocalTime.parse(hourDto.endTime));
                roh.setIsActive(true);
                routeOperatingHourRepository.save(roh);
            }
        }
        return assembleRouteDTO(route);
    }

    @Override
    @Transactional
    public void deleteRoute(UUID id) {
        routeStopRepository.deleteByRouteId(id);
        routeOperatingHourRepository.deleteByRouteId(id);
        routeRepository.deleteById(id);
    }

    private RouteWithStopsAndHoursDTO assembleRouteDTO(Route route) {
        RouteWithStopsAndHoursDTO dto = new RouteWithStopsAndHoursDTO();
        dto.id = route.getId();
        dto.name = route.getName();
        dto.description = route.getDescription();
        dto.color = route.getColor();
        dto.estimatedDuration = route.getEstimatedDuration();
        dto.baseFare = route.getBaseFare();
        dto.isActive = route.isIsActive();
        List<RouteStop> stops = routeStopRepository.findByRouteIdOrderByStopOrder(route.getId());
        dto.stops = stops.stream().map(rs -> {
            RouteWithStopsAndHoursDTO.StopDTO stopDto = new RouteWithStopsAndHoursDTO.StopDTO();
            stopDto.id = rs.getStop().getId();
            stopDto.name = rs.getStop().getName();
            stopDto.description = rs.getStop().getDescription();
            stopDto.latitude = rs.getStop().getLatitude();
            stopDto.longitude = rs.getStop().getLongitude();
            stopDto.address = rs.getStop().getAddress();
            stopDto.stopOrder = rs.getStopOrder();
            stopDto.estimatedTravelTime = rs.getEstimatedTravelTime();
            stopDto.distanceFromPrevious = rs.getDistanceFromPrevious();
            return stopDto;
        }).collect(Collectors.toList());
        List<RouteOperatingHour> hours = routeOperatingHourRepository.findByRouteId(route.getId());
        dto.operatingHours = hours.stream().map(h -> {
            RouteWithStopsAndHoursDTO.RouteOperatingHourDTO hourDto = new RouteWithStopsAndHoursDTO.RouteOperatingHourDTO();
            hourDto.id = h.getId();
            hourDto.dayOfWeek = h.getDayOfWeek();
            hourDto.startTime = h.getStartTime().toString();
            hourDto.endTime = h.getEndTime().toString();
            return hourDto;
        }).collect(Collectors.toList());
        return dto;
    }
}
