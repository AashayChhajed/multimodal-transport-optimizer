package com.optimizer.backend.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipmentResponseDTO {
    private Long id;
    private Long sourceCityId;
    private String sourceCityName;
    private Long destinationCityId;
    private String destinationCityName;
    private double weight;
    private String description;
}
