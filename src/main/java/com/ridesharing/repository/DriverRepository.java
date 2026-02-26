package com.ridesharing.repository;

import com.ridesharing.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {

    boolean existsByEmail(String email);

    boolean existsByLicensePlate(String licensePlate);

    List<Driver> findAllByAvailableTrue();
}
