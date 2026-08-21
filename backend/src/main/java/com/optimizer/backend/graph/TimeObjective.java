package com.optimizer.backend.graph;

import com.optimizer.backend.Service.CostCalculator;

/**
 * FASTEST objective: minimize total travel time.
 *
 * <h3>Edge scoring</h3>
 * <pre>score = distance / speed</pre>
 * This matches {@link CostCalculator#calculateTime} exactly. Time is not
 * affected by shipment weight.
 *
 * <h3>Heuristic (admissible)</h3>
 * <pre>h(n) = haversine(n, goal) / maxSpeed</pre>
 *
 * <p><strong>Why admissible:</strong> haversine is a lower bound on route distance.
 * Dividing by the maximum speed across all modes gives the minimum possible travel
 * time for that distance. No actual path can be faster than traveling the straight-line
 * distance at maximum speed. Therefore h(n) ≤ h*(n).</p>
 *
 * <p>Optimality guarantee: ✅ (consistent heuristic → A* finds optimal path)</p>
 */
public class TimeObjective implements ObjectiveFunction {

    @Override
    public double edgeScore(GraphEdge edge, double weight) {
        return CostCalculator.calculateTime(edge.distance(), edge.mode().getSpeed());
    }

    @Override
    public double heuristic(double fromCityLat, double fromCityLon,
                            double toCityLat, double toCityLon,
                            TransportGraph graph, double weight) {
        double haversineDistance = CostCalculator.haversine(fromCityLat, fromCityLon, toCityLat, toCityLon);
        // Lower bound: shortest distance / fastest speed
        return haversineDistance / graph.maxSpeed();
    }

    @Override
    public String name() {
        return "FASTEST";
    }
}
