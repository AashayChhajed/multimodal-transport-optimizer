package com.optimizer.backend.graph;

import com.optimizer.backend.Service.CostCalculator;

/**
 * GREENEST objective: minimize total carbon emissions.
 *
 * <h3>Edge scoring</h3>
 * <pre>score = distance × (weightKg / 1000) × carbonPerTonKm</pre>
 * This matches {@link CostCalculator#calculateCarbon} exactly.
 *
 * <h3>Heuristic (admissible)</h3>
 * <pre>h(n) = haversine(n, goal) × (weightKg / 1000) × minCarbonPerTonKm</pre>
 *
 * <p><strong>Why admissible:</strong> haversine is a lower bound on route distance.
 * Multiplying by the minimum carbon emission factor across all modes gives the
 * minimum possible emissions for that distance. No actual path can be greener than
 * traveling the straight-line distance using the lowest-emission mode. Therefore
 * h(n) ≤ h*(n).</p>
 *
 * <p>Optimality guarantee: ✅ (consistent heuristic → A* finds optimal path)</p>
 */
public class CarbonObjective implements ObjectiveFunction {

    @Override
    public double edgeScore(GraphEdge edge, double weight) {
        return CostCalculator.calculateCarbon(edge.distance(), weight, edge.mode().getCarbonPerTonKm());
    }

    @Override
    public double heuristic(double fromCityLat, double fromCityLon,
                            double toCityLat, double toCityLon,
                            TransportGraph graph, double weight) {
        double haversineDistance = CostCalculator.haversine(fromCityLat, fromCityLon, toCityLat, toCityLon);
        // Lower bound: shortest distance × shipment weight × minimum emission factor
        return haversineDistance * (weight / 1000.0) * graph.minCarbonPerTonKm();
    }

    @Override
    public String name() {
        return "GREENEST";
    }
}
