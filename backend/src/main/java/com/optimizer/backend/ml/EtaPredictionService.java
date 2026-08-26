package com.optimizer.backend.ml;

import com.optimizer.backend.Entity.City;
import com.optimizer.backend.Entity.TransportModeType;
import com.optimizer.backend.graph.GraphEdge;
import com.optimizer.backend.graph.PathResult;
import com.optimizer.backend.graph.TransferTimeCalculator;
import com.optimizer.backend.graph.TransportGraph;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service that bridges the Spring Boot optimization pipeline with
 * the Python ML ETA prediction service.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Map optimization/shipment data to ML feature schema</li>
 *   <li>Call the Python inference service via {@link EtaPredictionClient}</li>
 *   <li>Handle failures gracefully (return null, never break optimization)</li>
 * </ul>
 *
 * <h3>Feature Mapping</h3>
 * <ul>
 *   <li>distance_km → total route distance from optimization</li>
 *   <li>shipment_weight_kg → shipment weight</li>
 *   <li>transport_mode → primary mode used in the route</li>
 *   <li>transfer_count → number of mode switches</li>
 *   <li>departure_hour → current hour if not specified</li>
 *   <li>day_of_week → current day if not specified</li>
 *   <li>month → current month if not specified</li>
 *   <li>traffic_level → default "MEDIUM" (not yet real-time)</li>
 *   <li>weather_condition → default "CLEAR" (not yet real-time)</li>
 *   <li>historical_delay_rate → default 0.1 (10% baseline)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class EtaPredictionService {

    private static final Logger log = LoggerFactory.getLogger(EtaPredictionService.class);

    /** Default traffic level when real-time data is unavailable. */
    private static final String DEFAULT_TRAFFIC_LEVEL = "MEDIUM";

    /** Default weather condition when real-time data is unavailable. */
    private static final String DEFAULT_WEATHER_CONDITION = "CLEAR";

    /** Default historical delay rate (10%) when no historical data is available. */
    private static final double DEFAULT_HISTORICAL_DELAY_RATE = 0.10;

    private final EtaPredictionClient etaPredictionClient;
    private final TransferTimeCalculator transferTimeCalculator;

    /**
     * Attempt to predict ETA for an optimization result.
     *
     * @param pathResult the optimization result containing route edges and totals
     * @param weight shipment weight in kg
     * @return prediction response wrapped in Optional.empty() if unavailable
     */
    public Optional<EtaPredictionResponse> predictEta(PathResult pathResult, double weight,
                                                        TransportGraph graph) {
        if (pathResult == null || !pathResult.hasPath()) {
            return Optional.empty();
        }

        try {
            EtaPredictionRequest request = buildRequest(pathResult, weight, graph);
            return etaPredictionClient.predictEta(request);
        } catch (Exception e) {
            log.warn("Failed to build ETA prediction request: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Build an ML prediction request from the optimization path result.
     *
     * <p>City IDs from graph edges are resolved to city names using the
     * {@link TransportGraph} so the ML model receives categorical feature
     * values that match its training vocabulary.</p>
     */
    EtaPredictionRequest buildRequest(PathResult pathResult, double weight, TransportGraph graph) {
        List<GraphEdge> edges = pathResult.edges();

        // Determine the primary transport mode (use the mode of the longest leg)
        String primaryMode = "ROAD"; // fallback
        double maxDistance = 0;
        for (GraphEdge edge : edges) {
            if (edge.distance() > maxDistance) {
                maxDistance = edge.distance();
                primaryMode = edge.modeType().name();
            }
        }

        // Count mode switches (= transfer count)
        int transferCount = countModeSwitches(edges);

        // Use current time for temporal features
        LocalDateTime now = LocalDateTime.now();

        // Source and destination city names — resolve IDs via the graph
        String sourceCity = "Unknown";
        String destinationCity = "Unknown";
        if (!edges.isEmpty()) {
            City src = graph.getCity(edges.get(0).sourceId());
            City dst = graph.getCity(edges.get(edges.size() - 1).destinationId());
            if (src != null) sourceCity = src.getName();
            if (dst != null) destinationCity = dst.getName();
        }

        return new EtaPredictionRequest(
                pathResult.totalDistance(),
                weight,
                now.getHour(),
                now.getDayOfWeek().getValue() - 1, // 0=Monday in model, 1=Monday in Java
                now.getMonthValue(),
                sourceCity,
                destinationCity,
                primaryMode,
                DEFAULT_TRAFFIC_LEVEL,
                DEFAULT_WEATHER_CONDITION,
                transferCount,
                DEFAULT_HISTORICAL_DELAY_RATE
        );
    }

    /**
     * Count the number of transport mode switches in the route.
     */
    private int countModeSwitches(List<GraphEdge> edges) {
        int switches = 0;
        for (int i = 1; i < edges.size(); i++) {
            if (edges.get(i).modeType() != edges.get(i - 1).modeType()) {
                switches++;
            }
        }
        return switches;
    }
}
