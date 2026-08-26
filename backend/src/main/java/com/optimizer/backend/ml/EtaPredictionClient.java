package com.optimizer.backend.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * HTTP client that communicates with the Python ML inference service.
 *
 * <p>This client calls the Python service's /predict-eta endpoint.
 * It handles timeouts gracefully and returns empty Optional on any
 * failure so that route optimization is never blocked by ML failures.
 */
@Component
public class EtaPredictionClient {

    private static final Logger log = LoggerFactory.getLogger(EtaPredictionClient.class);

    private final RestTemplate restTemplate;
    private final MlServiceProperties properties;

    public EtaPredictionClient(RestTemplate restTemplate, MlServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Request an ETA prediction from the Python ML service.
     *
     * @param request prediction request with all required features
     * @return prediction response, or empty if the service is unavailable
     */
    public Optional<EtaPredictionResponse> predictEta(EtaPredictionRequest request) {
        String url = properties.url() + "/predict-eta";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<EtaPredictionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<EtaPredictionResponse> response = restTemplate.postForEntity(
                    url, entity, EtaPredictionResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(response.getBody());
            }

            log.warn("ML service returned non-success status: {}", response.getStatusCode());
            return Optional.empty();

        } catch (ResourceAccessException e) {
            log.warn("ML service unavailable at {}: {}", url, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Failed to get ETA prediction from ML service: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Check if the ML service is healthy.
     *
     * @return true if the service is reachable and the model is loaded
     */
    public boolean isHealthy() {
        String url = properties.url() + "/health";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().contains("\"model_loaded\":true");
            }
            return false;
        } catch (Exception e) {
            log.debug("ML service health check failed: {}", e.getMessage());
            return false;
        }
    }
}
