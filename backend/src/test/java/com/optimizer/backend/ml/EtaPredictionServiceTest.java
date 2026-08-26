package com.optimizer.backend.ml;

import com.optimizer.backend.Entity.City;
import com.optimizer.backend.Entity.TransportMode;
import com.optimizer.backend.Entity.TransportModeType;
import com.optimizer.backend.graph.GraphEdge;
import com.optimizer.backend.graph.PathResult;
import com.optimizer.backend.graph.TransferTimeCalculator;
import com.optimizer.backend.graph.TransportGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EtaPredictionServiceTest {

    @Mock
    private EtaPredictionClient etaPredictionClient;

    private TransferTimeCalculator transferTimeCalculator;

    @InjectMocks
    private EtaPredictionService etaPredictionService;

    private TransportMode road;
    private TransportMode rail;
    private City mumbai;
    private City delhi;
    private City bengaluru;
    private TransportGraph graph;

    @BeforeEach
    void setUp() {
        transferTimeCalculator = new TransferTimeCalculator();
        etaPredictionService = new EtaPredictionService(etaPredictionClient, transferTimeCalculator);

        road = TransportMode.builder().id(1L).name(TransportModeType.ROAD)
                .costPerKm(1.2).speed(60).carbonPerTonKm(0.062).build();
        rail = TransportMode.builder().id(2L).name(TransportModeType.RAIL)
                .costPerKm(0.8).speed(90).carbonPerTonKm(0.022).build();

        mumbai = City.builder().id(1L).name("Mumbai").latitude(19.076).longitude(72.877).build();
        delhi = City.builder().id(2L).name("Delhi").latitude(28.613).longitude(77.209).build();
        bengaluru = City.builder().id(3L).name("Bengaluru").latitude(12.971).longitude(77.594).build();

        graph = new TransportGraph(
                Map.of(),
                Map.of(1L, mumbai, 2L, delhi, 3L, bengaluru),
                1.2, 60.0, 0.8, 700.0, 0.022
        );
    }

    // ── 1. Successful ML prediction ──

    @Test
    void predictEta_successfulPrediction_returnsResponse() {
        GraphEdge edge = new GraphEdge(1L, 2L, road, 500.0, 600.0, 8.33, 31.0);
        PathResult pathResult = new PathResult(List.of(edge), 500.0, 600.0, 8.33, 31.0, 10);

        EtaPredictionResponse mockResponse = new EtaPredictionResponse(12.5, "XGBoost", "1.0");
        when(etaPredictionClient.predictEta(any(EtaPredictionRequest.class)))
                .thenReturn(Optional.of(mockResponse));

        Optional<EtaPredictionResponse> result = etaPredictionService.predictEta(pathResult, 500.0, graph);

        assertTrue(result.isPresent());
        assertEquals(12.5, result.get().predictedEtaHours(), 0.01);
        assertEquals("XGBoost", result.get().model());
        verify(etaPredictionClient).predictEta(any(EtaPredictionRequest.class));
    }

    // ── 2. ML service unavailable ──

    @Test
    void predictEta_serviceUnavailable_returnsEmpty() {
        GraphEdge edge = new GraphEdge(1L, 2L, road, 500.0, 600.0, 8.33, 31.0);
        PathResult pathResult = new PathResult(List.of(edge), 500.0, 600.0, 8.33, 31.0, 10);

        when(etaPredictionClient.predictEta(any(EtaPredictionRequest.class)))
                .thenReturn(Optional.empty());

        Optional<EtaPredictionResponse> result = etaPredictionService.predictEta(pathResult, 500.0, graph);

        assertFalse(result.isPresent());
        verify(etaPredictionClient).predictEta(any(EtaPredictionRequest.class));
    }

    // ── 3. Null/empty path result ──

    @Test
    void predictEta_nullPathResult_returnsEmpty() {
        Optional<EtaPredictionResponse> result = etaPredictionService.predictEta(null, 500.0, graph);
        assertFalse(result.isPresent());
        verifyNoInteractions(etaPredictionClient);
    }

    @Test
    void predictEta_noPath_returnsEmpty() {
        PathResult noPath = PathResult.noPath(0);
        Optional<EtaPredictionResponse> result = etaPredictionService.predictEta(noPath, 500.0, graph);
        assertFalse(result.isPresent());
        verifyNoInteractions(etaPredictionClient);
    }

    // ── 4. Client throws exception ──

    @Test
    void predictEta_clientThrowsException_returnsEmpty() {
        GraphEdge edge = new GraphEdge(1L, 2L, road, 500.0, 600.0, 8.33, 31.0);
        PathResult pathResult = new PathResult(List.of(edge), 500.0, 600.0, 8.33, 31.0, 10);

        when(etaPredictionClient.predictEta(any(EtaPredictionRequest.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        Optional<EtaPredictionResponse> result = etaPredictionService.predictEta(pathResult, 500.0, graph);

        assertFalse(result.isPresent());
    }

    // ── 5. Correct request mapping with city NAMES, not IDs ──

    @Test
    void predictEta_buildsCorrectRequest_withCityNames() {
        GraphEdge edge = new GraphEdge(1L, 2L, road, 500.0, 600.0, 8.33, 31.0);
        PathResult pathResult = new PathResult(List.of(edge), 500.0, 600.0, 8.33, 31.0, 10);

        when(etaPredictionClient.predictEta(any(EtaPredictionRequest.class)))
                .thenReturn(Optional.of(new EtaPredictionResponse(12.5, "XGBoost", "1.0")));

        etaPredictionService.predictEta(pathResult, 500.0, graph);

        ArgumentCaptor<EtaPredictionRequest> captor = ArgumentCaptor.forClass(EtaPredictionRequest.class);
        verify(etaPredictionClient).predictEta(captor.capture());

        EtaPredictionRequest captured = captor.getValue();
        assertEquals(500.0, captured.distanceKm(), 0.01);
        assertEquals(500.0, captured.shipmentWeightKg(), 0.01);
        assertEquals("ROAD", captured.transportMode());
        assertEquals(0, captured.transferCount());
        assertEquals("MEDIUM", captured.trafficLevel());
        assertEquals("CLEAR", captured.weatherCondition());
        assertTrue(captured.historicalDelayRate() > 0);

        // Verify city NAMES, not numeric IDs
        assertEquals("Mumbai", captured.sourceCity());
        assertEquals("Delhi", captured.destinationCity());
    }

    // ── 6. City ID correctly maps to city name ──

    @Test
    void predictEta_sourceCityId_resolvesToName() {
        GraphEdge edge = new GraphEdge(1L, 2L, road, 500.0, 600.0, 8.33, 31.0);
        PathResult pathResult = new PathResult(List.of(edge), 500.0, 600.0, 8.33, 31.0, 10);

        when(etaPredictionClient.predictEta(any(EtaPredictionRequest.class)))
                .thenReturn(Optional.of(new EtaPredictionResponse(12.5, "XGBoost", "1.0")));

        etaPredictionService.predictEta(pathResult, 500.0, graph);

        ArgumentCaptor<EtaPredictionRequest> captor = ArgumentCaptor.forClass(EtaPredictionRequest.class);
        verify(etaPredictionClient).predictEta(captor.capture());

        // Source city ID 1 → "Mumbai"
        assertEquals("Mumbai", captor.getValue().sourceCity());
        assertNotEquals("1", captor.getValue().sourceCity(),
                "sourceCity must be a city name, not a numeric ID");
    }

    @Test
    void predictEta_destinationCityId_resolvesToName() {
        GraphEdge edge = new GraphEdge(1L, 3L, road, 900.0, 1080.0, 15.0, 55.8);
        PathResult pathResult = new PathResult(List.of(edge), 900.0, 1080.0, 15.0, 55.8, 10);

        when(etaPredictionClient.predictEta(any(EtaPredictionRequest.class)))
                .thenReturn(Optional.of(new EtaPredictionResponse(18.0, "XGBoost", "1.0")));

        etaPredictionService.predictEta(pathResult, 500.0, graph);

        ArgumentCaptor<EtaPredictionRequest> captor = ArgumentCaptor.forClass(EtaPredictionRequest.class);
        verify(etaPredictionClient).predictEta(captor.capture());

        // Destination city ID 3 → "Bengaluru"
        assertEquals("Bengaluru", captor.getValue().destinationCity());
        assertNotEquals("3", captor.getValue().destinationCity(),
                "destinationCity must be a city name, not a numeric ID");
    }

    // ── 7. Unknown city handled gracefully ──

    @Test
    void predictEta_unknownCityId_usesFallbackName() {
        // City ID 999 doesn't exist in the graph
        GraphEdge edge = new GraphEdge(999L, 2L, road, 500.0, 600.0, 8.33, 31.0);
        PathResult pathResult = new PathResult(List.of(edge), 500.0, 600.0, 8.33, 31.0, 10);

        when(etaPredictionClient.predictEta(any(EtaPredictionRequest.class)))
                .thenReturn(Optional.of(new EtaPredictionResponse(12.5, "XGBoost", "1.0")));

        etaPredictionService.predictEta(pathResult, 500.0, graph);

        ArgumentCaptor<EtaPredictionRequest> captor = ArgumentCaptor.forClass(EtaPredictionRequest.class);
        verify(etaPredictionClient).predictEta(captor.capture());

        // Unknown city should use "Unknown" fallback, not crash
        assertEquals("Unknown", captor.getValue().sourceCity());
        assertEquals("Delhi", captor.getValue().destinationCity());
    }

    // ── 8. Multi-leg route with transfers ──

    @Test
    void predictEta_multiModeRoute_countsTransfersCorrectly() {
        GraphEdge roadEdge = new GraphEdge(1L, 2L, road, 300.0, 360.0, 5.0, 18.6);
        GraphEdge railEdge = new GraphEdge(2L, 3L, rail, 200.0, 160.0, 2.22, 4.4);
        PathResult pathResult = new PathResult(
                List.of(roadEdge, railEdge), 500.0, 520.0, 7.22, 23.0, 15);

        when(etaPredictionClient.predictEta(any(EtaPredictionRequest.class)))
                .thenReturn(Optional.of(new EtaPredictionResponse(10.0, "XGBoost", "1.0")));

        etaPredictionService.predictEta(pathResult, 500.0, graph);

        ArgumentCaptor<EtaPredictionRequest> captor = ArgumentCaptor.forClass(EtaPredictionRequest.class);
        verify(etaPredictionClient).predictEta(captor.capture());

        assertEquals(1, captor.getValue().transferCount());
        // Source should be Mumbai (ID 1), destination Bengaluru (ID 3)
        assertEquals("Mumbai", captor.getValue().sourceCity());
        assertEquals("Bengaluru", captor.getValue().destinationCity());
    }

    // ── 9. Response fields correct ──

    @Test
    void predictEta_responseFieldsAreCorrect() {
        EtaPredictionResponse response = new EtaPredictionResponse(24.8, "XGBoost", "1.0");
        assertEquals(24.8, response.predictedEtaHours(), 0.01);
        assertEquals("XGBoost", response.model());
        assertEquals("1.0", response.modelVersion());
    }
}
