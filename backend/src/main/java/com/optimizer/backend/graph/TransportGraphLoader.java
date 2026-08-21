package com.optimizer.backend.graph;

import com.optimizer.backend.Entity.City;
import com.optimizer.backend.Entity.Route;
import com.optimizer.backend.Entity.TransportMode;
import com.optimizer.backend.Exception.BadRequestException;
import com.optimizer.backend.Repository.RouteRepository;
import com.optimizer.backend.Repository.TransportModeRepository;
import com.optimizer.backend.Repository.CityRepository;
import com.optimizer.backend.Service.CostCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the transportation graph from the database into memory.
 *
 * <p>This service performs exactly 2 database queries ({@code routeRepository.findAll()}
 * and {@code transportModeRepository.findAll()}) per call. The returned
 * {@link TransportGraph} is a complete in-memory snapshot that requires no
 * further database access during pathfinding.</p>
 *
 * <p>The caller is responsible for calling this once per optimization request
 * and reusing the same graph instance for all algorithms and objectives.</p>
 */
@Service
@RequiredArgsConstructor
public class TransportGraphLoader {

    private final RouteRepository routeRepository;
    private final TransportModeRepository transportModeRepository;
    private final CityRepository cityRepository;

    /**
     * Load the transportation graph from the database.
     *
     * @param weight shipment weight in kg (used to pre-compute cost on each edge)
     * @return a fully constructed in-memory graph
     * @throws BadRequestException if no routes exist in the database
     */
    @Transactional(readOnly = true)
    public TransportGraph loadGraph(double weight) {
        List<Route> routes = routeRepository.findAll();
        if (routes.isEmpty()) {
            throw new BadRequestException("No routes are available for optimization");
        }

        List<TransportMode> modes = transportModeRepository.findAll();
        List<City> cities = cityRepository.findAll();

        // Build city lookup
        Map<Long, City> citiesById = new HashMap<>();
        for (City city : cities) {
            citiesById.put(city.getId(), city);
        }

        // Build adjacency list with pre-computed edge metrics
        Map<Long, List<GraphEdge>> adjacency = new HashMap<>();
        for (Route r : routes) {
            GraphEdge edge = new GraphEdge(
                    r.getSourceCity().getId(),
                    r.getDestinationCity().getId(),
                    r.getTransportMode(),
                    r.getDistance(),
                    CostCalculator.calculateCost(r.getDistance(),
                            r.getTransportMode().getCostPerKm(), weight),
                    CostCalculator.calculateTime(r.getDistance(),
                            r.getTransportMode().getSpeed()),
                    CostCalculator.calculateCarbon(r.getDistance(), weight,
                            r.getTransportMode().getCarbonPerTonKm())
            );
            adjacency.computeIfAbsent(edge.sourceId(), k -> new ArrayList<>()).add(edge);
        }

        // Compute aggregate statistics from transport modes (for heuristics)
        double avgCostPerKm = modes.stream()
                .mapToDouble(TransportMode::getCostPerKm).average().orElse(0.0);
        double avgSpeed = modes.stream()
                .mapToDouble(TransportMode::getSpeed).average().orElse(1.0);
        double minCostPerKm = modes.stream()
                .mapToDouble(TransportMode::getCostPerKm).min().orElse(0.0);
        double maxSpeed = modes.stream()
                .mapToDouble(TransportMode::getSpeed).max().orElse(1.0);
        double minCarbonPerTonKm = modes.stream()
                .mapToDouble(TransportMode::getCarbonPerTonKm).min().orElse(0.0);

        return new TransportGraph(
                adjacency, citiesById,
                avgCostPerKm, avgSpeed,
                minCostPerKm, maxSpeed, minCarbonPerTonKm
        );
    }
}
