package com.shuttle.shuttlesystem.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shuttle.shuttlesystem.dto.RoutePlanOptionDTO;
import com.shuttle.shuttlesystem.dto.RouteWithStopsAndHoursDTO;
import com.shuttle.shuttlesystem.model.Route;
import com.shuttle.shuttlesystem.model.RouteOperatingHour;
import com.shuttle.shuttlesystem.model.RouteStop;
import com.shuttle.shuttlesystem.model.Shuttle;
import com.shuttle.shuttlesystem.repository.RouteOperatingHourRepository;
import com.shuttle.shuttlesystem.repository.RouteRepository;
import com.shuttle.shuttlesystem.repository.RouteStopRepository;
import com.shuttle.shuttlesystem.repository.ShuttleRepository;
import com.shuttle.shuttlesystem.repository.StopRepository;
import com.shuttle.shuttlesystem.service.RouteService;

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
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ShuttleRepository shuttleRepository;

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
        try {
            System.out.println("Creating route with name: " + dto.name);
            
            // Validate required fields
            if (dto.name == null || dto.name.trim().isEmpty()) {
                throw new IllegalArgumentException("Route name is required");
            }
            if (dto.color == null || dto.color.trim().isEmpty()) {
                throw new IllegalArgumentException("Route color is required");
            }
            if (dto.estimatedDuration <= 0) {
                throw new IllegalArgumentException("Estimated duration must be greater than 0");
            }
            if (dto.baseFare < 0) {
                throw new IllegalArgumentException("Base fare cannot be negative");
            }
            
            Route route = new Route();
            route.setName(dto.name);
            route.setDescription(dto.description != null ? dto.description : "");
            route.setColor(dto.color);
            route.setEstimatedDuration(dto.estimatedDuration);
            route.setBaseFare(dto.baseFare);
            route.setIsActive(dto.isActive);
            
            System.out.println("Saving route to database...");
            route = routeRepository.save(route);
            System.out.println("Route saved with ID: " + route.getId());
            
            if (dto.stops != null) {
                System.out.println("Processing " + dto.stops.size() + " stops...");
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
                System.out.println("Processing " + dto.operatingHours.size() + " operating hours...");
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
            
            System.out.println("Assembling response DTO...");
            RouteWithStopsAndHoursDTO result = assembleRouteDTO(route);
            
            // Handle shuttle mapping separately to avoid transaction issues
            if (dto.shuttleName != null && !dto.shuttleName.isEmpty()) {
                System.out.println("Looking for shuttle: " + dto.shuttleName);
                Shuttle shuttle = shuttleRepository.findByShuttleNo(dto.shuttleName);
                if (shuttle != null) {
                    System.out.println("Shuttle found, will create mapping after transaction...");
                    // Set the shuttle name in the result for now
                    result.shuttleName = dto.shuttleName;
                    // We'll handle the actual mapping in a separate call
                } else {
                    System.out.println("Warning: Shuttle with number '" + dto.shuttleName + "' not found. Route created without shuttle assignment.");
                    result.shuttleName = "";
                }
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("Error in createRoute: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
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
        route.setIsActive(dto.isActive);
        routeRepository.save(route);
        
        // Update shuttle mapping if shuttleName is provided
        if (dto.shuttleName != null && !dto.shuttleName.isEmpty()) {
            // First, remove existing shuttle mapping
            jdbcTemplate.update("DELETE FROM shuttle_routes WHERE route_id = ?", route.getId());
            
            // Then add new shuttle mapping
            Shuttle shuttle = shuttleRepository.findByShuttleNo(dto.shuttleName);
            if (shuttle != null) {
                jdbcTemplate.update(
                    "INSERT INTO shuttle_routes (shuttle_id, route_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    shuttle.getId(), route.getId()
                );
            }
        } else {
            // If no shuttle name provided, remove existing mapping
            jdbcTemplate.update("DELETE FROM shuttle_routes WHERE route_id = ?", route.getId());
        }
        
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

    public Object optimizeRoutes(UUID startStopId, UUID endStopId, String departureTime, int k) {
        // 1. Parse departure time and determine search window
        java.time.LocalDateTime depTime = (departureTime != null && !departureTime.isEmpty())
            ? java.time.LocalDateTime.parse(departureTime)
            : java.time.LocalDate.now().atStartOfDay();
        int weekday = depTime.getDayOfWeek().getValue() % 7;
        java.time.LocalTime startTime = depTime.toLocalTime();
        // 2. Load schedule-edges with crowd, cost, time
        String sql = "SELECT rs.route_id, rs.stop_id as from_stop, " +
                "lead(rs.stop_id) OVER w as to_stop, " +
                "rs.departure_time as dep_time, lead(rs.arrival_time) OVER w as arr_time, " +
                "rt.distance_from_previous, rt.avg_travel_time_min, r.base_fare, ph.multiplier as peak_mul, " +
                "AVG(so.occupied_seats::FLOAT / s.capacity) OVER ocw as crowd_ratio " +
                "FROM route_schedule rs " +
                "JOIN route_stops rt ON rt.route_id = rs.route_id AND rt.stop_id = rs.stop_id " +
                "LEFT JOIN peak_hours ph ON ph.route_id = rs.route_id AND ph.day_of_week = rs.day_of_week AND rs.departure_time BETWEEN ph.start_time AND ph.end_time " +
                "JOIN shuttle_routes sr ON sr.route_id = rs.route_id " +
                "JOIN shuttles s ON s.id = sr.shuttle_id " +
                "JOIN shuttle_occupancy so ON so.shuttle_id = s.id AND so.recorded_at BETWEEN (rs.departure_time::timestamp - INTERVAL '5 minutes') AND (rs.departure_time::timestamp + INTERVAL '5 minutes') " +
                "JOIN routes r ON r.id = rs.route_id " +
                "WHERE rs.day_of_week = ? AND rs.departure_time >= ? " +
                "WINDOW w AS (PARTITION BY rs.route_id, rs.day_of_week ORDER BY rs.departure_time), " +
                "ocw AS (PARTITION BY rs.route_id, rs.stop_id, rs.departure_time)";
        List<Map<String, Object>> edges = jdbcTemplate.queryForList(sql, weekday, startTime);
        // 3. Build adjacency list
        Map<UUID, List<Map<String, Object>>> graph = new HashMap<>();
        for (Map<String, Object> e : edges) {
            UUID from = (UUID) e.get("from_stop");
            if (e.get("to_stop") == null) continue;
            graph.computeIfAbsent(from, k2 -> new ArrayList<>()).add(e);
        }
        // 4. Define weight functions
        Map<UUID, Double> routeTotalDistance = new HashMap<>();
        for (Map<String, Object> e : edges) {
            UUID routeId = (UUID) e.get("route_id");
            double dist = e.get("distance_from_previous") != null ? ((Number)e.get("distance_from_previous")).doubleValue() : 0;
            routeTotalDistance.put(routeId, routeTotalDistance.getOrDefault(routeId, 0.0) + dist);
        }
        java.util.function.Function<Map<String, Object>, Double> timeWeight = e -> e.get("avg_travel_time_min") != null ? ((Number)e.get("avg_travel_time_min")).doubleValue() : 0;
        java.util.function.Function<Map<String, Object>, Double> costWeight = e -> {
            UUID routeId = (UUID) e.get("route_id");
            double dist = e.get("distance_from_previous") != null ? ((Number)e.get("distance_from_previous")).doubleValue() : 0;
            double baseFare = e.get("base_fare") != null ? ((Number)e.get("base_fare")).doubleValue() : 0;
            double peakMul = e.get("peak_mul") != null ? ((Number)e.get("peak_mul")).doubleValue() : 1.0;
            double totalDist = routeTotalDistance.getOrDefault(routeId, 1.0);
            return dist * (baseFare / totalDist) * peakMul;
        };
        java.util.function.Function<Map<String, Object>, Double> crowdWeight = e -> e.get("crowd_ratio") != null ? ((Number)e.get("crowd_ratio")).doubleValue() : 0.0;
        Map<String, java.util.function.Function<Map<String, Object>, Double>> weights = Map.of(
            "fastest", timeWeight,
            "cheapest", costWeight,
            "least_crowd", crowdWeight
        );
        // 5. Multi-metric K-best with A* schedule search
        Map<String, List<Map<String, Object>>> results = new HashMap<>();
        for (String metric : weights.keySet()) {
            List<Map<String, Object>> metricResults = aStarYenSchedule(graph, startStopId, endStopId, depTime.toLocalTime(), weights.get(metric), k);
            results.put(metric, metricResults);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("from", startStopId);
        response.put("to", endStopId);
        response.put("departure_time", departureTime);
        response.put("results", results);
        return response;
    }

    // A* + Yen's for schedule-aware K-best
    private List<Map<String, Object>> aStarYenSchedule(Map<UUID, List<Map<String, Object>>> graph, UUID start, UUID end, java.time.LocalTime startTime, java.util.function.Function<Map<String, Object>, Double> weightFn, int K) {
        // This is a simplified version, for each metric
        List<Map<String, Object>> results = new ArrayList<>();
        PriorityQueue<PathState> open = new PriorityQueue<>(Comparator.comparingDouble(s -> s.fScore));
        Map<String, Double> gScore = new HashMap<>();
        open.add(new PathState(start, 0, startTime, new ArrayList<>(), 0));
        while (!open.isEmpty() && results.size() < K) {
            PathState state = open.poll();
            if (state.node.equals(end)) {
                results.add(Map.of(
                    "rank", results.size()+1,
                    "stops", extractStops(state.path, start),
                    "legs", describeLegs(state.path),
                    "total_time", state.g,
                    "total_cost", state.path.stream().mapToDouble(weightFn::apply).sum(),
                    "max_crowding", state.maxCrowd
                ));
                continue;
            }
            for (Map<String, Object> edge : graph.getOrDefault(state.node, List.of())) {
                java.time.LocalTime dep = (java.time.LocalTime) edge.get("dep_time");
                java.time.LocalTime arr = (java.time.LocalTime) edge.get("arr_time");
                if (dep.isBefore(state.time)) continue;
                double w = weightFn.apply(edge);
                double newG = state.g + w;
                double newMaxCrowd = Math.max(state.maxCrowd, edge.get("crowd_ratio") != null ? ((Number)edge.get("crowd_ratio")).doubleValue() : 0.0);
                List<Map<String, Object>> newPath = new ArrayList<>(state.path);
                newPath.add(edge);
                String key = edge.get("to_stop") + ":" + arr;
                if (!gScore.containsKey(key) || newG < gScore.get(key)) {
                    gScore.put(key, newG);
                    open.add(new PathState((UUID) edge.get("to_stop"), newG, arr, newPath, newMaxCrowd));
                }
            }
        }
        return results;
    }

    private List<UUID> extractStops(List<Map<String, Object>> path, UUID start) {
        List<UUID> stops = new ArrayList<>();
        stops.add(start);
        for (Map<String, Object> edge : path) stops.add((UUID) edge.get("to_stop"));
        return stops;
    }
    private List<Map<String, Object>> describeLegs(List<Map<String, Object>> path) {
        List<Map<String, Object>> legs = new ArrayList<>();
        for (Map<String, Object> edge : path) {
            legs.add(Map.of(
                "from", edge.get("from_stop"),
                "to", edge.get("to_stop"),
                "dep_time", edge.get("dep_time"),
                "arr_time", edge.get("arr_time"),
                "distance", edge.get("distance_from_previous"),
                "time", edge.get("avg_travel_time_min"),
                "cost", edge.get("base_fare"),
                "crowd", edge.get("crowd_ratio")
            ));
        }
        return legs;
    }
    private static class PathState {
        UUID node;
        double g;
        java.time.LocalTime time;
        List<Map<String, Object>> path;
        double maxCrowd;
        double fScore;
        PathState(UUID node, double g, java.time.LocalTime time, List<Map<String, Object>> path, double maxCrowd) {
            this.node = node;
            this.g = g;
            this.time = time;
            this.path = path;
            this.maxCrowd = maxCrowd;
            this.fScore = g; // If you want to use a heuristic, set fScore = g + h
        }
    }

    // Helper to get shuttle name for a route
    private String getShuttleNameForRoute(UUID routeId) {
        try {
            String shuttleNo = jdbcTemplate.queryForObject(
                "SELECT s.shuttle_no FROM shuttles s JOIN shuttle_routes sr ON s.id = sr.shuttle_id WHERE sr.route_id = ? LIMIT 1",
                new Object[]{routeId},
                String.class
            );
            return shuttleNo != null ? shuttleNo : "";
        } catch (Exception e) {
            return "";
        }
    }

    private RouteWithStopsAndHoursDTO assembleRouteDTO(Route route) {
        try {
            System.out.println("Starting to assemble DTO for route: " + route.getId());
            
            RouteWithStopsAndHoursDTO dto = new RouteWithStopsAndHoursDTO();
            dto.id = route.getId();
            dto.name = route.getName();
            dto.description = route.getDescription();
            dto.color = route.getColor();
            dto.estimatedDuration = route.getEstimatedDuration();
            dto.baseFare = route.getBaseFare();
            dto.isActive = route.isIsActive();
            
            System.out.println("Fetching stops for route...");
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
            
            System.out.println("Fetching operating hours for route...");
            List<RouteOperatingHour> hours = routeOperatingHourRepository.findByRouteId(route.getId());
            dto.operatingHours = hours.stream().map(h -> {
                RouteWithStopsAndHoursDTO.RouteOperatingHourDTO hourDto = new RouteWithStopsAndHoursDTO.RouteOperatingHourDTO();
                hourDto.id = h.getId();
                hourDto.dayOfWeek = h.getDayOfWeek();
                hourDto.startTime = h.getStartTime().toString();
                hourDto.endTime = h.getEndTime().toString();
                return hourDto;
            }).collect(Collectors.toList());
            
            System.out.println("Getting shuttle name for route...");
            dto.shuttleName = getShuttleNameForRoute(route.getId());
            
            System.out.println("DTO assembly completed successfully");
            return dto;
            
        } catch (Exception e) {
            System.err.println("Error in assembleRouteDTO: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public List<RoutePlanOptionDTO> planRoutes(UUID startStopId, UUID endStopId, LocalDateTime departureTime, int maxTransfers) {
        // 1. Determine search window
        LocalDate today = LocalDate.now();
        LocalDateTime searchStart, searchEnd;
        if (departureTime != null) {
            searchStart = departureTime;
            searchEnd = departureTime.toLocalDate().atTime(LocalTime.MAX);
        } else {
            searchStart = today.atStartOfDay();
            searchEnd = today.atTime(LocalTime.MAX);
        }
        int weekday = searchStart.getDayOfWeek().getValue() % 7;
        // 2. Load static graph data (segments)
        String segSql = "SELECT rs.route_id, rs.stop_id AS from_stop, lead(rs.stop_id) OVER (PARTITION BY rs.route_id ORDER BY rs.stop_order) AS to_stop, rs.distance_from_previous, rs.avg_travel_time_min, r.base_fare, ph.multiplier FROM route_stops rs JOIN routes r ON r.id = rs.route_id AND r.is_active LEFT JOIN peak_hours ph ON ph.route_id = r.id LEFT JOIN peak_hour_days phd ON phd.peak_hour_id = ph.id AND phd.day_of_week = ? WHERE rs.stop_order > 0";
        List<Map<String, Object>> segments = jdbcTemplate.queryForList(segSql, weekday);
        // 3. Load shuttle availability & occupancy
        String shuttleSql = "SELECT sr.route_id, s.id AS shuttle_id, s.capacity, co.occupied_seats, sr.created_at FROM shuttle_routes sr JOIN shuttles s ON s.id = sr.shuttle_id JOIN shuttle_occupancy co ON co.shuttle_id = s.id AND co.recorded_at = (SELECT MAX(recorded_at) FROM shuttle_occupancy WHERE shuttle_id = s.id) JOIN route_operating_hours roh ON roh.route_id = sr.route_id AND roh.day_of_week = ? AND roh.start_time <= ? AND roh.end_time >= ?";
        List<Map<String, Object>> shuttles = jdbcTemplate.queryForList(shuttleSql, weekday, searchEnd.toLocalTime(), searchStart.toLocalTime());
        // 4. Build in-memory graph
        Map<UUID, List<Map<String, Object>>> graph = new HashMap<>();
        for (Map<String, Object> seg : segments) {
            UUID from = (UUID) seg.get("from_stop");
            UUID to = (UUID) seg.get("to_stop");
            if (to == null) continue;
            graph.computeIfAbsent(from, k -> new ArrayList<>()).add(seg);
        }
        // 5. Enumerate all simple paths
        List<List<UUID>> paths = new ArrayList<>();
        findAllPaths(graph, startStopId, endStopId, maxTransfers + 1, new ArrayList<>(), paths);
        // 6. For each path, calculate metrics and build DTO
        List<RoutePlanOptionDTO> options = new ArrayList<>();
        // Build lookup maps for route names and shuttle numbers
        Map<UUID, String> routeNames = new HashMap<>();
        jdbcTemplate.query("SELECT id, name FROM routes", rs -> {
            routeNames.put(UUID.fromString(rs.getString("id")), rs.getString("name"));
        });
        Map<UUID, String> shuttleNos = new HashMap<>();
        jdbcTemplate.query("SELECT id, shuttle_no FROM shuttles", rs -> {
            shuttleNos.put(UUID.fromString(rs.getString("id")), rs.getString("shuttle_no"));
        });
        for (List<UUID> path : paths) {
            double totalDist = 0, totalCost = 0, maxCrowd = 0;
            int totalTime = 0;
            List<RoutePlanOptionDTO.LegDetail> legs = new ArrayList<>();
            for (int i = 0; i < path.size() - 1; i++) {
                UUID u = path.get(i), v = path.get(i + 1);
                Map<String, Object> edge = graph.get(u).stream().filter(e -> v.equals(e.get("to_stop"))).findFirst().orElse(null);
                if (edge == null) continue;
                double dist = edge.get("distance_from_previous") != null ? ((Number) edge.get("distance_from_previous")).doubleValue() : 0;
                int time = edge.get("avg_travel_time_min") != null ? ((Number) edge.get("avg_travel_time_min")).intValue() : 0;
                double baseFare = edge.get("base_fare") != null ? ((Number) edge.get("base_fare")).doubleValue() : 0;
                double multiplier = edge.get("multiplier") != null ? ((Number) edge.get("multiplier")).doubleValue() : 1.0;
                UUID routeId = (UUID) edge.get("route_id");
                double routeTotalDist = segments.stream().filter(s -> routeId.equals(s.get("route_id"))).mapToDouble(s -> s.get("distance_from_previous") != null ? ((Number) s.get("distance_from_previous")).doubleValue() : 0).sum();
                double costPerKm = routeTotalDist > 0 ? baseFare / routeTotalDist : 0;
                double legCost = dist * costPerKm * multiplier;
                // Find best shuttle for this leg
                List<Map<String, Object>> matchingShuttles = shuttles.stream().filter(s -> routeId.equals(s.get("route_id")) && ((Number)s.get("occupied_seats")).intValue() < ((Number)s.get("capacity")).intValue()).toList();
                Map<String, Object> bestShuttle = matchingShuttles.stream().min(Comparator.comparingDouble(s -> ((Number)s.get("occupied_seats")).doubleValue() / ((Number)s.get("capacity")).doubleValue())).orElse(null);
                double crowdRatio = bestShuttle != null ? ((Number)bestShuttle.get("occupied_seats")).doubleValue() / ((Number)bestShuttle.get("capacity")).doubleValue() : 0;
                maxCrowd = Math.max(maxCrowd, crowdRatio);
                totalDist += dist;
                totalTime += time;
                totalCost += legCost;
                RoutePlanOptionDTO.LegDetail leg = new RoutePlanOptionDTO.LegDetail();
                leg.route_id = routeId;
                leg.route_name = routeNames.get(routeId);
                leg.shuttle_id = bestShuttle != null ? (UUID) bestShuttle.get("shuttle_id") : null;
                leg.shuttle_no = bestShuttle != null ? shuttleNos.get((UUID) bestShuttle.get("shuttle_id")) : null;
                leg.from = u;
                leg.to = v;
                leg.time = time;
                leg.distance = dist;
                leg.cost = legCost;
                leg.crowd = crowdRatio;
                legs.add(leg);
            }
            RoutePlanOptionDTO opt = new RoutePlanOptionDTO();
            opt.stops = path;
            opt.legs = legs;
            opt.total_distance = Math.round(totalDist * 100.0) / 100.0;
            opt.total_time = totalTime;
            opt.total_cost = Math.round(totalCost * 100.0) / 100.0;
            opt.max_crowding = Math.round(maxCrowd * 100.0) / 100.0;
            options.add(opt);
        }
        return options;
    }

    // Helper to enumerate all simple paths up to maxHops
    private void findAllPaths(Map<UUID, List<Map<String, Object>>> graph, UUID current, UUID end, int maxHops, List<UUID> path, List<List<UUID>> result) {
        path.add(current);
        if (current.equals(end)) {
            result.add(new ArrayList<>(path));
        } else if (path.size() < maxHops) {
            for (Map<String, Object> edge : graph.getOrDefault(current, List.of())) {
                UUID next = (UUID) edge.get("to_stop");
                if (!path.contains(next)) {
                    findAllPaths(graph, next, end, maxHops, path, result);
                }
            }
        }
        path.remove(path.size() - 1);
    }

    // Separate method to handle shuttle mapping
    @Transactional
    public void assignShuttleToRoute(String shuttleName, UUID routeId) {
        if (shuttleName != null && !shuttleName.isEmpty()) {
            Shuttle shuttle = shuttleRepository.findByShuttleNo(shuttleName);
            if (shuttle != null) {
                jdbcTemplate.update(
                    "INSERT INTO shuttle_routes (shuttle_id, route_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    shuttle.getId(), routeId
                );
                System.out.println("Shuttle mapping created successfully");
            } else {
                System.out.println("Warning: Shuttle with number '" + shuttleName + "' not found.");
            }
        }
    }
}
