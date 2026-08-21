package com.optimizer.backend.graph;

import com.optimizer.backend.Entity.TransportModeType;
import org.springframework.stereotype.Component;

/**
 * Calculates transfer time penalty when consecutive route legs use different
 * transport modes.
 *
 * <p>Transfer time represents the realistic delay when switching between
 * transportation modes (e.g., loading/unloading cargo from a truck to a train).
 * The penalty applies only to totalTime and does not affect distance, cost,
 * or carbon emissions.</p>
 *
 * <h3>Default behavior</h3>
 * <ul>
 *   <li>ROAD → ROAD: 0 hours (no transfer)</li>
 *   <li>ROAD → RAIL: 0.5 hours (30 min transfer)</li>
 *   <li>RAIL → AIR: 0.5 hours (30 min transfer)</li>
 *   <li>ROAD → RAIL → AIR: 1.0 hours total (2 transfers × 0.5h)</li>
 * </ul>
 */
@Component
public class TransferTimeCalculator {

    /** Default transfer time: 30 minutes (0.5 hours). */
    public static final double DEFAULT_TRANSFER_HOURS = 0.5;

    private final double transferTimeHours;

    public TransferTimeCalculator() {
        this.transferTimeHours = DEFAULT_TRANSFER_HOURS;
    }

    /**
     * @param transferTimeHours custom transfer time in hours
     */
    public TransferTimeCalculator(double transferTimeHours) {
        this.transferTimeHours = transferTimeHours;
    }

    /**
     * Calculate transfer time penalty between two consecutive legs.
     *
     * @param previousMode mode of the previous leg (null if this is the first leg)
     * @param currentMode  mode of the current leg
     * @return transfer time in hours (0 if modes are the same or previous is null)
     */
    public double transferTime(TransportModeType previousMode, TransportModeType currentMode) {
        if (previousMode == null || previousMode == currentMode) {
            return 0.0;
        }
        return transferTimeHours;
    }

    /**
     * Calculate total transfer time for an ordered list of edges.
     *
     * @param edges ordered edges forming a path
     * @return total transfer time in hours
     */
    public double totalTransferTime(java.util.List<GraphEdge> edges) {
        if (edges == null || edges.size() <= 1) {
            return 0.0;
        }
        double total = 0.0;
        for (int i = 1; i < edges.size(); i++) {
            total += transferTime(edges.get(i - 1).modeType(), edges.get(i).modeType());
        }
        return total;
    }
}
