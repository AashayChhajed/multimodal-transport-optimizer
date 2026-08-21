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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AStarVsDijkstraTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private TransportModeRepository transportModeRepository;
    @Mock
    private CityRepository cityRepository;

    private TransportGraphLoader graphLoader;
    private AStarPathfinder astar;
    private DijkstraPathfinder dijkstra;
    private AlgorithmComparator comparator;

    private City mumbai;
    private City delhi;
    private City bengaluru;
    private TransportMode road;
    private TransportMode rail;
    private TransportMode air;

    @BeforeEach
    void setUp() {
        graphLoader = new TransportGraphLoader(routeRepository, transportModeRepository, cityRepository);
        astar = new AStarPathfinder();
        dijkstra = new DijkstraPathfinder();
        comparator = new AlgorithmComparator();

        mumbai = City.builder().id(1L).name("Mumbai").latitude(19.076).longitude(72.877).build();
        delhi = City.builder().id(2L).name("Delhi").latitude(28.613).longitude(77.209).build();
        bengaluru = City.builder().id(3L).name("Bengaluru").latitude(12.971).longitude(77.594).build();

        road = TransportMode.builder().id(1L).name(TransportModeType.ROAD)
                .costPerKm(1.2).speed(60).carbonPerTonKm(0.062).build();
        rail = TransportMode.builder().id(2L).name(TransportModeType.RAIL)
                .costPerKm(0.8).speed(90).carbonPerTonKm(0.022).build();
        air = TransportMode.builder().id(3L).name(TransportModeType.AIR)
                .costPerKm(3.0).speed(700).carbonPerTonKm(0.602).build();
    }

    private void mockRepos(List<Route> routes) {
        when(routeRepository.findAll()).thenReturn(routes);
        when(transportModeRepository.findAll()).thenReturn(List.of(road, rail, air));
        when(cityRepository.findAll()).thenReturn(List.of(mumbai, delhi, bengaluru));
    }

    @Test
    void cheapest_astarAndDijkstra_sameCostAndTime() {
        double dist = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(dist).build(),
                Route.builder().id(2L).sourceCity(mumbai).destinationCity(delhi).transportMode(rail).distance(dist).build(),
                Route.builder().id(3L).sourceCity(mumbai).destinationCity(delhi).transportMode(air).distance(dist).build()
        );
        mockRepos(routes);

        TransportGraph graph = graphLoader.loadGraph(500);
        ObjectiveFunction objective = new CostObjective();

        PathResult astarResult = astar.findPath(graph, 1L, 2L, objective, 500);
        PathResult dijkstraResult = dijkstra.findPath(graph, 1L, 2L, objective, 500);

        assertTrue(astarResult.hasPath());
        assertTrue(dijkstraResult.hasPath());

        assertEquals(dijkstraResult.totalCost(), astarResult.totalCost(), 0.01,
                "A* and Dijkstra should find the same cost for CHEAPEST");
        assertEquals(dijkstraResult.totalTime(), astarResult.totalTime(), 0.01,
                "A* and Dijkstra should find the same time for CHEAPEST");
        assertEquals(dijkstraResult.totalDistance(), astarResult.totalDistance(), 0.01,
                "A* and Dijkstra should find the same distance for CHEAPEST");
    }

    @Test
    void fastest_astarAndDijkstra_sameCostAndTime() {
        double dist = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(dist).build(),
                Route.builder().id(2L).sourceCity(mumbai).destinationCity(delhi).transportMode(rail).distance(dist).build(),
                Route.builder().id(3L).sourceCity(mumbai).destinationCity(delhi).transportMode(air).distance(dist).build()
        );
        mockRepos(routes);

        TransportGraph graph = graphLoader.loadGraph(500);
        ObjectiveFunction objective = new TimeObjective();

        PathResult astarResult = astar.findPath(graph, 1L, 2L, objective, 500);
        PathResult dijkstraResult = dijkstra.findPath(graph, 1L, 2L, objective, 500);

        assertTrue(astarResult.hasPath());
        assertTrue(dijkstraResult.hasPath());

        assertEquals(dijkstraResult.totalCost(), astarResult.totalCost(), 0.01,
                "A* and Dijkstra should find the same cost for FASTEST");
        assertEquals(dijkstraResult.totalTime(), astarResult.totalTime(), 0.01,
                "A* and Dijkstra should find the same time for FASTEST");
    }

    @Test
    void greenest_astarAndDijkstra_sameCostAndTime() {
        double dist = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(dist).build(),
                Route.builder().id(2L).sourceCity(mumbai).destinationCity(delhi).transportMode(rail).distance(dist).build(),
                Route.builder().id(3L).sourceCity(mumbai).destinationCity(delhi).transportMode(air).distance(dist).build()
        );
        mockRepos(routes);

        TransportGraph graph = graphLoader.loadGraph(500);
        ObjectiveFunction objective = new CarbonObjective();

        PathResult astarResult = astar.findPath(graph, 1L, 2L, objective, 500);
        PathResult dijkstraResult = dijkstra.findPath(graph, 1L, 2L, objective, 500);

        assertTrue(astarResult.hasPath());
        assertTrue(dijkstraResult.hasPath());

        assertEquals(dijkstraResult.totalCost(), astarResult.totalCost(), 0.01,
                "A* and Dijkstra should find the same cost for GREENEST");
        assertEquals(dijkstraResult.totalCarbon(), astarResult.totalCarbon(), 0.01,
                "A* and Dijkstra should find the same carbon for GREENEST");
    }

    @Test
    void balanced_astarAndDijkstra_sameCostAndTime() {
        double dist = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(dist).build(),
                Route.builder().id(2L).sourceCity(mumbai).destinationCity(delhi).transportMode(rail).distance(dist).build(),
                Route.builder().id(3L).sourceCity(mumbai).destinationCity(delhi).transportMode(air).distance(dist).build()
        );
        mockRepos(routes);

        TransportGraph graph = graphLoader.loadGraph(500);
        BalancedObjective balanced = new BalancedObjective();
        // Set normalization bounds
        double maxCost = 0, maxTime = 0, maxCarbon = 0;
        for (var edges : graph.adjacency().values()) {
            for (GraphEdge e : edges) {
                maxCost = Math.max(maxCost, e.cost());
                maxTime = Math.max(maxTime, e.time());
                maxCarbon = Math.max(maxCarbon, e.carbon());
            }
        }
        balanced.setNormalizationBounds(maxCost, maxTime, maxCarbon);

        PathResult astarResult = astar.findPath(graph, 1L, 2L, balanced, 500);
        PathResult dijkstraResult = dijkstra.findPath(graph, 1L, 2L, balanced, 500);

        assertTrue(astarResult.hasPath());
        assertTrue(dijkstraResult.hasPath());

        assertEquals(dijkstraResult.totalCost(), astarResult.totalCost(), 0.01,
                "A* and Dijkstra should find the same cost for BALANCED");
        assertEquals(dijkstraResult.totalTime(), astarResult.totalTime(), 0.01,
                "A* and Dijkstra should find the same time for BALANCED");
    }

    @Test
    void comparator_reportsEquivalentObjective() {
        double dist = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(dist).build(),
                Route.builder().id(2L).sourceCity(mumbai).destinationCity(delhi).transportMode(rail).distance(dist).build(),
                Route.builder().id(3L).sourceCity(mumbai).destinationCity(delhi).transportMode(air).distance(dist).build()
        );
        mockRepos(routes);

        TransportGraph graph = graphLoader.loadGraph(500);

        AlgorithmComparator.ComparisonResult result = comparator.compare(graph, 1L, 2L, new CostObjective(), 500);

        assertTrue(result.equivalentObjective(),
                "A* and Dijkstra should produce equivalent objective values for CHEAPEST");
    }

    @Test
    void comparator_aStarExploresFewerNodes() {
        // Build a more complex graph where A* heuristic pruning is evident
        double dist = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(dist).build(),
                Route.builder().id(2L).sourceCity(mumbai).destinationCity(delhi).transportMode(rail).distance(dist).build(),
                Route.builder().id(3L).sourceCity(mumbai).destinationCity(delhi).transportMode(air).distance(dist).build(),
                Route.builder().id(4L).sourceCity(mumbai).destinationCity(bengaluru).transportMode(road).distance(dist * 2).build(),
                Route.builder().id(5L).sourceCity(mumbai).destinationCity(bengaluru).transportMode(rail).distance(dist * 2).build(),
                Route.builder().id(6L).sourceCity(delhi).destinationCity(bengaluru).transportMode(road).distance(dist).build(),
                Route.builder().id(7L).sourceCity(delhi).destinationCity(bengaluru).transportMode(rail).distance(dist).build(),
                Route.builder().id(8L).sourceCity(delhi).destinationCity(mumbai).transportMode(road).distance(dist).build(),
                Route.builder().id(9L).sourceCity(bengaluru).destinationCity(mumbai).transportMode(road).distance(dist * 2).build(),
                Route.builder().id(10L).sourceCity(bengaluru).destinationCity(delhi).transportMode(road).distance(dist).build()
        );
        mockRepos(routes);

        TransportGraph graph = graphLoader.loadGraph(500);

        AlgorithmComparator.ComparisonResult result = comparator.compare(graph, 1L, 3L, new CostObjective(), 500);

        // A* with admissible heuristic should explore fewer or equal nodes
        assertTrue(result.aStarResult().nodesExplored() <= result.dijkstraResult().nodesExplored(),
                "A* should explore fewer or equal nodes compared to Dijkstra");
    }
}
