package com.optimizer.backend.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for the Python ETA prediction service.
 *
 * <p>All fields are required and match the XGBoost model's training schema exactly.
 * Feature names and types must not change without retraining the model.
 */
public record EtaPredictionRequest(
        @JsonProperty("distance_km") double distanceKm,
        @JsonProperty("shipment_weight_kg") double shipmentWeightKg,
        @JsonProperty("departure_hour") int departureHour,
        @JsonProperty("day_of_week") int dayOfWeek,
        @JsonProperty("month") int month,
        @JsonProperty("source_city") String sourceCity,
        @JsonProperty("destination_city") String destinationCity,
        @JsonProperty("transport_mode") String transportMode,
        @JsonProperty("traffic_level") String trafficLevel,
        @JsonProperty("weather_condition") String weatherCondition,
        @JsonProperty("transfer_count") int transferCount,
        @JsonProperty("historical_delay_rate") double historicalDelayRate
) {
}
