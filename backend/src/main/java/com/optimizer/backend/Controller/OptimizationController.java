package com.optimizer.backend.Controller;

import com.optimizer.backend.DTO.OptimizationResponseDTO;
import com.optimizer.backend.Service.OptimizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/optimization")
@RequiredArgsConstructor
public class OptimizationController {

    private final OptimizationService optimizationService;

    @GetMapping("/{shipmentId}")
    public ResponseEntity<OptimizationResponseDTO> getOptimizationResult(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(optimizationService.getByShipmentId(shipmentId));
    }
}
