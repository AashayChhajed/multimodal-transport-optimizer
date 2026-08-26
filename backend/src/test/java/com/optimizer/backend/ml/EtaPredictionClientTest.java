package com.optimizer.backend.ml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EtaPredictionClientTest {

    @Mock
    private RestTemplate restTemplate;

    private MlServiceProperties properties;
    private EtaPredictionClient client;

    @BeforeEach
    void setUp() {
        properties = new MlServiceProperties("http://localhost:5000", 5000);
        client = new EtaPredictionClient(restTemplate, properties);
    }

    private EtaPredictionRequest sampleRequest() {
        return new EtaPredictionRequest(
                500.0, 500.0, 10, 2, 8,
                "1", "2", "ROAD", "MEDIUM", "CLEAR", 1, 0.10
        );
    }

    // ── 1. Successful prediction ──

    @Test
    void predictEta_successful_returnsResponse() {
        EtaPredictionResponse mockResponse = new EtaPredictionResponse(12.5, "XGBoost", "1.0");
        when(restTemplate.postForEntity(
                eq("http://localhost:5000/predict-eta"),
                any(HttpEntity.class),
                eq(EtaPredictionResponse.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        Optional<EtaPredictionResponse> result = client.predictEta(sampleRequest());

        assertTrue(result.isPresent());
        assertEquals(12.5, result.get().predictedEtaHours(), 0.01);
    }

    // ── 2. Service unavailable (connection refused) ──

    @Test
    void predictEta_connectionRefused_returnsEmpty() {
        when(restTemplate.postForEntity(
                anyString(), any(HttpEntity.class), eq(EtaPredictionResponse.class)
        )).thenThrow(new ResourceAccessException("Connection refused"));

        Optional<EtaPredictionResponse> result = client.predictEta(sampleRequest());

        assertFalse(result.isPresent());
    }

    // ── 3. Malformed response ──

    @Test
    void predictEta_nullBody_returnsEmpty() {
        when(restTemplate.postForEntity(
                anyString(), any(HttpEntity.class), eq(EtaPredictionResponse.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.OK));

        Optional<EtaPredictionResponse> result = client.predictEta(sampleRequest());

        assertFalse(result.isPresent());
    }

    // ── 4. Timeout error ──

    @Test
    void predictEta_timeout_returnsEmpty() {
        when(restTemplate.postForEntity(
                anyString(), any(HttpEntity.class), eq(EtaPredictionResponse.class)
        )).thenThrow(new ResourceAccessException("Read timed out"));

        Optional<EtaPredictionResponse> result = client.predictEta(sampleRequest());

        assertFalse(result.isPresent());
    }

    // ── 5. Server error ──

    @Test
    void predictEta_serverError_returnsEmpty() {
        when(restTemplate.postForEntity(
                anyString(), any(HttpEntity.class), eq(EtaPredictionResponse.class)
        )).thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        Optional<EtaPredictionResponse> result = client.predictEta(sampleRequest());

        assertFalse(result.isPresent());
    }

    // ── 6. Health check ──

    @Test
    void isHealthy_serviceUp_modelLoaded_returnsTrue() {
        when(restTemplate.getForEntity("http://localhost:5000/health", String.class))
                .thenReturn(new ResponseEntity<>("{\"status\":\"UP\",\"model_loaded\":true}", HttpStatus.OK));

        assertTrue(client.isHealthy());
    }

    @Test
    void isHealthy_serviceDown_returnsFalse() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertFalse(client.isHealthy());
    }

    // ── 7. Correct URL ──

    @Test
    void predictEta_usesCorrectUrl() {
        MlServiceProperties customProps = new MlServiceProperties("http://ml-host:8080", 3000);
        EtaPredictionClient customClient = new EtaPredictionClient(restTemplate, customProps);

        EtaPredictionResponse mockResponse = new EtaPredictionResponse(10.0, "XGBoost", "1.0");
        when(restTemplate.postForEntity(
                eq("http://ml-host:8080/predict-eta"),
                any(HttpEntity.class),
                eq(EtaPredictionResponse.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        customClient.predictEta(sampleRequest());

        verify(restTemplate).postForEntity(
                eq("http://ml-host:8080/predict-eta"),
                any(HttpEntity.class),
                eq(EtaPredictionResponse.class)
        );
    }
}
