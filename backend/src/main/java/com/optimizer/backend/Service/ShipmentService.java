package com.optimizer.backend.Service;

import com.optimizer.backend.DTO.OptimizationResponseDTO;
import com.optimizer.backend.DTO.ShipmentRequestDTO;
import com.optimizer.backend.DTO.ShipmentResponseDTO;
import com.optimizer.backend.Entity.City;
import com.optimizer.backend.Entity.OptimizationType;
import com.optimizer.backend.Entity.Shipment;
import com.optimizer.backend.Exception.BadRequestException;
import com.optimizer.backend.Exception.ResourceNotFoundException;
import com.optimizer.backend.Repository.CityRepository;
import com.optimizer.backend.Repository.ShipmentRepository;
import com.optimizer.backend.graph.AlgorithmComparator;
import com.optimizer.backend.graph.AStarPathfinder;
import com.optimizer.backend.graph.DijkstraPathfinder;
import com.optimizer.backend.graph.PathfindingAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final CityRepository cityRepository;
    private final OptimizationService optimizationService;

    @Transactional
    public ShipmentResponseDTO createShipment(ShipmentRequestDTO request) {
        if (request.getWeight() <= 0) {
            throw new BadRequestException("Shipment weight must be greater than zero");
        }

        City sourceCity = cityRepository.findById(request.getSourceCityId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Source city not found: " + request.getSourceCityId()));
        City destinationCity = cityRepository.findById(request.getDestinationCityId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Destination city not found: " + request.getDestinationCityId()));

        Shipment shipment = Shipment.builder()
                .sourceCity(sourceCity)
                .destinationCity(destinationCity)
                .weight(request.getWeight())
                .description(request.getDescription())
                .build();

        Shipment savedShipment = shipmentRepository.save(shipment);
        return toResponse(savedShipment);
    }

    @Transactional(readOnly = true)
    public ShipmentResponseDTO getShipment(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));
        return toResponse(shipment);
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponseDTO> getAllShipments() {
        return shipmentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OptimizationResponseDTO optimizeShipment(Long shipmentId, OptimizationType optimizationType,
                                                     PathfindingAlgorithm algorithm) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));

        return optimizationService.optimize(shipment, optimizationType, algorithm);
    }

    /**
     * Compare A* and Dijkstra for a given shipment.
     */
    @Transactional
    public AlgorithmComparator.ComparisonResult compareAlgorithms(Long shipmentId,
                                                                   OptimizationType optimizationType) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));

        return optimizationService.compareAlgorithms(shipment, optimizationType);
    }

    /**
     * Parse algorithm string to PathfindingAlgorithm instance.
     *
     * @param algorithmName "ASTAR" (default) or "DIJKSTRA"
     * @return the corresponding algorithm
     */
    public static PathfindingAlgorithm parseAlgorithm(String algorithmName) {
        if (algorithmName == null || algorithmName.isBlank() || "ASTAR".equalsIgnoreCase(algorithmName)) {
            return new AStarPathfinder();
        }
        if ("DIJKSTRA".equalsIgnoreCase(algorithmName)) {
            return new DijkstraPathfinder();
        }
        throw new BadRequestException("Unknown algorithm: " + algorithmName + ". Use ASTAR or DIJKSTRA.");
    }

    private ShipmentResponseDTO toResponse(Shipment shipment) {
        return ShipmentResponseDTO.builder()
                .id(shipment.getId())
                .sourceCityId(shipment.getSourceCity().getId())
                .sourceCityName(shipment.getSourceCity().getName())
                .destinationCityId(shipment.getDestinationCity().getId())
                .destinationCityName(shipment.getDestinationCity().getName())
                .weight(shipment.getWeight())
                .description(shipment.getDescription())
                .build();
    }
}
