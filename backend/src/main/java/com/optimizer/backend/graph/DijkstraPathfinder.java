package com.optimizer.backend.graph;

import java.util.*;

/**
 * Dijkstra's shortest-path algorithm for the transportation graph.
 *
 * <p>Uses the {@link ObjectiveFunction} abstraction for edge scoring.
 * Unlike A*, Dijkstra does not use a heuristic — it explores nodes purely
 * based on accumulated cost from the source.</p>
 *
 * <h3>Algorithm summary</h3>
 * <ol>
 *   <li>Start with source node, g = 0</li>
 *   <li>At each step, expand the node with lowest accumulated g-cost</li>
 *   <li>For each neighbor, compute tentative g = g(current) + edgeScore(edge)</li>
 *   <li>If tentative g < known g for neighbor, update and add to open set</li>
 *   <li>Repeat until destination is expanded or open set is empty</li>
 * </ol>
 *
 * <p>Dijkstra always finds the optimal path regardless of the objective function.
 * It explores more nodes than A* when a good heuristic is available (CHEAPEST,
 * FASTEST, GREENEST) but performs identically for BALANCED (where A* uses h=0).</p>
 */
public class DijkstraPathfinder implements PathfindingAlgorithm {

    @Override
    public PathResult findPath(TransportGraph graph, Long sourceId, Long destId,
                               ObjectiveFunction objective, double weight) {
        // Priority queue: min-heap by accumulated g-cost only
        PriorityQueue<NodeEntry> openSet = new PriorityQueue<>(
                Comparator.comparingDouble(NodeEntry::gScore));
        Set<Long> closedSet = new HashSet<>();

        Map<Long, Double> gScore = new HashMap<>();
        Map<Long, Long> cameFrom = new HashMap<>();
        Map<Long, GraphEdge> cameFromEdge = new HashMap<>();

        gScore.put(sourceId, 0.0);
        openSet.add(new NodeEntry(sourceId, 0.0));

        int nodesExplored = 0;

        while (!openSet.isEmpty()) {
            NodeEntry current = openSet.poll();
            Long currentId = current.cityId();

            if (closedSet.contains(currentId)) {
                continue;
            }

            closedSet.add(currentId);
            nodesExplored++;

            if (currentId.equals(destId)) {
                return reconstructPath(cameFrom, cameFromEdge, sourceId, destId, nodesExplored);
            }

            for (GraphEdge edge : graph.getEdges(currentId)) {
                Long neighborId = edge.destinationId();
                if (closedSet.contains(neighborId)) {
                    continue;
                }

                double tentativeG = gScore.getOrDefault(currentId, Double.MAX_VALUE)
                        + objective.edgeScore(edge, weight);

                if (tentativeG < gScore.getOrDefault(neighborId, Double.MAX_VALUE)) {
                    gScore.put(neighborId, tentativeG);
                    cameFrom.put(neighborId, currentId);
                    cameFromEdge.put(neighborId, edge);
                    openSet.add(new NodeEntry(neighborId, tentativeG));
                }
            }
        }

        return PathResult.noPath(nodesExplored);
    }

    private PathResult reconstructPath(Map<Long, Long> cameFrom,
                                       Map<Long, GraphEdge> cameFromEdge,
                                       Long sourceId, Long destId,
                                       int nodesExplored) {
        List<GraphEdge> edges = new ArrayList<>();
        Long current = destId;
        while (!current.equals(sourceId)) {
            GraphEdge edge = cameFromEdge.get(current);
            if (edge == null) {
                return PathResult.noPath(nodesExplored);
            }
            edges.add(edge);
            current = cameFrom.get(current);
        }
        Collections.reverse(edges);

        double totalDistance = edges.stream().mapToDouble(GraphEdge::distance).sum();
        double totalCost = edges.stream().mapToDouble(GraphEdge::cost).sum();
        double totalTime = edges.stream().mapToDouble(GraphEdge::time).sum();
        double totalCarbon = edges.stream().mapToDouble(GraphEdge::carbon).sum();

        return new PathResult(edges, totalDistance, totalCost, totalTime, totalCarbon, nodesExplored);
    }

    @Override
    public String name() {
        return "DIJKSTRA";
    }

    private record NodeEntry(Long cityId, double gScore) {}
}
