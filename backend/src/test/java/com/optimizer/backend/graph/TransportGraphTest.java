package com.optimizer.backend.graph;

import com.optimizer.backend.Entity.City;
import com.optimizer.backend.Entity.Route;
import com.optimizer.backend.Entity.TransportMode;
import com.optimizer.backend.Entity.TransportModeType;
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
class TransportGraphTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private TransportModeRepository transportModeRepository;
    @Mock
    private CityRepository cityRepository;

    private TransportGraphLoader loader;

    private City mumbai;
    private City delhi;
    private City bengaluru;
    private TransportMode road;
    private TransportMode rail;
    private TransportMode air;

    @BeforeEach
    void setUp() {
        loader = new TransportGraphLoader(routeRepository, transportModeRepository, cityRepository);

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

    @Test
    void loadGraph_correctNumberOfNodesAndEdges() {
        double dist = 1000.0;
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(dist).build(),
                Route.builder().id(2L).sourceCity(mumbai).destinationCity(delhi).transportMode(rail).distance(dist).build(),
                Route.builder().id(3L).sourceCity(mumbai).destinationCity(delhi).transportMode(air).distance(dist).build(),
                Route.builder().id(4L).sourceCity(delhi).destinationCity(mumbai).transportMode(road).distance(dist).build()
        );

        when(routeRepository.findAll()).thenReturn(routes);
        when(transportModeRepository.findAll()).thenReturn(List.of(road, rail, air));
        when(cityRepository.findAll()).thenReturn(List.of(mumbai, delhi));

        TransportGraph graph = loader.loadGraph(100);

        assertEquals(2, graph.nodeCount(), "Should have 2 cities");
        assertEquals(4, graph.edgeCount(), "Should have 4 edges (3 Mumbai→Delhi + 1 Delhi→Mumbai)");
    }

    @Test
    void loadGraph_preComputesEdgeMetrics() {
        double dist = 1000.0;
        double weight = 500.0;

        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi)
                        .transportMode(road).distance(dist).build()
        );

        when(routeRepository.findAll()).thenReturn(routes);
        when(transportModeRepository.findAll()).thenReturn(List.of(road));
        when(cityRepository.findAll()).thenReturn(List.of(mumbai, delhi));

        TransportGraph graph = loader.loadGraph(weight);

        List<GraphEdge> edges = graph.getEdges(1L);
        assertEquals(1, edges.size());

        GraphEdge edge = edges.get(0);
        assertEquals(1L, edge.sourceId());
        assertEquals(2L, edge.destinationId());
        assertEquals(dist, edge.distance(), 0.01);

        // Verify pre-computed cost
        double expectedCost = dist * 1.2 * (1 + 500 * 0.0001);
        assertEquals(expectedCost, edge.cost(), 0.01, "Cost should be pre-computed with weight factor");

        // Verify pre-computed time
        double expectedTime = dist / 60.0;
        assertEquals(expectedTime, edge.time(), 0.01, "Time should be pre-computed");

        // Verify pre-computed carbon
        double expectedCarbon = dist * (weight / 1000.0) * 0.062;
        assertEquals(expectedCarbon, edge.carbon(), 0.01, "Carbon should be pre-computed");
    }

    @Test
    void loadGraph_computesAggregateStats() {
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi)
                        .transportMode(road).distance(1000).build()
        );

        when(routeRepository.findAll()).thenReturn(routes);
        when(transportModeRepository.findAll()).thenReturn(List.of(road, rail, air));
        when(cityRepository.findAll()).thenReturn(List.of(mumbai, delhi));

        TransportGraph graph = loader.loadGraph(100);

        assertEquals(0.8, graph.minCostPerKm(), 0.01, "minCostPerKm should be RAIL's");
        assertEquals(700.0, graph.maxSpeed(), 0.01, "maxSpeed should be AIR's");
        assertEquals(0.022, graph.minCarbonPerTonKm(), 0.001, "minCarbonPerTonKm should be RAIL's");
    }

    @Test
    void loadGraph_emptyRoutes_throwsBadRequest() {
        when(routeRepository.findAll()).thenReturn(List.of());

        assertThrows(com.optimizer.backend.Exception.BadRequestException.class,
                () -> loader.loadGraph(100));
    }

    @Test
    void loadGraph_noEdgesForIsolatedCity() {
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi)
                        .transportMode(road).distance(1000).build()
        );

        when(routeRepository.findAll()).thenReturn(routes);
        when(transportModeRepository.findAll()).thenReturn(List.of(road));
        when(cityRepository.findAll()).thenReturn(List.of(mumbai, delhi, bengaluru));

        TransportGraph graph = loader.loadGraph(100);

        // Bengaluru has no incoming or outgoing edges in this graph
        assertTrue(graph.getEdges(3L).isEmpty(), "Isolated city should have no edges");
        assertTrue(graph.containsCity(3L), "City should still be in the graph");
    }
}
