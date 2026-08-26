package com.optimizer.backend.ml;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Python ML inference service.
 *
 * <p>Configured via application.properties:
 * <pre>
 * ml.service.url=http://localhost:5000
 * ml.service.timeout=5000
 * </pre>
 *
 * <p>The ML_SERVICE_URL environment variable overrides the default URL.
 */
@ConfigurationProperties(prefix = "ml.service")
public record MlServiceProperties(
        String url,
        int timeout
) {
    public MlServiceProperties {
        if (url == null || url.isBlank()) {
            url = "http://localhost:5000";
        }
        if (timeout <= 0) {
            timeout = 5000;
        }
    }
}
