package com.optimizer.backend.Repository;

import com.optimizer.backend.Entity.Route;
import com.optimizer.backend.Entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findBySourceCity(City sourceCity);

    List<Route> findBySourceCityAndDestinationCity(City sourceCity, City destinationCity);
}
