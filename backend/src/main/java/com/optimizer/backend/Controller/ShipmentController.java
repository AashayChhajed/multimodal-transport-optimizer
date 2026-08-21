package com.optimizer.backend.Controller;

import com.optimizer.backend.DTO.OptimizationResponseDTO;
import com.optimizer.backend.DTO.ShipmentRequestDTO;
import com.optimizer.backend.DTO.ShipmentResponseDTO;
import com.optimizer.backend.Entity.OptimizationType;
import com.optimizer.backend.Service.ShipmentService;
import com.optimizer.backend.graph.AlgorithmComparator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    public ResponseEntity<ShipmentResponseDTO> createShipment(@Valid @RequestBody ShipmentRequestDTO request) {
        ShipmentResponseDTO response = shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ShipmentResponseDTO>> getAllShipments() {
        return ResponseEntity.ok(shipmentService.getAllShipments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponseDTO> getShipment(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getShipment(id));
    }

    @PostMapping("/{id}/optimize")
    public ResponseEntity<OptimizationResponseDTO> optimizeShipment(
            @PathVariable Long id,
            @RequestParam(defaultValue = "CHEAPEST") OptimizationType optimizationType,
            @RequestParam(defaultValue = "ASTAR") String algorithm) {
        return ResponseEntity.ok(shipmentService.optimizeShipment(
                id, optimizationType, ShipmentService.parseAlgorithm(algorithm)));
    }

    @GetMapping("/{id}/compare")
    public ResponseEntity<AlgorithmComparator.ComparisonResult> compareAlgorithms(
            @PathVariable Long id,
            @RequestParam(defaultValue = "CHEAPEST") OptimizationType optimizationType) {
        return ResponseEntity.ok(shipmentService.compareAlgorithms(id, optimizationType));
    }
}
