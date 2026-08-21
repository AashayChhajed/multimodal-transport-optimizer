package com.optimizer.backend.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CityResponseDTO {
    private Long id;
    private String name;
    private double latitude;
    private double longitude;
}
