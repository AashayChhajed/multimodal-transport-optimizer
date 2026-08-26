package com.optimizer.backend.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO from the Python ETA prediction service.
 */
public record EtaPredictionResponse(
        @JsonProperty("predicted_eta_hours") double predictedEtaHours,
        @JsonProperty("model") String model,
        @JsonProperty("model_version") String modelVersion
) {
}
