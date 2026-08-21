package com.optimizer.backend.graph;

import com.optimizer.backend.Entity.TransportMode;
import com.optimizer.backend.Entity.TransportModeType;

/**
 * Represents a single directed edge in the transportation graph.
 *
 * <p>Each edge corresponds to one route between two cities via a specific
 * transport mode. Cost, time, and carbon values are pre-computed at graph
 * loading time using {@link com.optimizer.backend.Service.CostCalculator}
 * so that no database access is needed during pathfinding.</p>
 *
 * @param sourceId      ID of the source city
 * @param destinationId ID of the destination city
 * @param mode          transport mode (ROAD, RAIL, AIR)
 * @param distance      route distance in km
 * @param cost          pre-computed transport cost (weight-aware)
 * @param time          pre-computed travel time in hours
 * @param carbon        pre-computed carbon emissions in kg CO₂
 */
public record GraphEdge(
        Long sourceId,
        Long destinationId,
        TransportMode mode,
        double distance,
        double cost,
        double time,
        double carbon
) {
    public TransportModeType modeType() {
        return mode.getName();
    }
}
