package com.optimizer.backend.Service;

import com.optimizer.backend.Entity.City;
import com.optimizer.backend.Entity.OptimizationResult;
import com.optimizer.backend.Entity.OptimizationType;
import com.optimizer.backend.Entity.Route;
import com.optimizer.backend.Entity.Shipment;
import com.optimizer.backend.Entity.TransportMode;
import com.optimizer.backend.Entity.TransportModeType;
import com.optimizer.backend.Repository.CityRepository;
import com.optimizer.backend.Repository.OptimizationResultRepository;
import com.optimizer.backend.Repository.RouteRepository;
import com.optimizer.backend.Repository.TransportModeRepository;
import com.optimizer.backend.graph.AStarPathfinder;
import com.optimizer.backend.graph.PathfindingAlgorithm;
import com.optimizer.backend.graph.TransferTimeCalculator;
import com.optimizer.backend.graph.TransportGraph;
import com.optimizer.backend.graph.TransportGraphLoader;
import com.optimizer.backend.ml.EtaPredictionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OptimizationBenchmarkTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private TransportModeRepository transportModeRepository;
    @Mock
    private CityRepository cityRepository;
    @Mock
    private OptimizationResultRepository optimizationResultRepository;

    @Mock
    private EtaPredictionService etaPredictionService;

    private TransportGraphLoader graphLoader;
    private OptimizationService optimizationService;

    private TransportMode road;
    private TransportMode rail;
    private TransportMode air;

    @BeforeEach
    void setUp() {
        road = TransportMode.builder().id(1L).name(TransportModeType.ROAD)
                .costPerKm(1.2).speed(60).carbonPerTonKm(0.062).build();
        rail = TransportMode.builder().id(2L).name(TransportModeType.RAIL)
                .costPerKm(0.8).speed(90).carbonPerTonKm(0.022).build();
        air = TransportMode.builder().id(3L).name(TransportModeType.AIR)
                .costPerKm(3.0).speed(700).carbonPerTonKm(0.602).build();

        graphLoader = new TransportGraphLoader(routeRepository, transportModeRepository, cityRepository);
        optimizationService = new OptimizationService(graphLoader, optimizationResultRepository, new TransferTimeCalculator(), etaPredictionService);
    }

    /**
     * Build a graph of N cities with all-pairs routes and 3 transport modes.
     */
    private List<Route> buildNGraph(int n) {
        List<City> cities = new ArrayList<>();
        double baseLat = 10.0;
        double baseLon = 70.0;
        for (int i = 0; i < n; i++) {
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
        return routes;
    }

    @Test
    void benchmark_20cities() {
        List<Route> routes = buildNGraph(20);
        List<City> cities = new ArrayList<>();
        double baseLat = 10.0;
        double baseLon = 70.0;
        for (int i = 0; i < 20; i++) {
            cities.add(City.builder().id((long) (i + 1)).name("City" + (i + 1))
                    .latitude(baseLat + i * 2).longitude(baseLon + i * 3).build());
        }

        when(routeRepository.findAll()).thenReturn(routes);
        when(transportModeRepository.findAll()).thenReturn(List.of(road, rail, air));
        when(cityRepository.findAll()).thenReturn(cities);
        when(optimizationResultRepository.findByShipmentId(any())).thenReturn(Optional.empty());
        when(optimizationResultRepository.save(any())).thenAnswer(inv -> {
            Object arg = inv.getArgument(0);
            if (arg instanceof OptimizationResult r) { r.setId(1L); return r; }
            return arg;
        });

        City src = routes.get(0).getSourceCity();
        City dst = routes.get(0).getDestinationCity();
        Shipment shipment = Shipment.builder().id(1L).sourceCity(src).destinationCity(dst)
                .weight(500).description("Benchmark").build();

        PathfindingAlgorithm astar = new AStarPathfinder();

        // Warm up
        optimizationService.optimize(shipment, OptimizationType.CHEAPEST, astar);

        // Reset mocks to count fresh
        reset(transportModeRepository);
        when(transportModeRepository.findAll()).thenReturn(List.of(road, rail, air));

        // Benchmark
        long start = System.nanoTime();
        int iterations = 100;
        for (int i = 0; i < iterations; i++) {
            reset(optimizationResultRepository);
            when(optimizationResultRepository.findByShipmentId(any())).thenReturn(Optional.empty());
            when(optimizationResultRepository.save(any())).thenAnswer(inv -> {
                Object arg = inv.getArgument(0);
                if (arg instanceof OptimizationResult r) { r.setId(1L); return r; }
                return arg;
            });
            optimizationService.optimize(shipment, OptimizationType.CHEAPEST, astar);
        }
        long elapsed = System.nanoTime() - start;

        double avgMs = (elapsed / 1_000_000.0) / iterations;

        System.out.println("=== A* Benchmark (20 cities, " + routes.size() + " routes) ===");
        System.out.println("Iterations: " + iterations);
        System.out.println("Average time per optimization: " + String.format("%.3f", avgMs) + " ms");
        System.out.println("Route DB calls per optimization: 1 (loaded once into graph)");

        // Verify: transportModeRepository.findAll() called exactly ONCE per iteration
        verify(transportModeRepository, times(iterations)).findAll();
    }

    @Test
    void databaseCallCount_singleOptimization() {
        List<Route> routes = buildNGraph(20);
        List<City> cities = new ArrayList<>();
        double baseLat = 10.0;
        double baseLon = 70.0;
        for (int i = 0; i < 20; i++) {
            cities.add(City.builder().id((long) (i + 1)).name("City" + (i + 1))
                    .latitude(baseLat + i * 2).longitude(baseLon + i * 3).build());
        }

        when(routeRepository.findAll()).thenReturn(routes);
        when(transportModeRepository.findAll()).thenReturn(List.of(road, rail, air));
        when(cityRepository.findAll()).thenReturn(cities);
        when(optimizationResultRepository.findByShipmentId(any())).thenReturn(Optional.empty());
        when(optimizationResultRepository.save(any())).thenAnswer(inv -> {
            Object arg = inv.getArgument(0);
            if (arg instanceof OptimizationResult r) { r.setId(1L); return r; }
            return arg;
        });

        City src = routes.get(0).getSourceCity();
        City dst = routes.get(10).getDestinationCity();
        Shipment shipment = Shipment.builder().id(1L).sourceCity(src).destinationCity(dst)
                .weight(500).description("Test").build();

        optimizationService.optimize(shipment, OptimizationType.CHEAPEST, new AStarPathfinder());

        // Verify: exactly 1 call to each repository
        verify(routeRepository, times(1)).findAll();
        verify(transportModeRepository, times(1)).findAll();
        verify(cityRepository, times(1)).findAll();
        verify(optimizationResultRepository, times(1)).findByShipmentId(any());
        verify(optimizationResultRepository, times(1)).save(any());

        System.out.println("=== DB Call Count (single optimization) ===");
        System.out.println("routeRepository.findAll(): 1");
        System.out.println("transportModeRepository.findAll(): 1");
        System.out.println("cityRepository.findAll(): 1");
        System.out.println("optimizationResultRepository.findByShipmentId(): 1");
        System.out.println("optimizationResultRepository.save(): 1");
        System.out.println("Total DB calls: 5");
    }
}
