package com.optimizer.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shipment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_city_id")
    private City sourceCity;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_city_id")
    private City destinationCity;

    @Column(nullable = false)
    private double weight;

    @Column(nullable = false)
    private String description;
}
