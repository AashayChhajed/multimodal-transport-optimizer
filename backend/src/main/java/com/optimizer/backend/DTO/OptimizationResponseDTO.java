package com.optimizer.backend.DTO;

import com.optimizer.backend.Entity.OptimizationType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OptimizationResponseDTO {
    private Long shipmentId;
    private OptimizationType optimizationType;
    private double totalCost;
    private double totalTime;
    private double totalDistance;
    private double totalCarbon;
    private List<String> cities;
    private List<RouteStepDTO> routes;

    @Data
    @Builder
    public static class RouteStepDTO {
        private String sourceCity;
        private String destinationCity;
        private String transportMode;
        private double distance;
        private double cost;
        private double time;
        private double carbon;
    }
}
