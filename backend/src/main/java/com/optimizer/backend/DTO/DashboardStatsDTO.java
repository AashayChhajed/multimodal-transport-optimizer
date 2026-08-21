package com.optimizer.backend.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsDTO {
    private long totalShipments;
    private long optimizedShipments;
    private long totalRoutes;
    private Double averageCost;
    private Double averageTime;
    private Double totalDistance;
    private Double totalCarbon;
}
