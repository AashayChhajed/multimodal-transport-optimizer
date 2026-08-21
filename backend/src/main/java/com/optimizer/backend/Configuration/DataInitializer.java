package com.optimizer.backend.Configuration;

import com.optimizer.backend.Entity.City;
import com.optimizer.backend.Entity.Route;
import com.optimizer.backend.Entity.TransportMode;
import com.optimizer.backend.Entity.TransportModeType;
import com.optimizer.backend.Repository.CityRepository;
import com.optimizer.backend.Repository.RouteRepository;
import com.optimizer.backend.Repository.TransportModeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

        private final CityRepository cityRepository;
        private final TransportModeRepository transportModeRepository;
        private final RouteRepository routeRepository;
        private final JdbcTemplate jdbcTemplate;

        @Value("${app.seed.enabled:false}")
        private boolean seedEnabled;

        @Bean
        @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
        CommandLineRunner seedData() {
                return args -> {
                        long cityCount = cityRepository.count();
                        if (!seedEnabled && cityCount > 0) {
                                // DB already has data and seeding not explicitly enabled - skip seeding
                                return;
                        }

                        List<CitySeed> citySeeds = List.of(
                                        new CitySeed("Mumbai", 19.0760, 72.8777),
                                        new CitySeed("Delhi", 28.6139, 77.2090),
                                        new CitySeed("Bengaluru", 12.9716, 77.5946),
                                        new CitySeed("Chennai", 13.0827, 80.2707),
                                        new CitySeed("Kolkata", 22.5726, 88.3639),
                                        new CitySeed("Hyderabad", 17.3850, 78.4867),
                                        new CitySeed("Pune", 18.5204, 73.8567),
                                        new CitySeed("Ahmedabad", 23.0225, 72.5714),
                                        new CitySeed("Jaipur", 26.9124, 75.7873),
                                        new CitySeed("Lucknow", 26.8467, 80.9462),
                                        new CitySeed("Surat", 21.1702, 72.8311),
                                        new CitySeed("Bhopal", 23.2599, 77.4126),
                                        new CitySeed("Indore", 22.7196, 75.8577),
                                        new CitySeed("Nagpur", 21.1458, 79.0882),
                                        new CitySeed("Patna", 25.5941, 85.1376),
                                        new CitySeed("Kochi", 9.9312, 76.2673),
                                        new CitySeed("Visakhapatnam", 17.6868, 83.2185),
                                        new CitySeed("Bhubaneswar", 20.2961, 85.8245),
                                        new CitySeed("Coimbatore", 11.0168, 76.9558),
                                        new CitySeed("Guwahati", 26.1445, 91.7362));

                        for (CitySeed citySeed : citySeeds) {
                                cityRepository.findByName(citySeed.name())
                                                .orElseGet(() -> cityRepository.save(City.builder()
                                                                .name(citySeed.name())
                                                                .latitude(citySeed.latitude())
                                                                .longitude(citySeed.longitude())
                                                                .build()));
                        }

                        jdbcTemplate.update(
                                        "DELETE FROM route WHERE transport_mode_id IN (SELECT id FROM transport_mode WHERE name = 'SEA')");
                        jdbcTemplate.update("DELETE FROM transport_mode WHERE name = 'SEA'");

                        for (ModeSeed modeSeed : List.of(
                                        new ModeSeed(TransportModeType.ROAD, 1.2, 60, 0.062),
                                        new ModeSeed(TransportModeType.RAIL, 0.8, 90, 0.022),
                                        new ModeSeed(TransportModeType.AIR, 3.0, 700, 0.602))) {
                                transportModeRepository.findByName(modeSeed.type())
                                                .orElseGet(() -> transportModeRepository.save(TransportMode.builder()
                                                                .name(modeSeed.type())
                                                                .costPerKm(modeSeed.costPerKm())
                                                                .speed(modeSeed.speed())
                                                                .carbonPerTonKm(modeSeed.carbonPerTonKm())
                                                                .build()));
                        }

                        List<City> cities = cityRepository.findAll();
                        List<TransportMode> modes = transportModeRepository.findAll();

                        Map<Long, City> cityById = new HashMap<>();
                        for (City city : cities) {
                                cityById.put(city.getId(), city);
                        }

                        Set<String> existingRoutes = new HashSet<>();
                        routeRepository.findAll().forEach(route -> existingRoutes.add(
                                        route.getSourceCity().getId() + "-" + route.getDestinationCity().getId() + "-"
                                                        + route.getTransportMode().getId()));

                        List<Route> newRoutes = new ArrayList<>();
                        for (City source : cities) {
                                for (City destination : cities) {
                                        if (source.getId().equals(destination.getId())) {
                                                continue;
                                        }

                                        double distance = haversine(source.getLatitude(), source.getLongitude(),
                                                        destination.getLatitude(), destination.getLongitude());

                                        for (TransportMode mode : modes) {
                                                String key = source.getId() + "-" + destination.getId() + "-"
                                                                + mode.getId();
                                                if (!existingRoutes.contains(key)) {
                                                        newRoutes.add(Route.builder()
                                                                        .sourceCity(cityById.get(source.getId()))
                                                                        .destinationCity(cityById
                                                                                        .get(destination.getId()))
                                                                        .distance(Math.round(distance * 100.0) / 100.0)
                                                                        .transportMode(mode)
                                                                        .build());
                                                }
                                        }
                                }
                        }

                        if (!newRoutes.isEmpty()) {
                                routeRepository.saveAll(newRoutes);
                        }
                };
        }

        private double haversine(double lat1, double lon1, double lat2, double lon2) {
                final double earthRadiusKm = 6371.0;

                double dLat = Math.toRadians(lat2 - lat1);
                double dLon = Math.toRadians(lon2 - lon1);
                double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                                                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
                double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

                return earthRadiusKm * c;
        }

        private record CitySeed(String name, double latitude, double longitude) {
        }

        private record ModeSeed(TransportModeType type, double costPerKm, double speed, double carbonPerTonKm) {
        }
}
