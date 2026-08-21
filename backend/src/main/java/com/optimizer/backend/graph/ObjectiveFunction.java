package com.optimizer.backend.graph;

/**
 * Defines how edges are scored during pathfinding and provides an admissible
 * heuristic for A*.
 *
 * <p>Each optimization objective (CHEAPEST, FASTEST, GREENEST, BALANCED)
 * implements this interface with its own edge scoring formula and heuristic.</p>
 *
 * <p><strong>Edge scoring:</strong> Returns the cost of traversing a single edge
 * according to this objective. Both A* (for g-cost) and Dijkstra use this method.</p>
 *
 * <p><strong>Heuristic:</strong> Returns a lower bound on the remaining cost from
 * a city to the destination. Used ONLY by A* (Dijkstra ignores it). Must be
 * admissible: h(n) ≤ true cost from n to goal for all n.</p>
 */
public interface ObjectiveFunction {

    /**
     * Score a single edge for path accumulation (g-cost in A*, sole cost in Dijkstra).
     *
     * @param edge   the edge being traversed
     * @param weight shipment weight in kg (may be needed for cost calculations)
     * @return the objective-specific cost of this edge (non-negative)
     */
    double edgeScore(GraphEdge edge, double weight);

    /**
     * Admissible heuristic: lower bound on remaining cost from a city to the destination.
     *
     * <p>For A*, this must satisfy h(n) ≤ h*(n) for all n, where h*(n) is the
     * true minimum cost from n to the goal. If no valid heuristic exists,
     * return 0 (which degrades A* to Dijkstra but preserves optimality).</p>
     *
     * @param fromCityLat  latitude of the current city
     * @param fromCityLon  longitude of the current city
     * @param toCityLat    latitude of the destination city
     * @param toCityLon    longitude of the destination city
     * @param graph        the transport graph (for aggregate statistics)
     * @param weight       shipment weight in kg
     * @return admissible heuristic value (non-negative, ≤ true optimal cost)
     */
    double heuristic(double fromCityLat, double fromCityLon,
                     double toCityLat, double toCityLon,
                     TransportGraph graph, double weight);

    /**
     * Human-readable name for logging and comparison output.
     */
    String name();
}
