package com.optimizer.backend.Service;

/**
 * Utility class for transportation cost, time, and carbon calculations.
 *
 * <h3>Weight-Aware Cost Model</h3>
 * <pre>cost = distance × costPerKm × (1 + weightKg × WEIGHT_FACTOR)</pre>
 *
 * WEIGHT_FACTOR = 0.0001 (0.01% per kg).
 * Reasoning: heavier shipments consume more fuel and require more handling.
 * At 1000 kg the cost increases by 10%, at 5000 kg by 50%. This is a simple
 * multiplicative model that keeps cost proportional to both distance and weight
 * without requiring a separate per-kg charge.
 *
 * <h3>Carbon Emissions Model</h3>
 * <pre>carbon = distance × (weightKg / 1000) × carbonPerTonKm</pre>
 *
 * Synthetic emission coefficients used for comparative optimization experiments.
 * <ul>
 *   <li>ROAD:  0.062 kg CO₂ per ton-km</li>
 *   <li>RAIL:  0.022 kg CO₂ per ton-km</li>
 *   <li>AIR:   0.602 kg CO₂ per ton-km</li>
 * </ul>
 *
 * These are <em>synthetic values</em> for this project. They are not sourced from
 * a specific standard or dataset. They reflect the general ordering:
 * rail &lt; road &lt; air.
 */
public final class CostCalculator {

    /** 0.01% cost increase per kg of shipment weight. */
    public static final double WEIGHT_FACTOR = 0.0001;

    private CostCalculator() { }

    /**
     * Calculate transportation cost for a route leg.
     *
     * @param distanceKm  route distance in kilometres
     * @param costPerKm   transport mode cost per km (e.g. $/km)
     * @param weightKg    shipment weight in kilograms
     * @return cost in the same currency unit as costPerKm
     */
    public static double calculateCost(double distanceKm, double costPerKm, double weightKm) {
        return distanceKm * costPerKm * (1 + weightKm * WEIGHT_FACTOR);
    }

    /**
     * Calculate travel time for a route leg.
     *
     * @param distanceKm route distance in kilometres
     * @param speedKmh   transport mode speed in km/h
     * @return time in hours
     */
    public static double calculateTime(double distanceKm, double speedKmh) {
        return distanceKm / speedKmh;
    }

    /**
     * Calculate carbon emissions for a route leg.
     *
     * @param distanceKm       route distance in kilometres
     * @param weightKg         shipment weight in kilograms
     * @param carbonPerTonKm   emission factor in kg CO₂ per ton-km
     * @return carbon emissions in kg CO₂
     */
    public static double calculateCarbon(double distanceKm, double weightKg, double carbonPerTonKm) {
        return distanceKm * (weightKg / 1000.0) * carbonPerTonKm;
    }

    /**
     * Haversine distance between two geographic coordinates.
     *
     * @return great-circle distance in kilometres
     */
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
