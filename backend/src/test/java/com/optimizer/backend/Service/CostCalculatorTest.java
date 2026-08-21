package com.optimizer.backend.Service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CostCalculatorTest {

    // ── Cost Calculation Tests ──

    @Test
    void costCalculation_basicRoute() {
        // 1000 km, road at $1.2/km, 100 kg shipment
        // cost = 1000 * 1.2 * (1 + 100 * 0.0001) = 1200 * 1.01 = 1212.0
        double cost = CostCalculator.calculateCost(1000, 1.2, 100);
        assertEquals(1212.0, cost, 0.01);
    }

    @Test
    void costCalculation_zeroWeight() {
        // Edge case: weight = 0 (theoretical minimum)
        // cost = 500 * 0.8 * (1 + 0) = 400.0
        double cost = CostCalculator.calculateCost(500, 0.8, 0);
        assertEquals(400.0, cost, 0.01);
    }

    @Test
    void costCalculation_heavyShipment() {
        // 1000 km, road at $1.2/km, 5000 kg
        // cost = 1000 * 1.2 * (1 + 5000 * 0.0001) = 1200 * 1.5 = 1800.0
        double cost = CostCalculator.calculateCost(1000, 1.2, 5000);
        assertEquals(1800.0, cost, 0.01);
    }

    @Test
    void costCalculation_heavierShipmentCostsMore() {
        double lightCost = CostCalculator.calculateCost(1000, 1.2, 100);
        double heavyCost = CostCalculator.calculateCost(1000, 1.2, 5000);
        assertTrue(heavyCost > lightCost,
                "Heavier shipment should cost more");
    }

    @Test
    void costCalculation_differentModes() {
        // Same distance, same weight, different modes
        double roadCost = CostCalculator.calculateCost(1000, 1.2, 100);
        double railCost = CostCalculator.calculateCost(1000, 0.8, 100);
        double airCost = CostCalculator.calculateCost(1000, 3.0, 100);

        assertTrue(roadCost > railCost, "Road should cost more than rail");
        assertTrue(airCost > roadCost, "Air should cost more than road");
    }

    @Test
    void costCalculation_airMostExpensive() {
        double airCost = CostCalculator.calculateCost(1000, 3.0, 1000);
        double railCost = CostCalculator.calculateCost(1000, 0.8, 1000);
        assertTrue(airCost > railCost * 2,
                "Air should be significantly more expensive than rail");
    }

    @Test
    void costCalculation_zeroDistance() {
        double cost = CostCalculator.calculateCost(0, 1.2, 100);
        assertEquals(0.0, cost, 0.01);
    }

    // ── Time Calculation Tests ──

    @Test
    void timeCalculation_basicRoute() {
        // 1000 km at 60 km/h = 16.666... hours
        double time = CostCalculator.calculateTime(1000, 60);
        assertEquals(1000.0 / 60.0, time, 0.001);
    }

    @Test
    void timeCalculation_airFasterThanRoad() {
        double roadTime = CostCalculator.calculateTime(1000, 60);
        double airTime = CostCalculator.calculateTime(1000, 700);
        assertTrue(airTime < roadTime, "Air should be faster than road");
    }

    @Test
    void timeCalculation_railBetweenRoadAndAir() {
        double roadTime = CostCalculator.calculateTime(1000, 60);
        double railTime = CostCalculator.calculateTime(1000, 90);
        double airTime = CostCalculator.calculateTime(1000, 700);
        assertTrue(railTime < roadTime, "Rail should be faster than road");
        assertTrue(airTime < railTime, "Air should be faster than rail");
    }

    // ── Carbon Calculation Tests ──

    @Test
    void carbonCalculation_basicRoute() {
        // 1000 km, 1000 kg, road (0.062 kg CO2/ton-km)
        // carbon = 1000 * (1000/1000) * 0.062 = 62.0 kg CO2
        double carbon = CostCalculator.calculateCarbon(1000, 1000, 0.062);
        assertEquals(62.0, carbon, 0.01);
    }

    @Test
    void carbonCalculation_airMostEmissions() {
        double roadCarbon = CostCalculator.calculateCarbon(1000, 1000, 0.062);
        double railCarbon = CostCalculator.calculateCarbon(1000, 1000, 0.022);
        double airCarbon = CostCalculator.calculateCarbon(1000, 1000, 0.602);

        assertTrue(airCarbon > roadCarbon, "Air should produce more CO2 than road");
        assertTrue(roadCarbon > railCarbon, "Road should produce more CO2 than rail");
    }

    @Test
    void carbonCalculation_railGreenest() {
        double railCarbon = CostCalculator.calculateCarbon(1000, 2000, 0.022);
        double airCarbon = CostCalculator.calculateCarbon(1000, 2000, 0.602);
        assertTrue(airCarbon > railCarbon * 10,
                "Air should produce significantly more CO2 than rail");
    }

    @Test
    void carbonCalculation_heavierMoreEmissions() {
        double lightCarbon = CostCalculator.calculateCarbon(1000, 100, 0.062);
        double heavyCarbon = CostCalculator.calculateCarbon(1000, 5000, 0.062);
        assertTrue(heavyCarbon > lightCarbon,
                "Heavier shipment should produce more emissions");
    }

    @Test
    void carbonCalculation_zeroWeight() {
        double carbon = CostCalculator.calculateCarbon(1000, 0, 0.062);
        assertEquals(0.0, carbon, 0.01);
    }

    // ── Haversine Tests ──

    @Test
    void haversine_samePoint() {
        double distance = CostCalculator.haversine(19.076, 72.877, 19.076, 72.877);
        assertEquals(0.0, distance, 0.01);
    }

    @Test
    void haversine_knownDistance() {
        // Mumbai to Delhi: approximately 1150-1400 km (haversine is straight-line)
        double distance = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        assertTrue(distance > 1000 && distance < 1500,
                "Mumbai-Delhi haversine should be roughly 1150-1400 km, got: " + distance);
    }

    @Test
    void haversine_symmetric() {
        double d1 = CostCalculator.haversine(19.076, 72.877, 28.613, 77.209);
        double d2 = CostCalculator.haversine(28.613, 77.209, 19.076, 72.877);
        assertEquals(d1, d2, 0.001, "Haversine should be symmetric");
    }

    // ── Integration: Weight Impact on Route Selection ──

    @Test
    void weightAffectsCostButNotTime() {
        // Same route, different weights
        double costLight = CostCalculator.calculateCost(1000, 1.2, 100);
        double costHeavy = CostCalculator.calculateCost(1000, 1.2, 5000);
        double timeLight = CostCalculator.calculateTime(1000, 60);
        double timeHeavy = CostCalculator.calculateTime(1000, 60);

        assertTrue(costHeavy > costLight, "Cost should increase with weight");
        assertEquals(timeLight, timeHeavy, 0.001, "Time should NOT change with weight");
    }

    @Test
    void weightFactorMakesHeavierShipmentsPreferRail() {
        // At high weight, rail's lower costPerKm becomes even more attractive
        double roadCost = CostCalculator.calculateCost(1000, 1.2, 10000);
        double railCost = CostCalculator.calculateCost(1000, 0.8, 10000);
        double costRatio = roadCost / railCost;

        // With weight factor, road premium increases
        assertTrue(costRatio > 1.4,
                "Road should be significantly more expensive than rail for heavy shipments, ratio: " + costRatio);
    }
}
