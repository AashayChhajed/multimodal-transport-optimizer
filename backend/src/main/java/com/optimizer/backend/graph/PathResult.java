package com.optimizer.backend.graph;

import java.util.Collections;
import java.util.List;

/**
 * Result of a pathfinding algorithm invocation.
 *
 * <p>Contains the ordered edges of the found path and pre-computed totals
 * for distance, cost, time, carbon, and nodes explored. This is the single
 * source of truth for path metrics — no recalculation should be needed
 * at higher layers.</p>
 *
 * <p>If no path exists, {@link #noPath()} returns a result with empty edges
 * and zero totals.</p>
 *
 * @param edges         ordered edges from source to destination (empty if no path)
 * @param totalDistance  total distance in km
 * @param totalCost      total cost in currency units
 * @param totalTime      total time in hours (excluding transfer time — added by caller)
 * @param totalCarbon    total carbon emissions in kg CO₂
 * @param nodesExplored  number of nodes explored during search (for benchmarking)
 */
public record PathResult(
        List<GraphEdge> edges,
        double totalDistance,
        double totalCost,
        double totalTime,
        double totalCarbon,
        int nodesExplored
) {
    /**
     * A result representing "no path found".
     */
    public static PathResult noPath(int nodesExplored) {
        return new PathResult(
                Collections.emptyList(),
                0.0, 0.0, 0.0, 0.0,
                nodesExplored
        );
    }

    /**
     * Check if a valid path was found.
     */
    public boolean hasPath() {
        return !edges.isEmpty();
    }
}
