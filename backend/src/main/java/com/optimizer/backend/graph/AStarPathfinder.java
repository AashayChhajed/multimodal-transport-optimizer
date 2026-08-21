package com.optimizer.backend.graph;

import com.optimizer.backend.Entity.City;

import java.util.*;

/**
 * A* shortest-path algorithm for the transportation graph.
 *
 * <p>Uses the {@link ObjectiveFunction} abstraction for both edge scoring (g-cost)
 * and heuristic (h-cost). The heuristic is provided by the objective and is
 * guaranteed to be admissible for CHEAPEST, FASTEST, and GREENEST. For BALANCED,
 * the heuristic returns 0, making A* equivalent to Dijkstra for that objective.</p>
 *
 * <h3>Algorithm summary</h3>
 * <ol>
 *   <li>Start with source node, f = h(source)</li>
 *   <li>At each step, expand the node with lowest f = g + h</li>
 *   <li>For each neighbor, compute tentative g = g(current) + edgeScore(edge)</li>
 *   <li>If tentative g < known g for neighbor, update and set f = g + h(neighbor)</li>
 *   <li>Repeat until destination is expanded or open set is empty</li>
 * </ol>
 *
 * <p>Nodes explored count includes all nodes that are removed from the open set
 * (i.e., their neighbors are examined), which is the standard metric for
 * algorithm comparison.</p>
 */
public class AStarPathfinder implements PathfindingAlgorithm {

    @Override
    public PathResult findPath(TransportGraph graph, Long sourceId, Long destId,
                               ObjectiveFunction objective, double weight) {
        // Priority queue: min-heap by f-score (g + h)
        PriorityQueue<NodeEntry> openSet = new PriorityQueue<>(
                Comparator.comparingDouble(NodeEntry::fScore));
        Set<Long> closedSet = new HashSet<>();

        // g-scores: cost from source to each node
        Map<Long, Double> gScore = new HashMap<>();
        // Reconstruction: predecessor tracking
        Map<Long, Long> cameFrom = new HashMap<>();
        Map<Long, GraphEdge> cameFromEdge = new HashMap<>();

        // Initialize source
        gScore.put(sourceId, 0.0);
        City sourceCity = graph.getCity(sourceId);
        City destCity = graph.getCity(destId);

        double h = objective.heuristic(
                sourceCity.getLatitude(), sourceCity.getLongitude(),
                destCity.getLatitude(), destCity.getLongitude(),
                graph, weight);
        openSet.add(new NodeEntry(sourceId, h));

        int nodesExplored = 0;

        while (!openSet.isEmpty()) {
            NodeEntry current = openSet.poll();
            Long currentId = current.cityId();

            // Skip if already processed (duplicates in PQ due to lazy deletion)
            if (closedSet.contains(currentId)) {
                continue;
            }

            closedSet.add(currentId);
            nodesExplored++;

            // Goal check
            if (currentId.equals(destId)) {
                return reconstructPath(cameFrom, cameFromEdge, sourceId, destId, nodesExplored);
            }

            // Expand neighbors
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

                    City neighborCity = graph.getCity(neighborId);
                    double neighborH = objective.heuristic(
                            neighborCity.getLatitude(), neighborCity.getLongitude(),
                            destCity.getLatitude(), destCity.getLongitude(),
                            graph, weight);

                    openSet.add(new NodeEntry(neighborId, tentativeG + neighborH));
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
        return "ASTAR";
    }

    private record NodeEntry(Long cityId, double fScore) {}
}
