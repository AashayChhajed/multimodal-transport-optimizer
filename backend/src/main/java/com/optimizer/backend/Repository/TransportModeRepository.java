package com.optimizer.backend.Repository;

import com.optimizer.backend.Entity.TransportMode;
import com.optimizer.backend.Entity.TransportModeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransportModeRepository extends JpaRepository<TransportMode, Long> {
    Optional<TransportMode> findByName(TransportModeType name);
}
