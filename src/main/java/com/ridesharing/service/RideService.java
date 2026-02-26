package com.ridesharing.service;

import com.ridesharing.dto.request.CancelRideRequest;
import com.ridesharing.dto.request.DriverActionRequest;
import com.ridesharing.dto.request.RequestRideRequest;
import com.ridesharing.dto.response.RideResponse;
import com.ridesharing.dto.response.RiderSummaryResponse;
import com.ridesharing.dto.response.DriverSummaryResponse;
import com.ridesharing.entity.Driver;
import com.ridesharing.entity.Ride;
import com.ridesharing.entity.Rider;
import com.ridesharing.enums.RideStatus;
import com.ridesharing.exception.ConflictException;
import com.ridesharing.exception.ForbiddenException;
import com.ridesharing.exception.NoDriverAvailableException;
import com.ridesharing.exception.ResourceNotFoundException;
import com.ridesharing.repository.DriverRepository;
import com.ridesharing.repository.RideRepository;
import com.ridesharing.repository.RiderRepository;
import com.ridesharing.util.FareCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RideService {

    private final RideRepository rideRepository;
    private final RiderRepository riderRepository;
    private final DriverRepository driverRepository;
    private final FareCalculator fareCalculator;

    public RideService(RideRepository rideRepository,
                       RiderRepository riderRepository,
                       DriverRepository driverRepository,
                       FareCalculator fareCalculator) {
        this.rideRepository = rideRepository;
        this.riderRepository = riderRepository;
        this.driverRepository = driverRepository;
        this.fareCalculator = fareCalculator;
    }

    // ── FR-RI01 to FR-RI05 ───────────────────────────────────
    public RideResponse requestRide(RequestRideRequest request) {
        // TODO: Phase 3 — implement
        // 1. findRider → ResourceNotFoundException
        // 2. Check rider has no active ride → ConflictException (A5)
        // 3. driverRepository.findAllByAvailableTrue() → NoDriverAvailableException if empty (FR-RI02/03)
        // 4. fareCalculator.calculate(distanceKm)
        // 5. Build Ride entity (status=REQUESTED set in @PrePersist), save
        // 6. Return RideResponse
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ── FR-RI11 ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public RideResponse getById(UUID rideId) {
        // TODO: Phase 3 — implement
        // 1. findById → ResourceNotFoundException if absent
        // 2. Map to RideResponse
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ── FR-RI06 ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<RideResponse> listByStatus(RideStatus status) {
        // TODO: Phase 3 — implement
        // 1. If status param is present: rideRepository.findAllByStatus(status)
        // 2. If absent: rideRepository.findAll()
        // 3. Map each Ride to RideResponse
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ── FR-RI07 ──────────────────────────────────────────────
    public RideResponse acceptRide(UUID rideId, DriverActionRequest request) {
        // TODO: Phase 3 — implement
        // 1. findRide → ResourceNotFoundException
        // 2. Validate ride.status == REQUESTED → ConflictException
        // 3. findDriver → ResourceNotFoundException
        // 4. Driver must be available + no active ride → ConflictException (A4)
        // 5. ride.setDriver(driver), ride.setStatus(ACCEPTED), ride.setAcceptedAt(now)
        // 6. driver.setAvailable(false)
        // 7. Save both, return RideResponse
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ── FR-RI08 ──────────────────────────────────────────────
    public RideResponse rejectRide(UUID rideId, DriverActionRequest request) {
        // TODO: Phase 3 — implement
        // 1. findRide → ResourceNotFoundException
        // 2. Validate ride.status == REQUESTED → ConflictException
        // 3. findDriver (must exist) → ResourceNotFoundException
        // 4. No state change — ride stays REQUESTED
        // 5. Return RideResponse with message
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ── FR-RI09 ──────────────────────────────────────────────
    public RideResponse startRide(UUID rideId, DriverActionRequest request) {
        // TODO: Phase 3 — implement
        // 1. findRide → ResourceNotFoundException
        // 2. Validate ride.status == ACCEPTED → ConflictException
        // 3. Validate driver is the assigned driver → ForbiddenException
        // 4. ride.setStatus(IN_PROGRESS), ride.setStartedAt(now)
        // 5. Save and return RideResponse
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ── FR-RI10 ──────────────────────────────────────────────
    public RideResponse completeRide(UUID rideId, DriverActionRequest request) {
        // TODO: Phase 3 — implement
        // 1. findRide → ResourceNotFoundException
        // 2. Validate ride.status == IN_PROGRESS → ConflictException
        // 3. Validate driver is the assigned driver → ForbiddenException
        // 4. ride.setStatus(COMPLETED), ride.setCompletedAt(now)
        // 5. driver.setAvailable(true)
        // 6. Save both, return RideResponse
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ── FR-C01 / FR-C02 / FR-C03 / FR-C04 ───────────────────
    public RideResponse cancelRide(UUID rideId, CancelRideRequest request) {
        // TODO: Phase 3 — implement
        // 1. findRide → ResourceNotFoundException
        // 2. Validate status is REQUESTED or ACCEPTED → ConflictException if not
        // 3. Validate actor authorization:
        //    - RIDER:  actorId must match ride.rider.id and status != IN_PROGRESS → ForbiddenException
        //    - DRIVER: actorId must match ride.driver.id and status == ACCEPTED → ForbiddenException
        // 4. ride.setStatus(CANCELLED), ride.setCancellationReason, ride.setCancelledAt(now)
        // 5. If cancelled by DRIVER: driver.setAvailable(true)  (FR-C04)
        // 6. Save and return RideResponse
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ── mapping helper ───────────────────────────────────────
    static RideResponse toResponse(Ride ride) {
        RiderSummaryResponse riderSummary = RiderSummaryResponse.builder()
                .id(ride.getRider().getId())
                .name(ride.getRider().getName())
                .build();

        DriverSummaryResponse driverSummary = null;
        if (ride.getDriver() != null) {
            driverSummary = DriverSummaryResponse.builder()
                    .id(ride.getDriver().getId())
                    .name(ride.getDriver().getName())
                    .vehicleMake(ride.getDriver().getVehicleMake())
                    .vehicleModel(ride.getDriver().getVehicleModel())
                    .licensePlate(ride.getDriver().getLicensePlate())
                    .build();
        }

        return RideResponse.builder()
                .id(ride.getId())
                .rider(riderSummary)
                .driver(driverSummary)
                .pickupLocation(ride.getPickupLocation())
                .dropoffLocation(ride.getDropoffLocation())
                .distanceKm(ride.getDistanceKm())
                .fareAmount(ride.getFareAmount())
                .status(ride.getStatus())
                .cancellationReason(ride.getCancellationReason())
                .requestedAt(ride.getRequestedAt())
                .acceptedAt(ride.getAcceptedAt())
                .startedAt(ride.getStartedAt())
                .completedAt(ride.getCompletedAt())
                .cancelledAt(ride.getCancelledAt())
                .build();
    }
}
