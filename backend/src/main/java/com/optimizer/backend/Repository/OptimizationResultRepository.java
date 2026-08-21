package com.optimizer.backend.Repository;

import com.optimizer.backend.Entity.OptimizationResult;
import com.optimizer.backend.Entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OptimizationResultRepository extends JpaRepository<OptimizationResult, Long> {
    Optional<OptimizationResult> findByShipment(Shipment shipment);

    Optional<OptimizationResult> findByShipmentId(Long shipmentId);
}
