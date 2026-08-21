package com.optimizer.backend.graph;

import com.optimizer.backend.Entity.City;
import com.optimizer.backend.Entity.Route;
import com.optimizer.backend.Entity.TransportMode;
import com.optimizer.backend.Entity.TransportModeType;
import com.optimizer.backend.Service.CostCalculator;
import com.optimizer.backend.Repository.CityRepository;
import com.optimizer.backend.Repository.RouteRepository;
import com.optimizer.backend.Repository.TransportModeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Benchmark comparing A* and Dijkstra across all optimization objectives.
 *
 * Methodology:
 * - 20-city complete graph, 1140 edges, all 3 transport modes
 * - 200 warmup iterations to trigger JIT C1/C2 compilation before timing
 * - Alternating execution order (A* and Dijkstra swapped per iteration) to eliminate ordering bias
 * - 50 timed iterations per algorithm per objective
 * - Graph construction is excluded from timing (built before System.nanoTime())
 *
 * Documented Limitations:
 * - This is a lightweight demonstration benchmark, not a JMH-grade microbenchmark.
 * - GC pauses are not excluded; averaged over 50 iterations to dilute their effect.
 * - Wall-clock times are approximate and should not be used for precise performance claims.
 * - The reliable metrics are: (a) nodes explored ratio, (b) objective equivalence.
 * - For rigorous benchmarking, use JMH with profilers and GC control.
 */
@ExtendWith(MockitoExtension.class)
class AlgorithmBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 200;
    private static final int BENCH_ITERATIONS = 50;

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private TransportModeRepository transportModeRepository;
    @Mock
    private CityRepository cityRepository;

    private TransportGraphLoader graphLoader;
    private AStarPathfinder astar;
    private DijkstraPathfinder dijkstra;

    private TransportMode road, rail, air;

    @BeforeEach
    void setUp() {
        graphLoader = new TransportGraphLoader(routeRepository, transportModeRepository, cityRepository);
        astar = new AStarPathfinder();
        dijkstra = new DijkstraPathfinder();

        road = TransportMode.builder().id(1L).name(TransportModeType.ROAD)
                .costPerKm(1.2).speed(60).carbonPerTonKm(0.062).build();
        rail = TransportMode.builder().id(2L).name(TransportModeType.RAIL)
                .costPerKm(0.8).speed(90).carbonPerTonKm(0.022).build();
        air = TransportMode.builder().id(3L).name(TransportModeType.AIR)
                .costPerKm(3.0).speed(700).carbonPerTonKm(0.602).build();
    }

    private TransportGraph buildGraph20() {
        List<City> cities = new ArrayList<>();
        double baseLat = 10.0;
        double baseLon = 70.0;
        for (int i = 0; i < 20; i++) {
            cities.add(City.builder().id((long) (i + 1)).name("City" + (i + 1))
                    .latitude(baseLat + i * 2).longitude(baseLon + i * 3).build());
        }

        List<Route> routes = new ArrayList<>();
        long routeId = 1;
        for (City src : cities) {
            for (City dst : cities) {
                if (src.getId().equals(dst.getId())) continue;
                double dist = CostCalculator.haversine(src.getLatitude(), src.getLongitude(),
                        dst.getLatitude(), dst.getLongitude());
                for (TransportMode mode : List.of(road, rail, air)) {
                    routes.add(Route.builder().id(routeId++).sourceCity(src).destinationCity(dst)
                            .transportMode(mode).distance(dist).build());
                }
            }
        }

        when(routeRepository.findAll()).thenReturn(routes);
        when(transportModeRepository.findAll()).thenReturn(List.of(road, rail, air));
        when(cityRepository.findAll()).thenReturn(cities);

        return graphLoader.loadGraph(500);
    }

    private BalancedObjective createBalancedObjective(TransportGraph graph) {
        BalancedObjective balanced = new BalancedObjective();
        double maxCost = 0, maxTime = 0, maxCarbon = 0;
        for (var edges : graph.adjacency().values()) {
            for (GraphEdge e : edges) {
                maxCost = Math.max(maxCost, e.cost());
                maxTime = Math.max(maxTime, e.time());
                maxCarbon = Math.max(maxCarbon, e.carbon());
            }
        }
        balanced.setNormalizationBounds(maxCost, maxTime, maxCarbon);
        return balanced;
    }

    /**
     * Run both algorithms with alternating order to eliminate ordering bias.
     * On even iterations: A* first, then Dijkstra.
     * On odd iterations: Dijkstra first, then A*.
     */
    private double[] benchmarkAlternating(TransportGraph graph, Long src, Long dest,
                                          ObjectiveFunction objective, double weight, int iterations) {
        long aStarTotal = 0;
        long dijkstraTotal = 0;
        PathResult lastAStar = null;
        PathResult lastDijkstra = null;

        for (int i = 0; i < iterations; i++) {
            if (i % 2 == 0) {
                long t1 = System.nanoTime();
                lastAStar = astar.findPath(graph, src, dest, objective, weight);
                aStarTotal += System.nanoTime() - t1;

                long t2 = System.nanoTime();
                lastDijkstra = dijkstra.findPath(graph, src, dest, objective, weight);
                dijkstraTotal += System.nanoTime() - t2;
            } else {
                long t1 = System.nanoTime();
                lastDijkstra = dijkstra.findPath(graph, src, dest, objective, weight);
                dijkstraTotal += System.nanoTime() - t1;

                long t2 = System.nanoTime();
                lastAStar = astar.findPath(graph, src, dest, objective, weight);
                aStarTotal += System.nanoTime() - t2;
            }
        }

        // [aStarAvgMs, dijkstraAvgMs, aStarNodes, dijkstraNodes, aStarCost, dijkstraCost,
        //  aStarTime, dijkstraTime, aStarCarbon, dijkstraCarbon, aStarDist, dijkstraDist]
        return new double[]{
                (aStarTotal / 1_000_000.0) / iterations,
                (dijkstraTotal / 1_000_000.0) / iterations,
                lastAStar.nodesExplored(),
                lastDijkstra.nodesExplored(),
                lastAStar.totalCost(),
                lastDijkstra.totalCost(),
                lastAStar.totalTime(),
                lastDijkstra.totalTime(),
                lastAStar.totalCarbon(),
                lastDijkstra.totalCarbon(),
                lastAStar.totalDistance(),
                lastDijkstra.totalDistance()
        };
    }

    @Test
    void benchmark_allObjectives_20cities() {
        TransportGraph graph = buildGraph20();

        System.out.println("=== ALGORITHM BENCHMARK: A* vs Dijkstra (20 cities, " + graph.edgeCount() + " edges) ===");
        System.out.println("Methodology: " + WARMUP_ITERATIONS + " warmup iterations, "
                + BENCH_ITERATIONS + " timed iterations, alternating order");
        System.out.println("Limitations: wall-clock times approximate; GC pauses not excluded");
        System.out.println("Shipment weight: 500 kg | Source: City1 | Destination: City20");
        System.out.println();

        ObjectiveFunction[] objectives = {
                new CostObjective(),
                new TimeObjective(),
                new CarbonObjective(),
                createBalancedObjective(graph)
        };
        String[] names = {"CHEAPEST", "FASTEST", "GREENEST", "BALANCED"};

        for (int i = 0; i < objectives.length; i++) {
            // Warmup: trigger JIT compilation
            for (int w = 0; w < WARMUP_ITERATIONS; w++) {
                astar.findPath(graph, 1L, 20L, objectives[i], 500);
                dijkstra.findPath(graph, 1L, 20L, objectives[i], 500);
            }

            double[] r = benchmarkAlternating(graph, 1L, 20L, objectives[i], 500, BENCH_ITERATIONS);

            System.out.println("--- " + names[i] + " ---");
            System.out.printf("  A*       : %.3f ms avg, %d nodes, cost=%.2f, time=%.2f, carbon=%.4f%n",
                    r[0], (int) r[2], r[4], r[6], r[8]);
            System.out.printf("  Dijkstra : %.3f ms avg, %d nodes, cost=%.2f, time=%.2f, carbon=%.4f%n",
                    r[1], (int) r[3], r[5], r[7], r[9]);

            assertEquals(r[5], r[4], 0.01, names[i] + ": cost should be equivalent");
            assertEquals(r[7], r[6], 0.01, names[i] + ": time should be equivalent");
            assertEquals(r[9], r[8], 0.01, names[i] + ": carbon should be equivalent");
            assertEquals(r[11], r[10], 0.01, names[i] + ": distance should be equivalent");

            if (!names[i].equals("BALANCED")) {
                assertTrue((int) r[2] <= (int) r[3],
                        names[i] + ": A* should explore <= Dijkstra nodes");
            }

            int aNodes = (int) r[2];
            int dNodes = (int) r[3];
            System.out.printf("  Nodes    : A* %d, Dijkstra %d (A* explores %.0f%%)%n",
                    aNodes, dNodes, dNodes > 0 ? (100.0 * aNodes / dNodes) : 0);
            System.out.println();
        }

        System.out.println("=== BENCHMARK COMPLETE ===");
    }

    @Test
    void benchmark_differentPairs() {
        TransportGraph graph = buildGraph20();

        System.out.println("=== PAIR BENCHMARK: A* vs Dijkstra across different city pairs ===");
        System.out.println("Methodology: " + WARMUP_ITERATIONS + " warmup iterations, "
                + BENCH_ITERATIONS + " timed iterations, alternating order");

        long[][] pairs = {{1, 5}, {1, 10}, {1, 15}, {1, 20}, {5, 15}, {10, 20}};
        ObjectiveFunction objective = new CostObjective();

        System.out.printf("%-15s %10s %10s %10s %10s %10s%n",
                "Pair", "A* ms", "Dij ms", "A* nodes", "Dij nodes", "A* cost");
        System.out.println("-".repeat(65));

        for (long[] pair : pairs) {
            // Warmup
            for (int w = 0; w < WARMUP_ITERATIONS; w++) {
                astar.findPath(graph, pair[0], pair[1], objective, 500);
                dijkstra.findPath(graph, pair[0], pair[1], objective, 500);
            }

            double[] r = benchmarkAlternating(graph, pair[0], pair[1], objective, 500, BENCH_ITERATIONS);

            System.out.printf("City%d->City%d    %10.3f %10.3f %10d %10d %10.2f%n",
                    pair[0], pair[1], r[0], r[1], (int) r[2], (int) r[3], r[4]);

            assertEquals(r[5], r[4], 0.01,
                    "Cost should agree for City" + pair[0] + "->City" + pair[1]);
        }

        System.out.println();
        System.out.println("=== PAIR BENCHMARK COMPLETE ===");
    }
}
