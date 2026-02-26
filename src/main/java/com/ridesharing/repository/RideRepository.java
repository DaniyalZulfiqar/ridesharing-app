package com.ridesharing.repository;

import com.ridesharing.entity.Ride;
import com.ridesharing.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RideRepository extends JpaRepository<Ride, UUID> {

    List<Ride> findAllByStatus(RideStatus status);

    List<Ride> findAllByRiderId(UUID riderId);

    List<Ride> findAllByDriverId(UUID driverId);

    boolean existsByRiderIdAndStatusIn(UUID riderId, List<RideStatus> statuses);

    boolean existsByDriverIdAndStatusIn(UUID driverId, List<RideStatus> statuses);
}
