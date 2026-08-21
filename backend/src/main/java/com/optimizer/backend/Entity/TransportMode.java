package com.optimizer.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transport_mode")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportMode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private TransportModeType name;

    @Column(nullable = false)
    private double costPerKm;

    @Column(nullable = false)
    private double speed;

    /**
     * Carbon emission factor in kg CO₂ per ton-km.
     * Synthetic emission coefficients used for comparative optimization experiments:
     *   ROAD = 0.062, RAIL = 0.022, AIR = 0.602
     */
    @Column(nullable = false)
    private double carbonPerTonKm;
}
