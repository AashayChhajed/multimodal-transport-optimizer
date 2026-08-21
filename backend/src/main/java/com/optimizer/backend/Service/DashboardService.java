package com.optimizer.backend.Service;

import com.optimizer.backend.DTO.DashboardStatsDTO;
import com.optimizer.backend.Repository.OptimizationResultRepository;
import com.optimizer.backend.Repository.RouteRepository;
import com.optimizer.backend.Repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ShipmentRepository shipmentRepository;
    private final OptimizationResultRepository optimizationResultRepository;
    private final RouteRepository routeRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDTO getStats() {
        long totalShipments = shipmentRepository.count();
        long totalRoutes = routeRepository.count();

        var results = optimizationResultRepository.findAll();
        long optimizedShipments = results.size();

        double avgCost = results.stream()
                .mapToDouble(r -> r.getTotalCost() != null ? r.getTotalCost() : 0.0)
                .average().orElse(0.0);

        double avgTime = results.stream()
                .mapToDouble(r -> r.getTotalTime() != null ? r.getTotalTime() : 0.0)
                .average().orElse(0.0);

        double totalDistance = results.stream()
                .mapToDouble(r -> r.getTotalDistance() != null ? r.getTotalDistance() : 0.0)
                .sum();

        double totalCarbon = results.stream()
                .mapToDouble(r -> r.getTotalCarbon() != null ? r.getTotalCarbon() : 0.0)
                .sum();

        return DashboardStatsDTO.builder()
                .totalShipments(totalShipments)
                .optimizedShipments(optimizedShipments)
                .totalRoutes(totalRoutes)
                .averageCost(avgCost)
                .averageTime(avgTime)
                .totalDistance(totalDistance)
                .totalCarbon(totalCarbon)
                .build();
    }
}
