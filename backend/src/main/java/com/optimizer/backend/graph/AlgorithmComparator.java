package com.optimizer.backend.graph;

/**
 * Compares A* and Dijkstra algorithms on the same graph, source, destination,
 * objective, and weight.
 *
 * <p>Runs both algorithms and reports execution time, nodes explored, and
 * whether the resulting objective values are equivalent (within floating-point
 * tolerance). Also reports whether the edge sequences are identical.</p>
 *
 * <h3>Benchmarking Notes</h3>
 * <p>This is a single-run comparison, not a statistically rigorous benchmark.
 * The timing is approximate and affected by JVM warmup state, GC pauses,
 * and OS scheduling. For rigorous benchmarks, see {@code AlgorithmBenchmarkTest}
 * which uses warmup iterations and alternating order.</p>
 *
 * <p>The order of execution is alternated to partially mitigate ordering bias:
 * the first run executes A* then Dijkstra; a warmup pass executes Dijkstra
 * then A*.</p>
 */
public class AlgorithmComparator {

    private static final double OBJECTIVE_TOLERANCE = 1e-6;
    private static final int WARMUP_RUNS = 5;

    private final AStarPathfinder aStar;
    private final DijkstraPathfinder dijkstra;

    public AlgorithmComparator() {
        this.aStar = new AStarPathfinder();
        this.dijkstra = new DijkstraPathfinder();
    }

    /**
     * Run both algorithms on the same inputs and compare results.
     *
     * <p>Includes a small warmup (WARMUP_RUNS iterations with alternating order)
     * before the timed comparison to reduce JIT and cache effects.</p>
     *
     * @param graph     the transportation graph
     * @param sourceId  source city ID
     * @param destId    destination city ID
     * @param objective the optimization objective
     * @param weight    shipment weight in kg
     * @return comparison results
     */
    public ComparisonResult compare(TransportGraph graph, Long sourceId, Long destId,
                                    ObjectiveFunction objective, double weight) {
        // Warmup: run both algorithms to trigger JIT, alternating order
        for (int i = 0; i < WARMUP_RUNS; i++) {
            if (i % 2 == 0) {
                dijkstra.findPath(graph, sourceId, destId, objective, weight);
                aStar.findPath(graph, sourceId, destId, objective, weight);
            } else {
                aStar.findPath(graph, sourceId, destId, objective, weight);
                dijkstra.findPath(graph, sourceId, destId, objective, weight);
            }
        }

        // Timed comparison: alternate order on this single run
        // Even warmup count: A* first; odd: Dijkstra first
        PathResult aStarResult, dijkstraResult;
        long aStarTimeNanos, dijkstraTimeNanos;

        if (WARMUP_RUNS % 2 == 0) {
            // A* first
            long t1 = System.nanoTime();
            aStarResult = aStar.findPath(graph, sourceId, destId, objective, weight);
            aStarTimeNanos = System.nanoTime() - t1;

            long t2 = System.nanoTime();
            dijkstraResult = dijkstra.findPath(graph, sourceId, destId, objective, weight);
            dijkstraTimeNanos = System.nanoTime() - t2;
        } else {
            // Dijkstra first
            long t1 = System.nanoTime();
            dijkstraResult = dijkstra.findPath(graph, sourceId, destId, objective, weight);
            dijkstraTimeNanos = System.nanoTime() - t1;

            long t2 = System.nanoTime();
            aStarResult = aStar.findPath(graph, sourceId, destId, objective, weight);
            aStarTimeNanos = System.nanoTime() - t2;
        }

        // Compare objective values
        boolean equivalentCost = Math.abs(aStarResult.totalCost() - dijkstraResult.totalCost()) < OBJECTIVE_TOLERANCE;
        boolean equivalentTime = Math.abs(aStarResult.totalTime() - dijkstraResult.totalTime()) < OBJECTIVE_TOLERANCE;
        boolean equivalentCarbon = Math.abs(aStarResult.totalCarbon() - dijkstraResult.totalCarbon()) < OBJECTIVE_TOLERANCE;
        boolean equivalentDistance = Math.abs(aStarResult.totalDistance() - dijkstraResult.totalDistance()) < OBJECTIVE_TOLERANCE;

        // Compare edge sequences
        boolean identicalEdges = edgesEqual(aStarResult.edges(), dijkstraResult.edges());

        return new ComparisonResult(
                aStarResult, dijkstraResult,
                aStarTimeNanos, dijkstraTimeNanos,
                equivalentCost && equivalentTime && equivalentCarbon && equivalentDistance,
                identicalEdges
        );
    }

    private boolean edgesEqual(java.util.List<GraphEdge> a, java.util.List<GraphEdge> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).sourceId().equals(b.get(i).sourceId()) ||
                !a.get(i).destinationId().equals(b.get(i).destinationId()) ||
                a.get(i).modeType() != b.get(i).modeType()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Comparison result containing both algorithms' outputs and timing.
     *
     * <p>Note: timing values are approximate (single run, not statistically
     * rigorous). The reliable fields are {@code equivalentObjective} and
     * {@code identicalEdges}.</p>
     */
    public record ComparisonResult(
            PathResult aStarResult,
            PathResult dijkstraResult,
            long aStarTimeNanos,
            long dijkstraTimeNanos,
            boolean equivalentObjective,
            boolean identicalEdges
    ) {
        public double aStarTimeMs() {
            return aStarTimeNanos / 1_000_000.0;
        }

        public double dijkstraTimeMs() {
            return dijkstraTimeNanos / 1_000_000.0;
        }

        @Override
        public String toString() {
            return String.format(
                    "=== A* vs Dijkstra Comparison ===%n" +
                    "A*       : %.3f ms, %d nodes explored, cost=%.2f, time=%.2f, carbon=%.4f%n" +
                    "Dijkstra : %.3f ms, %d nodes explored, cost=%.2f, time=%.2f, carbon=%.4f%n" +
                    "Equivalent objective: %s%n" +
                    "Identical edges: %s%n" +
                    "(Note: timing is approximate, single run)",
                    aStarTimeMs(), aStarResult.nodesExplored(),
                    aStarResult.totalCost(), aStarResult.totalTime(), aStarResult.totalCarbon(),
                    dijkstraTimeMs(), dijkstraResult.nodesExplored(),
                    dijkstraResult.totalCost(), dijkstraResult.totalTime(), dijkstraResult.totalCarbon(),
                    equivalentObjective, identicalEdges
            );
        }
    }
}
