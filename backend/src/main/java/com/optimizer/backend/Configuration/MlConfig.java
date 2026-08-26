package com.optimizer.backend.Configuration;

import com.optimizer.backend.ml.MlServiceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration for ML service integration.
 *
 * <p>Enables the {@link MlServiceProperties} configuration properties
 * and provides a RestTemplate bean for HTTP communication with the
 * Python ML inference service.
 */
@Configuration
@EnableConfigurationProperties(MlServiceProperties.class)
public class MlConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
