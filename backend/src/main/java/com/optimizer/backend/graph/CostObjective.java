package com.optimizer.backend.graph;

import com.optimizer.backend.Service.CostCalculator;

/**
 * CHEAPEST objective: minimize total transportation cost.
 *
 * <h3>Edge scoring</h3>
 * <pre>score = distance × costPerKm × (1 + weightKg × WEIGHT_FACTOR)</pre>
 * This matches {@link CostCalculator#calculateCost} exactly.
 *
 * <h3>Heuristic (admissible)</h3>
 * <pre>h(n) = haversine(n, goal) × minCostPerKm × (1 + weightKg × WEIGHT_FACTOR)</pre>
 *
 * <p><strong>Why admissible:</strong> haversine distance is a lower bound on actual
 * route distance (straight line ≤ any path). Multiplying by the minimum costPerKm
 * across all modes gives the cheapest possible cost per km, which is ≤ the actual
 * cost on the optimal path. The weight factor is applied identically, so the
 * heuristic is a consistent lower bound on the remaining cost.</p>
 *
 * <p>Optimality guarantee: ✅ (consistent heuristic → A* finds optimal path)</p>
 */
public class CostObjective implements ObjectiveFunction {

    @Override
    public double edgeScore(GraphEdge edge, double weight) {
        return CostCalculator.calculateCost(edge.distance(), edge.mode().getCostPerKm(), weight);
    }

    @Override
    public double heuristic(double fromCityLat, double fromCityLon,
                            double toCityLat, double toCityLon,
                            TransportGraph graph, double weight) {
        double haversineDistance = CostCalculator.haversine(fromCityLat, fromCityLon, toCityLat, toCityLon);
        // Lower bound: shortest distance × cheapest rate
        return haversineDistance * graph.minCostPerKm() * (1 + weight * CostCalculator.WEIGHT_FACTOR);
    }

    @Override
    public String name() {
        return "CHEAPEST";
    }
}
