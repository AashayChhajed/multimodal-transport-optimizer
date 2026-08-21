package com.optimizer.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "route",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"source_city_id", "destination_city_id", "transport_mode_id"}),
        indexes = {
                @Index(name = "idx_route_source", columnList = "source_city_id"),
                @Index(name = "idx_route_destination", columnList = "destination_city_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_city_id")
    private City sourceCity;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_city_id")
    private City destinationCity;

    @ManyToOne
    @JoinColumn(name = "transport_mode_id")
    private TransportMode transportMode;

    @Column(nullable = false)
    private double distance;
}
