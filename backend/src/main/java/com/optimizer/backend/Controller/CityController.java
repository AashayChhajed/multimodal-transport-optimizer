package com.optimizer.backend.Controller;

import com.optimizer.backend.DTO.CityResponseDTO;
import com.optimizer.backend.Service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @GetMapping
    public ResponseEntity<List<CityResponseDTO>> getAllCities() {
        return ResponseEntity.ok(cityService.getAllCities());
    }
}
