package com.optimizer.backend.graph;

import com.optimizer.backend.Entity.City;
import com.optimizer.backend.Entity.TransportMode;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * In-memory transportation graph used by pathfinding algorithms.
 *
 * <p>Nodes are cities (referenced by ID). Edges are {@link GraphEdge} instances
 * grouped by source city ID. This class also holds pre-computed aggregate
 * statistics from the transport modes, which are used to build admissible
 * A* heuristics.</p>
 *
 * <p>No database access occurs through this class. The graph is constructed
 * once by {@link TransportGraphLoader} and reused for all algorithms and
 * objectives within a single optimization request.</p>
 *
 * @param adjacency      adjacency list: source city ID → list of outgoing edges
 * @param citiesById     all cities in the graph, keyed by ID
 * @param avgCostPerKm   average costPerKm across all transport modes
 * @param avgSpeed       average speed across all transport modes
 * @param minCostPerKm   minimum costPerKm (used for CHEAPEST heuristic lower bound)
 * @param maxSpeed       maximum speed (used for FASTEST heuristic lower bound)
 * @param minCarbonPerTonKm  minimum carbon emission factor (used for GREENEST heuristic lower bound)
 */
public record TransportGraph(
        Map<Long, List<GraphEdge>> adjacency,
        Map<Long, City> citiesById,
        double avgCostPerKm,
        double avgSpeed,
        double minCostPerKm,
        double maxSpeed,
        double minCarbonPerTonKm
) {
    /**
     * Get outgoing edges for a city, or an empty list if the city has no outgoing routes.
     */
    public List<GraphEdge> getEdges(Long cityId) {
        return adjacency.getOrDefault(cityId, Collections.emptyList());
    }

    /**
     * Check if a city exists in the graph.
     */
    public boolean containsCity(Long cityId) {
        return citiesById.containsKey(cityId);
    }

    /**
     * Get a city by ID.
     */
    public City getCity(Long cityId) {
        return citiesById.get(cityId);
    }

    /**
     * Total number of edges in the graph.
     */
    public int edgeCount() {
        return adjacency.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Total number of nodes (cities) in the graph.
     */
    public int nodeCount() {
        return citiesById.size();
    }
}
