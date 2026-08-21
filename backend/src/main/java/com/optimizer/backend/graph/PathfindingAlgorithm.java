package com.optimizer.backend.graph;

/**
 * Interface for pathfinding algorithms operating on a {@link TransportGraph}.
 *
 * <p>Both A* and Dijkstra implement this interface. The algorithm selection
 * is made at the service layer; the algorithms themselves are interchangeable
 * through this contract.</p>
 */
public interface PathfindingAlgorithm {

    /**
     * Find the optimal path from source to destination.
     *
     * @param graph     the in-memory transportation graph
     * @param sourceId  ID of the source city
     * @param destId    ID of the destination city
     * @param objective the optimization objective (determines edge scoring and heuristic)
     * @param weight    shipment weight in kg
     * @return a {@link PathResult} with the path and metrics, or noPath() if unreachable
     */
    PathResult findPath(TransportGraph graph, Long sourceId, Long destId,
                        ObjectiveFunction objective, double weight);

    /**
     * Human-readable algorithm name.
     */
    String name();
}
