package com.ridesharing.service;

import com.ridesharing.dto.request.RegisterDriverRequest;
import com.ridesharing.dto.request.UpdateAvailabilityRequest;
import com.ridesharing.dto.response.DriverResponse;
import com.ridesharing.dto.response.DriverSummaryResponse;
import com.ridesharing.dto.response.RideResponse;
import com.ridesharing.entity.Driver;
import com.ridesharing.enums.RideStatus;
import com.ridesharing.exception.ConflictException;
import com.ridesharing.exception.ResourceNotFoundException;
import com.ridesharing.repository.DriverRepository;
import com.ridesharing.repository.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DriverService {

    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;

    public DriverService(DriverRepository driverRepository, RideRepository rideRepository) {
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
    }

    // ── FR-D01 ───────────────────────────────────────────────

    public DriverResponse register(RegisterDriverRequest request) {
        if (driverRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }
        if (driverRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new ConflictException("License plate already registered: " + request.getLicensePlate());
        }

        Driver driver = Driver.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .vehicleMake(request.getVehicleMake())
                .vehicleModel(request.getVehicleModel())
                .licensePlate(request.getLicensePlate())
                .available(false)
                .build();

        return toResponse(driverRepository.save(driver));
    }

    // ── FR-D02 ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DriverResponse getById(UUID driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + driverId));
        return toResponse(driver);
    }

    // ── FR-D03 ───────────────────────────────────────────────

    public DriverResponse updateAvailability(UUID driverId, UpdateAvailabilityRequest request) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + driverId));

        boolean hasActiveRide = rideRepository.existsByDriverIdAndStatusIn(
                driverId, List.of(RideStatus.ACCEPTED, RideStatus.IN_PROGRESS));

        if (hasActiveRide) {
            throw new ConflictException("Cannot change availability while an active ride is in progress.");
        }

        driver.setAvailable(request.getAvailable());
        return toResponse(driverRepository.save(driver));
    }

    // ── FR-RI02 (used by RideService) ────────────────────────

    @Transactional(readOnly = true)
    public List<DriverSummaryResponse> getAvailableDrivers() {
        return driverRepository.findAllByAvailableTrue()
                .stream()
                .map(DriverService::toSummary)
                .toList();
    }

    // ── FR-D04 / FR-RI13 ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RideResponse> getRideHistory(UUID driverId) {
        if (!driverRepository.existsById(driverId)) {
            throw new ResourceNotFoundException("Driver not found: " + driverId);
        }
        return rideRepository.findAllByDriverIdWithActors(driverId)
                .stream()
                .map(RideService::toResponse)
                .toList();
    }

    // ── mapping helpers ──────────────────────────────────────
    static DriverResponse toResponse(Driver driver) {
        return DriverResponse.builder()
                .id(driver.getId())
                .name(driver.getName())
                .email(driver.getEmail())
                .phone(driver.getPhone())
                .vehicleMake(driver.getVehicleMake())
                .vehicleModel(driver.getVehicleModel())
                .licensePlate(driver.getLicensePlate())
                .available(driver.isAvailable())
                .createdAt(driver.getCreatedAt())
                .build();
    }

    static DriverSummaryResponse toSummary(Driver driver) {
        return DriverSummaryResponse.builder()
                .id(driver.getId())
                .name(driver.getName())
                .vehicleMake(driver.getVehicleMake())
                .vehicleModel(driver.getVehicleModel())
                .licensePlate(driver.getLicensePlate())
                .build();
    }
}
