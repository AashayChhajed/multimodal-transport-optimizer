package com.optimizer.backend.Service;

import com.optimizer.backend.DTO.CityResponseDTO;
import com.optimizer.backend.Repository.CityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;

    @Transactional(readOnly = true)
    public List<CityResponseDTO> getAllCities() {
        return cityRepository.findAll().stream()
                .map(city -> CityResponseDTO.builder()
                        .id(city.getId())
                        .name(city.getName())
                        .latitude(city.getLatitude())
                        .longitude(city.getLongitude())
                        .build())
                .toList();
    }
}
