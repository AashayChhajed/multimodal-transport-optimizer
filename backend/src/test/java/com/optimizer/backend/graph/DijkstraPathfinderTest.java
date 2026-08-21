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
class DijkstraPathfinderTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private TransportModeRepository transportModeRepository;
    @Mock
    private CityRepository cityRepository;

    private TransportGraphLoader graphLoader;
    private DijkstraPathfinder dijkstra;

    private City mumbai;
    private City delhi;
    private City bengaluru;
    private TransportMode road;
    private TransportMode rail;
    private TransportMode air;

    @BeforeEach
    void setUp() {
        graphLoader = new TransportGraphLoader(routeRepository, transportModeRepository, cityRepository);
        dijkstra = new DijkstraPathfinder();

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
    void dijkstra_cheapest_selectsCheapestEdge() {
        double dist = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(dist).build(),
                Route.builder().id(2L).sourceCity(mumbai).destinationCity(delhi).transportMode(rail).distance(dist).build(),
                Route.builder().id(3L).sourceCity(mumbai).destinationCity(delhi).transportMode(air).distance(dist).build()
        );
        mockRepos(routes);

        TransportGraph graph = graphLoader.loadGraph(100);
        ObjectiveFunction objective = new CostObjective();

        PathResult result = dijkstra.findPath(graph, 1L, 2L, objective, 100);

        assertTrue(result.hasPath());
        assertEquals(1, result.edges().size());
        assertEquals(TransportModeType.RAIL, result.edges().get(0).modeType(),
                "Dijkstra CHEAPEST should select rail (lowest costPerKm)");
    }

    @Test
    void dijkstra_fastest_selectsFastestEdge() {
        double dist = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(dist).build(),
                Route.builder().id(2L).sourceCity(mumbai).destinationCity(delhi).transportMode(rail).distance(dist).build(),
                Route.builder().id(3L).sourceCity(mumbai).destinationCity(delhi).transportMode(air).distance(dist).build()
        );
        mockRepos(routes);

        TransportGraph graph = graphLoader.loadGraph(100);
        ObjectiveFunction objective = new TimeObjective();

        PathResult result = dijkstra.findPath(graph, 1L, 2L, objective, 100);

        assertTrue(result.hasPath());
        assertEquals(1, result.edges().size());
        assertEquals(TransportModeType.AIR, result.edges().get(0).modeType(),
                "Dijkstra FASTEST should select air (highest speed)");
    }

    @Test
    void dijkstra_greenest_selectsGreenestEdge() {
        double dist = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(dist).build(),
                Route.builder().id(2L).sourceCity(mumbai).destinationCity(delhi).transportMode(rail).distance(dist).build(),
                Route.builder().id(3L).sourceCity(mumbai).destinationCity(delhi).transportMode(air).distance(dist).build()
        );
        mockRepos(routes);

        TransportGraph graph = graphLoader.loadGraph(1000);
        ObjectiveFunction objective = new CarbonObjective();

        PathResult result = dijkstra.findPath(graph, 1L, 2L, objective, 1000);

        assertTrue(result.hasPath());
        assertEquals(TransportModeType.RAIL, result.edges().get(0).modeType(),
                "Dijkstra GREENEST should select rail (lowest carbon factor)");
    }

    @Test
    void dijkstra_unreachable_returnsNoPath() {
        City isolated = City.builder().id(99L).name("Isolated").latitude(0).longitude(0).build();
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi)
                        .transportMode(road).distance(1000).build()
        );
        when(routeRepository.findAll()).thenReturn(routes);
        when(transportModeRepository.findAll()).thenReturn(List.of(road));
        when(cityRepository.findAll()).thenReturn(List.of(mumbai, delhi, isolated));

        TransportGraph graph = graphLoader.loadGraph(100);
        PathResult result = dijkstra.findPath(graph, 1L, 99L, new CostObjective(), 100);

        assertFalse(result.hasPath(), "Should return no path for isolated city");
    }

    @Test
    void dijkstra_sameSourceAndDestination_returnsNoPath() {
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi)
                        .transportMode(road).distance(1000).build()
        );
        mockRepos(routes);

        TransportGraph graph = graphLoader.loadGraph(100);
        PathResult result = dijkstra.findPath(graph, 1L, 1L, new CostObjective(), 100);

        assertFalse(result.hasPath(), "Same source and destination should return no path");
    }

    @Test
    void dijkstra_indirectRoute_findsPathThroughIntermediate() {
        double mumbaiDelhiDist = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        double delhiBengDist = CostCalculator.haversine(28.613, 77.209, 12.971, 77.594);

        // Only routes: Mumbai→Delhi and Delhi→Bengaluru (no direct Mumbai→Bengaluru)
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(mumbaiDelhiDist).build(),
                Route.builder().id(2L).sourceCity(mumbai).destinationCity(delhi).transportMode(rail).distance(mumbaiDelhiDist).build(),
                Route.builder().id(3L).sourceCity(delhi).destinationCity(bengaluru).transportMode(road).distance(delhiBengDist).build(),
                Route.builder().id(4L).sourceCity(delhi).destinationCity(bengaluru).transportMode(rail).distance(delhiBengDist).build()
        );
        mockRepos(routes);

        TransportGraph graph = graphLoader.loadGraph(100);
        PathResult result = dijkstra.findPath(graph, 1L, 3L, new CostObjective(), 100);

        assertTrue(result.hasPath());
        assertEquals(2, result.edges().size(), "Should have 2 legs: Mumbai→Delhi→Bengaluru");
        assertTrue(result.totalDistance() > 0);
        assertTrue(result.totalCost() > 0);
        assertTrue(result.totalTime() > 0);
        assertTrue(result.totalCarbon() > 0);
    }

    @Test
    void dijkstra_nodesExplored_isPositive() {
        double dist = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(dist).build()
        );
        mockRepos(routes);

        TransportGraph graph = graphLoader.loadGraph(100);
        PathResult result = dijkstra.findPath(graph, 1L, 2L, new CostObjective(), 100);

        assertTrue(result.nodesExplored() > 0, "Should have explored at least 1 node");
    }

    @Test
    void dijkstra_balanced_findsPath() {
        double dist = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(dist).build(),
                Route.builder().id(2L).sourceCity(mumbai).destinationCity(delhi).transportMode(rail).distance(dist).build()
        );
        mockRepos(routes);

        TransportGraph graph = graphLoader.loadGraph(100);

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

        PathResult result = dijkstra.findPath(graph, 1L, 2L, balanced, 100);

        assertTrue(result.hasPath());
        assertEquals(1, result.edges().size());
    }
}
