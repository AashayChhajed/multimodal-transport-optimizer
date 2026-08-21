package com.optimizer.backend.graph;

import com.optimizer.backend.Entity.TransportMode;
import com.optimizer.backend.Entity.TransportModeType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransferTimeCalculatorTest {

    private final TransferTimeCalculator calculator = new TransferTimeCalculator();

    @Test
    void sameMode_noTransfer() {
        assertEquals(0.0, calculator.transferTime(TransportModeType.ROAD, TransportModeType.ROAD),
                "Same mode should have 0 transfer time");
        assertEquals(0.0, calculator.transferTime(TransportModeType.RAIL, TransportModeType.RAIL),
                "Same mode should have 0 transfer time");
        assertEquals(0.0, calculator.transferTime(TransportModeType.AIR, TransportModeType.AIR),
                "Same mode should have 0 transfer time");
    }

    @Test
    void roadToRail_transfer() {
        assertEquals(0.5, calculator.transferTime(TransportModeType.ROAD, TransportModeType.RAIL),
                "ROAD→RAIL should have 0.5h transfer");
    }

    @Test
    void railToAir_transfer() {
        assertEquals(0.5, calculator.transferTime(TransportModeType.RAIL, TransportModeType.AIR),
                "RAIL→AIR should have 0.5h transfer");
    }

    @Test
    void roadToAir_transfer() {
        assertEquals(0.5, calculator.transferTime(TransportModeType.ROAD, TransportModeType.AIR),
                "ROAD→AIR should have 0.5h transfer");
    }

    @Test
    void firstLeg_noTransfer() {
        assertEquals(0.0, calculator.transferTime(null, TransportModeType.ROAD),
                "First leg (null previous) should have 0 transfer time");
    }

    @Test
    void multipleEdges_roadRailAir_twoTransfers() {
        // ROAD→RAIL→AIR = 2 transfers × 0.5h = 1.0h
        List<GraphEdge> edges = List.of(
                new GraphEdge(1L, 2L, createMode(TransportModeType.ROAD), 100, 120, 1.67, 6.2),
                new GraphEdge(2L, 3L, createMode(TransportModeType.RAIL), 100, 80, 1.11, 2.2),
                new GraphEdge(3L, 4L, createMode(TransportModeType.AIR), 100, 300, 0.14, 60.2)
        );

        assertEquals(1.0, calculator.totalTransferTime(edges),
                "ROAD→RAIL→AIR should have 1.0h total transfer time");
    }

    @Test
    void multipleEdges_sameMode_noTransfer() {
        List<GraphEdge> edges = List.of(
                new GraphEdge(1L, 2L, createMode(TransportModeType.ROAD), 100, 120, 1.67, 6.2),
                new GraphEdge(2L, 3L, createMode(TransportModeType.ROAD), 200, 240, 3.33, 12.4),
                new GraphEdge(3L, 4L, createMode(TransportModeType.ROAD), 150, 180, 2.5, 9.3)
        );

        assertEquals(0.0, calculator.totalTransferTime(edges),
                "All same mode should have 0 transfer time");
    }

    @Test
    void singleEdge_noTransfer() {
        List<GraphEdge> edges = List.of(
                new GraphEdge(1L, 2L, createMode(TransportModeType.ROAD), 100, 120, 1.67, 6.2)
        );

        assertEquals(0.0, calculator.totalTransferTime(edges),
                "Single edge should have 0 transfer time");
    }

    @Test
    void emptyEdges_noTransfer() {
        assertEquals(0.0, calculator.totalTransferTime(List.of()),
                "Empty edges should have 0 transfer time");
        assertEquals(0.0, calculator.totalTransferTime(null),
                "Null edges should have 0 transfer time");
    }

    @Test
    void threeTransfers_roadRailAirroad() {
        // ROAD→RAIL→AIR→ROAD = 3 transfers × 0.5h = 1.5h
        List<GraphEdge> edges = List.of(
                new GraphEdge(1L, 2L, createMode(TransportModeType.ROAD), 100, 120, 1.67, 6.2),
                new GraphEdge(2L, 3L, createMode(TransportModeType.RAIL), 100, 80, 1.11, 2.2),
                new GraphEdge(3L, 4L, createMode(TransportModeType.AIR), 100, 300, 0.14, 60.2),
                new GraphEdge(4L, 5L, createMode(TransportModeType.ROAD), 100, 120, 1.67, 6.2)
        );

        assertEquals(1.5, calculator.totalTransferTime(edges),
                "ROAD→RAIL→AIR→ROAD should have 1.5h total transfer time");
    }

    @Test
    void customTransferTime() {
        TransferTimeCalculator custom = new TransferTimeCalculator(1.0);
        assertEquals(1.0, custom.transferTime(TransportModeType.ROAD, TransportModeType.RAIL),
                "Custom transfer time should be used");
    }

    private TransportMode createMode(TransportModeType type) {
        return switch (type) {
            case ROAD -> TransportMode.builder().id(1L).name(type).costPerKm(1.2).speed(60).carbonPerTonKm(0.062).build();
            case RAIL -> TransportMode.builder().id(2L).name(type).costPerKm(0.8).speed(90).carbonPerTonKm(0.022).build();
            case AIR -> TransportMode.builder().id(3L).name(type).costPerKm(3.0).speed(700).carbonPerTonKm(0.602).build();
        };
    }
}
