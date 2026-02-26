package com.ridesharing.service;

import com.ridesharing.dto.request.RegisterDriverRequest;
import com.ridesharing.dto.request.UpdateAvailabilityRequest;
import com.ridesharing.dto.response.DriverResponse;
import com.ridesharing.dto.response.DriverSummaryResponse;
import com.ridesharing.dto.response.RideResponse;
import com.ridesharing.entity.Driver;
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
        // TODO: Phase 3 — implement
        // 1. Check existsByEmail and existsByLicensePlate → ConflictException if duplicate
        // 2. Build Driver entity (available=false by default)
        // 3. Save and map to DriverResponse
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ── FR-D02 ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public DriverResponse getById(UUID driverId) {
        // TODO: Phase 3 — implement
        // 1. findById → ResourceNotFoundException if absent
        // 2. Map to DriverResponse
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ── FR-D03 ───────────────────────────────────────────────
    public DriverResponse updateAvailability(UUID driverId, UpdateAvailabilityRequest request) {
        // TODO: Phase 3 — implement
        // 1. findById → ResourceNotFoundException if absent
        // 2. If going offline: check no active ride → ConflictException if busy
        // 3. Set available flag, save, return updated DriverResponse
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ── FR-RI02 (used by RideService) ────────────────────────
    @Transactional(readOnly = true)
    public List<DriverSummaryResponse> getAvailableDrivers() {
        // TODO: Phase 3 — implement
        // 1. driverRepository.findAllByAvailableTrue()
        // 2. Map each Driver to DriverSummaryResponse
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ── FR-D04 / FR-RI13 ─────────────────────────────────────
    @Transactional(readOnly = true)
    public List<RideResponse> getRideHistory(UUID driverId) {
        // TODO: Phase 3 — implement
        // 1. Verify driver exists → ResourceNotFoundException if absent
        // 2. rideRepository.findAllByDriverId(driverId)
        // 3. Map each Ride to RideResponse
        throw new UnsupportedOperationException("Not yet implemented");
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
