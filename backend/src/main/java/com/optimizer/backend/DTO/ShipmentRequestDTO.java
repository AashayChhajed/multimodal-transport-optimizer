package com.optimizer.backend.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ShipmentRequestDTO {

    @NotNull
    private Long sourceCityId;

    @NotNull
    private Long destinationCityId;

    @Positive
    private double weight;

    @NotNull
    private String description;
}
