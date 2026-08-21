package com.optimizer.backend.Service;

import com.optimizer.backend.DTO.OptimizationResponseDTO;
import com.optimizer.backend.Entity.City;
import com.optimizer.backend.Entity.OptimizationResult;
import com.optimizer.backend.Entity.OptimizationType;
import com.optimizer.backend.Entity.Route;
import com.optimizer.backend.Entity.Shipment;
import com.optimizer.backend.Entity.TransportMode;
import com.optimizer.backend.Entity.TransportModeType;
import com.optimizer.backend.Exception.BadRequestException;
import com.optimizer.backend.Exception.ResourceNotFoundException;
import com.optimizer.backend.Repository.OptimizationResultRepository;
import com.optimizer.backend.Repository.CityRepository;
import com.optimizer.backend.Repository.RouteRepository;
import com.optimizer.backend.Repository.TransportModeRepository;
import com.optimizer.backend.graph.AStarPathfinder;
import com.optimizer.backend.graph.PathfindingAlgorithm;
import com.optimizer.backend.graph.TransferTimeCalculator;
import com.optimizer.backend.graph.TransportGraph;
import com.optimizer.backend.graph.TransportGraphLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OptimizationServiceTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private TransportModeRepository transportModeRepository;
    @Mock
    private CityRepository cityRepository;
    @Mock
    private OptimizationResultRepository optimizationResultRepository;

    @Mock
    private TransportGraphLoader graphLoader;

    @InjectMocks
    private TransportGraphLoader realGraphLoader;

    private OptimizationService optimizationService;

    private City mumbai;
    private City delhi;
    private City bengaluru;
    private TransportMode road;
    private TransportMode rail;
    private TransportMode air;

    private final PathfindingAlgorithm astar = new AStarPathfinder();
    private final TransferTimeCalculator transferCalculator = new TransferTimeCalculator();

    @BeforeEach
    void setUp() {
        mumbai = City.builder().id(1L).name("Mumbai").latitude(19.076).longitude(72.877).build();
        delhi = City.builder().id(2L).name("Delhi").latitude(28.613).longitude(77.209).build();
        bengaluru = City.builder().id(3L).name("Bengaluru").latitude(12.971).longitude(77.594).build();

        road = TransportMode.builder().id(1L).name(TransportModeType.ROAD)
                .costPerKm(1.2).speed(60).carbonPerTonKm(0.062).build();
        rail = TransportMode.builder().id(2L).name(TransportModeType.RAIL)
                .costPerKm(0.8).speed(90).carbonPerTonKm(0.022).build();
        air = TransportMode.builder().id(3L).name(TransportModeType.AIR)
                .costPerKm(3.0).speed(700).carbonPerTonKm(0.602).build();

        // Build real graph loader with mocked repos
        realGraphLoader = new TransportGraphLoader(routeRepository, transportModeRepository, cityRepository);
        optimizationService = new OptimizationService(realGraphLoader, optimizationResultRepository, transferCalculator);
    }

    private Shipment createShipment(Long id, City source, City dest, double weight) {
        return Shipment.builder().id(id).sourceCity(source).destinationCity(dest)
                .weight(weight).description("Test").build();
    }

    private List<Route> buildGraph() {
        double mumbaiDelhiDist = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        double mumbaiBengDist = CostCalculator.haversine(19.076, 72.877, 12.971, 77.594);
        double delhiBengDist = CostCalculator.haversine(28.613, 77.209, 12.971, 77.594);

        return List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(mumbaiDelhiDist).build(),
                Route.builder().id(2L).sourceCity(mumbai).destinationCity(delhi).transportMode(rail).distance(mumbaiDelhiDist).build(),
                Route.builder().id(3L).sourceCity(mumbai).destinationCity(delhi).transportMode(air).distance(mumbaiDelhiDist).build(),
                Route.builder().id(4L).sourceCity(mumbai).destinationCity(bengaluru).transportMode(road).distance(mumbaiBengDist).build(),
                Route.builder().id(5L).sourceCity(mumbai).destinationCity(bengaluru).transportMode(rail).distance(mumbaiBengDist).build(),
                Route.builder().id(6L).sourceCity(mumbai).destinationCity(bengaluru).transportMode(air).distance(mumbaiBengDist).build(),
                Route.builder().id(7L).sourceCity(delhi).destinationCity(mumbai).transportMode(road).distance(mumbaiDelhiDist).build(),
                Route.builder().id(8L).sourceCity(delhi).destinationCity(mumbai).transportMode(rail).distance(mumbaiDelhiDist).build(),
                Route.builder().id(9L).sourceCity(delhi).destinationCity(mumbai).transportMode(air).distance(mumbaiDelhiDist).build(),
                Route.builder().id(10L).sourceCity(delhi).destinationCity(bengaluru).transportMode(road).distance(delhiBengDist).build(),
                Route.builder().id(11L).sourceCity(delhi).destinationCity(bengaluru).transportMode(rail).distance(delhiBengDist).build(),
                Route.builder().id(12L).sourceCity(delhi).destinationCity(bengaluru).transportMode(air).distance(delhiBengDist).build(),
                Route.builder().id(13L).sourceCity(bengaluru).destinationCity(mumbai).transportMode(road).distance(mumbaiBengDist).build(),
                Route.builder().id(14L).sourceCity(bengaluru).destinationCity(mumbai).transportMode(rail).distance(mumbaiBengDist).build(),
                Route.builder().id(15L).sourceCity(bengaluru).destinationCity(mumbai).transportMode(air).distance(mumbaiBengDist).build(),
                Route.builder().id(16L).sourceCity(bengaluru).destinationCity(delhi).transportMode(road).distance(delhiBengDist).build(),
                Route.builder().id(17L).sourceCity(bengaluru).destinationCity(delhi).transportMode(rail).distance(delhiBengDist).build(),
                Route.builder().id(18L).sourceCity(bengaluru).destinationCity(delhi).transportMode(air).distance(delhiBengDist).build()
        );
    }

    private void mockRepositories(List<Route> routes) {
        when(routeRepository.findAll()).thenReturn(routes);
        when(transportModeRepository.findAll()).thenReturn(List.of(road, rail, air));
        when(cityRepository.findAll()).thenReturn(List.of(mumbai, delhi, bengaluru));
        when(optimizationResultRepository.findByShipmentId(any())).thenReturn(Optional.empty());
        when(optimizationResultRepository.save(any())).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof OptimizationResult result) {
                result.setId(1L);
                return result;
            }
            return arg;
        });
    }

    // ── CHEAPEST Tests ──

    @Test
    void optimizeCheapest_selectsCheapestRoute() {
        mockRepositories(buildGraph());
        Shipment shipment = createShipment(1L, mumbai, delhi, 100);

        OptimizationResponseDTO result = optimizationService.optimize(shipment, OptimizationType.CHEAPEST, astar);

        assertNotNull(result);
        assertEquals(1L, result.getShipmentId());
        assertEquals(OptimizationType.CHEAPEST, result.getOptimizationType());
        assertFalse(result.getRoutes().isEmpty());
        assertTrue(result.getCities().contains("Mumbai"));
        assertTrue(result.getCities().contains("Delhi"));

        // Cheapest should prefer rail (costPerKm=0.8) over road (1.2) and air (3.0)
        String mode = result.getRoutes().get(0).getTransportMode();
        assertEquals("RAIL", mode, "CHEAPEST should select rail (lowest costPerKm)");
    }

    @Test
    void optimizeCheapest_costIncludesWeightFactor() {
        mockRepositories(buildGraph());
        Shipment lightShipment = createShipment(1L, mumbai, delhi, 100);
        Shipment heavyShipment = createShipment(2L, mumbai, delhi, 10000);

        OptimizationResponseDTO lightResult = optimizationService.optimize(lightShipment, OptimizationType.CHEAPEST, astar);
        OptimizationResponseDTO heavyResult = optimizationService.optimize(heavyShipment, OptimizationType.CHEAPEST, astar);

        assertTrue(heavyResult.getTotalCost() > lightResult.getTotalCost(),
                "Heavier shipment should cost more");
    }

    // ── FASTEST Tests ──

    @Test
    void optimizeFastest_selectsFastestRoute() {
        mockRepositories(buildGraph());
        Shipment shipment = createShipment(1L, mumbai, delhi, 100);

        OptimizationResponseDTO result = optimizationService.optimize(shipment, OptimizationType.FASTEST, astar);

        assertNotNull(result);
        assertEquals(OptimizationType.FASTEST, result.getOptimizationType());
        assertFalse(result.getRoutes().isEmpty());

        // Fastest should prefer air (speed=700) over rail (90) and road (60)
        String mode = result.getRoutes().get(0).getTransportMode();
        assertEquals("AIR", mode, "FASTEST should select air (highest speed)");
    }

    @Test
    void optimizeFastest_timeNotAffectedByWeight() {
        mockRepositories(buildGraph());
        Shipment lightShipment = createShipment(1L, mumbai, delhi, 100);
        Shipment heavyShipment = createShipment(2L, mumbai, delhi, 10000);

        OptimizationResponseDTO lightResult = optimizationService.optimize(lightShipment, OptimizationType.FASTEST, astar);
        OptimizationResponseDTO heavyResult = optimizationService.optimize(heavyShipment, OptimizationType.FASTEST, astar);

        assertEquals(lightResult.getTotalTime(), heavyResult.getTotalTime(), 0.001,
                "Time should NOT change with weight");
    }

    // ── Carbon Tests ──

    @Test
    void optimizeCalculatesCarbonEmissions() {
        mockRepositories(buildGraph());
        Shipment shipment = createShipment(1L, mumbai, delhi, 1000);

        OptimizationResponseDTO result = optimizationService.optimize(shipment, OptimizationType.CHEAPEST, astar);

        assertTrue(result.getTotalCarbon() > 0, "Carbon should be positive");
        assertFalse(result.getRoutes().isEmpty());
        assertTrue(result.getRoutes().get(0).getCarbon() > 0,
                "Per-leg carbon should be positive");
    }

    @Test
    void optimizeCheapestHasLowerCarbonThanFastest() {
        mockRepositories(buildGraph());
        Shipment shipment = createShipment(1L, mumbai, delhi, 1000);

        OptimizationResponseDTO cheapest = optimizationService.optimize(shipment, OptimizationType.CHEAPEST, astar);

        // Re-stub for second call (different optimization type)
        mockRepositories(buildGraph());
        OptimizationResponseDTO fastest = optimizationService.optimize(shipment, OptimizationType.FASTEST, astar);

        // CHEAPEST selects rail (low carbon), FASTEST selects air (high carbon)
        assertTrue(cheapest.getTotalCarbon() < fastest.getTotalCarbon(),
                "CHEAPEST (rail) should have lower carbon than FASTEST (air)");
    }

    // ── Distance Tests ──

    @Test
    void optimizeCalculatesTotalDistance() {
        mockRepositories(buildGraph());
        Shipment shipment = createShipment(1L, mumbai, delhi, 100);

        OptimizationResponseDTO result = optimizationService.optimize(shipment, OptimizationType.CHEAPEST, astar);

        assertTrue(result.getTotalDistance() > 0, "Total distance should be positive");
        assertEquals(result.getRoutes().get(0).getDistance(), result.getTotalDistance(), 0.01,
                "For single-leg route, total distance should equal leg distance");
    }

    // ── Error Handling Tests ──

    @Test
    void optimizeSameCity_throwsBadRequest() {
        Shipment shipment = createShipment(1L, mumbai, mumbai, 100);

        assertThrows(BadRequestException.class,
                () -> optimizationService.optimize(shipment, OptimizationType.CHEAPEST, astar));
    }

    @Test
    void optimizeNoRoutes_throwsBadRequest() {
        when(routeRepository.findAll()).thenReturn(Collections.emptyList());
        when(transportModeRepository.findAll()).thenReturn(List.of(road, rail, air));
        when(cityRepository.findAll()).thenReturn(List.of(mumbai, delhi));
        Shipment shipment = createShipment(1L, mumbai, delhi, 100);

        assertThrows(BadRequestException.class,
                () -> optimizationService.optimize(shipment, OptimizationType.CHEAPEST, astar));
    }

    @Test
    void optimizeUnreachableDestination_throwsResourceNotFound() {
        City isolated = City.builder().id(99L).name("Isolated").latitude(0).longitude(0).build();
        Route onlyRoute = Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi)
                .transportMode(road).distance(100).build();
        when(routeRepository.findAll()).thenReturn(List.of(onlyRoute));
        when(transportModeRepository.findAll()).thenReturn(List.of(road));
        when(cityRepository.findAll()).thenReturn(List.of(mumbai, delhi, isolated));
        when(optimizationResultRepository.findByShipmentId(any())).thenReturn(Optional.empty());

        Shipment shipment = createShipment(1L, mumbai, isolated, 100);

        assertThrows(ResourceNotFoundException.class,
                () -> optimizationService.optimize(shipment, OptimizationType.CHEAPEST, astar));
    }

    // ── Graph Path Tests ──

    @Test
    void optimizeIndirectRoute_findsPathThroughIntermediateCity() {
        double mumbaiDelhiDist = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        double delhiBengDist = CostCalculator.haversine(28.613, 77.209, 12.971, 77.594);

        List<Route> limitedRoutes = List.of(
                Route.builder().id(1L).sourceCity(mumbai).destinationCity(delhi).transportMode(road).distance(mumbaiDelhiDist).build(),
                Route.builder().id(2L).sourceCity(mumbai).destinationCity(delhi).transportMode(rail).distance(mumbaiDelhiDist).build(),
                Route.builder().id(3L).sourceCity(delhi).destinationCity(bengaluru).transportMode(road).distance(delhiBengDist).build(),
                Route.builder().id(4L).sourceCity(delhi).destinationCity(bengaluru).transportMode(rail).distance(delhiBengDist).build()
        );
        mockRepositories(limitedRoutes);

        Shipment shipment = createShipment(1L, mumbai, bengaluru, 100);
        OptimizationResponseDTO result = optimizationService.optimize(shipment, OptimizationType.CHEAPEST, astar);

        assertNotNull(result);
        assertEquals(3, result.getCities().size(), "Should have 3 cities in path");
        assertEquals("Mumbai", result.getCities().get(0));
        assertEquals("Delhi", result.getCities().get(1));
        assertEquals("Bengaluru", result.getCities().get(2));
        assertEquals(2, result.getRoutes().size(), "Should have 2 route legs");
    }

    // ── getByShipmentId Tests ──

    @Test
    void getByShipmentId_returnsStoredResult() {
        OptimizationResult stored = OptimizationResult.builder()
                .id(1L)
                .shipment(createShipment(1L, mumbai, delhi, 100))
                .totalCost(1212.0)
                .totalTime(19.17)
                .totalDistance(1400.0)
                .totalCarbon(86.8)
                .path("C:TXVtYmFp;R:TXVtYmFp|RGVsaQ==|UkFJTA==|1400.0|1212.0|19.17|86.8")
                .build();
        when(optimizationResultRepository.findByShipmentId(1L)).thenReturn(Optional.of(stored));

        OptimizationResponseDTO result = optimizationService.getByShipmentId(1L);

        assertEquals(1212.0, result.getTotalCost(), 0.01);
        assertEquals(19.17, result.getTotalTime(), 0.01);
        assertEquals(1400.0, result.getTotalDistance(), 0.01);
        assertEquals(86.8, result.getTotalCarbon(), 0.01);
    }

    @Test
    void getByShipmentId_notFound_throwsResourceNotFound() {
        when(optimizationResultRepository.findByShipmentId(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> optimizationService.getByShipmentId(999L));
    }
}
