package com.optimizer.backend.graph;

/**
 * BALANCED objective: combine normalized cost, time, and carbon emissions.
 *
 * <h3>Normalization</h3>
 * <p>Each raw value is normalized against the worst single-leg value in the graph
 * so that all three dimensions contribute equally to the score:</p>
 * <pre>
 * normalizedCost   = cost   / maxCostPerLeg
 * normalizedTime   = time   / maxTimePerLeg
 * normalizedCarbon = carbon / maxCarbonPerLeg
 * </pre>
 *
 * <p>{@code maxCostPerLeg}, {@code maxTimePerLeg}, and {@code maxCarbonPerLeg}
 * are computed at graph loading time from the single most expensive edge in the
 * graph. This ensures normalization is consistent within a single graph instance.</p>
 *
 * <h3>Weights</h3>
 * <pre>score = 0.33 × normalizedCost + 0.33 × normalizedTime + 0.34 × normalizedCarbon</pre>
 * <p>Carbon is given a slight extra weight (0.34 vs 0.33) to reflect the growing
 * importance of environmental impact. All three weights sum to 1.0.</p>
 *
 * <h3>Heuristic</h3>
 * <p><strong>h(n) = 0 (zero heuristic).</strong></p>
 *
 * <p><strong>Why zero:</strong> A valid admissible heuristic for the normalized
 * BALANCED objective would require proving that a lower bound on the normalized
 * sum of cost, time, and carbon can be computed from geographic distance alone.
 * The normalization constants depend on the worst edge in the graph, making it
 * difficult to derive a tight lower bound that is always ≤ the true normalized
 * cost-to-go. Using h(n) = 0 is always admissible (zero is trivially ≤ any
 * non-negative cost) and preserves the optimality guarantee. The trade-off is
 * that A* explores more nodes than with a non-trivial heuristic, effectively
 * performing like Dijkstra for this objective. This is an acceptable trade-off
 * for correctness.</p>
 *
 * <p>Optimality guarantee: ✅ (h=0 is always admissible → A* finds optimal path)</p>
 */
public class BalancedObjective implements ObjectiveFunction {

    private static final double W_COST = 0.33;
    private static final double W_TIME = 0.33;
    private static final double W_CARBON = 0.34;

    // Normalization bounds (computed per-graph in TransportGraph)
    private double maxCostPerLeg = 1.0;
    private double maxTimePerLeg = 1.0;
    private double maxCarbonPerLeg = 1.0;

    /**
     * Set normalization bounds based on the graph's worst-case single-leg values.
     * Must be called before scoring edges.
     *
     * @param maxCost   maximum cost of any single edge in the graph
     * @param maxTime   maximum time of any single edge in the graph
     * @param maxCarbon maximum carbon of any single edge in the graph
     */
    public void setNormalizationBounds(double maxCost, double maxTime, double maxCarbon) {
        this.maxCostPerLeg = maxCost > 0 ? maxCost : 1.0;
        this.maxTimePerLeg = maxTime > 0 ? maxTime : 1.0;
        this.maxCarbonPerLeg = maxCarbon > 0 ? maxCarbon : 1.0;
    }

    @Override
    public double edgeScore(GraphEdge edge, double weight) {
        double normCost = edge.cost() / maxCostPerLeg;
        double normTime = edge.time() / maxTimePerLeg;
        double normCarbon = edge.carbon() / maxCarbonPerLeg;
        return W_COST * normCost + W_TIME * normTime + W_CARBON * normCarbon;
    }

    /**
     * Zero heuristic — always admissible.
     *
     * <p>See class-level documentation for rationale.</p>
     */
    @Override
    public double heuristic(double fromCityLat, double fromCityLon,
                            double toCityLat, double toCityLon,
                            TransportGraph graph, double weight) {
        return 0.0;
    }

    @Override
    public String name() {
        return "BALANCED";
    }
}
