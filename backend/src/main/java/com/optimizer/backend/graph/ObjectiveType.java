package com.optimizer.backend.graph;

import com.optimizer.backend.Entity.OptimizationType;

/**
 * Bridges {@link OptimizationType} to the corresponding {@link ObjectiveFunction}.
 *
 * <p>This avoids scattered switch/if-else blocks when converting between
 * the API-facing enum and the pathfinding-level objective abstraction.</p>
 */
public final class ObjectiveType {

    private ObjectiveType() { }

    /**
     * Get the ObjectiveFunction for a given OptimizationType.
     *
     * @param type the optimization type from the API
     * @return the corresponding objective function
     */
    public static ObjectiveFunction resolve(OptimizationType type) {
        return switch (type) {
            case CHEAPEST -> new CostObjective();
            case FASTEST -> new TimeObjective();
            case GREENEST -> new CarbonObjective();
            case BALANCED -> new BalancedObjective();
        };
    }
}
