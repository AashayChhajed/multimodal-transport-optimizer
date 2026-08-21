package com.optimizer.backend.graph;

import com.optimizer.backend.Entity.City;
import com.optimizer.backend.Entity.Route;
import com.optimizer.backend.Entity.TransportMode;
import com.optimizer.backend.Entity.TransportModeType;
import com.optimizer.backend.Repository.CityRepository;
import com.optimizer.backend.Repository.RouteRepository;
import com.optimizer.backend.Repository.TransportModeRepository;
import com.optimizer.backend.Service.CostCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObjectiveFunctionTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private TransportModeRepository transportModeRepository;
    @Mock
    private CityRepository cityRepository;

    private TransportGraphLoader graphLoader;

    private City mumbai;
    private City delhi;
    private TransportMode road;
    private TransportMode rail;
    private TransportMode air;

    @BeforeEach
    void setUp() {
        graphLoader = new TransportGraphLoader(routeRepository, transportModeRepository, cityRepository);

        mumbai = City.builder().id(1L).name("Mumbai").latitude(19.076).longitude(72.877).build();
        delhi = City.builder().id(2L).name("Delhi").latitude(28.613).longitude(77.209).build();

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
        when(cityRepository.findAll()).thenReturn(List.of(mumbai, delhi));
    }

    // ── CostObjective ──

    @Test
    void costObjective_edgeScore_matchesCostCalculator() {
        GraphEdge edge = new GraphEdge(1L, 2L, road, 1000, 0, 0, 0);
        CostObjective objective = new CostObjective();

        double score = objective.edgeScore(edge, 500);
        double expected = CostCalculator.calculateCost(1000, 1.2, 500);

        assertEquals(expected, score, 0.01, "CostObjective edgeScore should match CostCalculator.calculateCost");
    }

    @Test
    void costObjective_heuristic_isLowerBound() {
        CostObjective objective = new CostObjective();
        double h = objective.heuristic(19.076, 72.877, 28.613, 77.209, createGraph(), 500);

        assertTrue(h > 0, "Heuristic should be positive for distant cities");
        // Heuristic should be less than the actual cheapest route cost
        double haversine = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        double actualCheapest = CostCalculator.calculateCost(haversine, 0.8, 500);
        assertTrue(h <= actualCheapest + 0.01,
                "Heuristic should not overestimate cheapest cost. h=" + h + ", actual=" + actualCheapest);
    }

    // ── TimeObjective ──

    @Test
    void timeObjective_edgeScore_matchesCostCalculator() {
        GraphEdge edge = new GraphEdge(1L, 2L, air, 1000, 0, 0, 0);
        TimeObjective objective = new TimeObjective();

        double score = objective.edgeScore(edge, 500);
        double expected = CostCalculator.calculateTime(1000, 700);

        assertEquals(expected, score, 0.01, "TimeObjective edgeScore should match CostCalculator.calculateTime");
    }

    @Test
    void timeObjective_heuristic_isLowerBound() {
        TimeObjective objective = new TimeObjective();
        double h = objective.heuristic(19.076, 72.877, 28.613, 77.209, createGraph(), 500);

        assertTrue(h > 0, "Heuristic should be positive for distant cities");
        double haversine = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        double actualFastest = CostCalculator.calculateTime(haversine, 700);
        assertTrue(h <= actualFastest + 0.01,
                "Heuristic should not overestimate fastest time. h=" + h + ", actual=" + actualFastest);
    }

    // ── CarbonObjective ──

    @Test
    void carbonObjective_edgeScore_matchesCostCalculator() {
        GraphEdge edge = new GraphEdge(1L, 2L, road, 1000, 0, 0, 0);
        CarbonObjective objective = new CarbonObjective();

        double score = objective.edgeScore(edge, 2000);
        double expected = CostCalculator.calculateCarbon(1000, 2000, 0.062);

        assertEquals(expected, score, 0.01, "CarbonObjective edgeScore should match CostCalculator.calculateCarbon");
    }

    @Test
    void carbonObjective_heuristic_isLowerBound() {
        CarbonObjective objective = new CarbonObjective();
        double h = objective.heuristic(19.076, 72.877, 28.613, 77.209, createGraph(), 2000);

        assertTrue(h > 0, "Heuristic should be positive for distant cities");
        double haversine = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        double actualGreenest = CostCalculator.calculateCarbon(haversine, 2000, 0.022);
        assertTrue(h <= actualGreenest + 0.01,
                "Heuristic should not overestimate greenest carbon. h=" + h + ", actual=" + actualGreenest);
    }

    // ── BalancedObjective ──

    @Test
    void balancedObjective_edgeScore_usesNormalization() {
        BalancedObjective balanced = new BalancedObjective();
        balanced.setNormalizationBounds(1800, 16.67, 62.0);

        GraphEdge edge = new GraphEdge(1L, 2L, road, 1000, 1212, 16.67, 62.0);
        double score = balanced.edgeScore(edge, 500);

        assertTrue(score > 0 && score <= 1.0,
                "Balanced score should be between 0 and 1. Got: " + score);
    }

    @Test
    void balancedObjective_heuristic_isZero() {
        BalancedObjective balanced = new BalancedObjective();
        double h = balanced.heuristic(19.076, 72.877, 28.613, 77.209, createGraph(), 500);

        assertEquals(0.0, h, 0.001, "BALANCED heuristic should be zero (documented design decision)");
    }

    @Test
    void balancedObjective_equalWeights() {
        BalancedObjective balanced = new BalancedObjective();
        balanced.setNormalizationBounds(1.0, 1.0, 1.0);

        // All-normalized edge
        GraphEdge edge = new GraphEdge(1L, 2L, road, 1000, 1.0, 1.0, 1.0);
        double score = balanced.edgeScore(edge, 100);

        // With equal normalization and weights: 0.33 + 0.33 + 0.34 = 1.0
        assertEquals(1.0, score, 0.01, "All-normalized edge should score ~1.0");
    }

    // ── Name tests ──

    @Test
    void objectiveNames_areCorrect() {
        assertEquals("CHEAPEST", new CostObjective().name());
        assertEquals("FASTEST", new TimeObjective().name());
        assertEquals("GREENEST", new CarbonObjective().name());
        assertEquals("BALANCED", new BalancedObjective().name());
    }

    // ── Helper ──

    private TransportGraph createGraph() {
        double dist = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        List<Route> routes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(dist).build(),
                Route.builder().id(2L).sourceCity(mumbai).destinationCity(delhi).transportMode(rail).distance(dist).build(),
                Route.builder().id(3L).sourceCity(mumbai).destinationCity(delhi).transportMode(air).distance(dist).build()
        );
        mockRepos(routes);
        return graphLoader.loadGraph(500);
    }
}
