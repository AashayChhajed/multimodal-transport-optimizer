package com.optimizer.backend.Repository;

import com.optimizer.backend.Entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
}
