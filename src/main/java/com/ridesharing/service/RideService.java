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

import java.time.LocalDateTime;
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
        Rider rider = riderRepository.findById(request.getRiderId())
                .orElseThrow(() -> new ResourceNotFoundException("Rider not found: " + request.getRiderId()));

        boolean hasActiveRide = rideRepository.existsByRiderIdAndStatusIn(
                rider.getId(),
                List.of(RideStatus.REQUESTED, RideStatus.ACCEPTED, RideStatus.IN_PROGRESS));
        if (hasActiveRide) {
            throw new ConflictException("Rider already has an active ride.");
        }

        List<Driver> availableDrivers = driverRepository.findAllByAvailableTrue();
        if (availableDrivers.isEmpty()) {
            throw new NoDriverAvailableException("No drivers are available at this time.");
        }

        Ride ride = Ride.builder()
                .rider(rider)
                .pickupLocation(request.getPickupLocation())
                .dropoffLocation(request.getDropoffLocation())
                .distanceKm(request.getDistanceKm())
                .fareAmount(fareCalculator.calculate(request.getDistanceKm()))
                .build();

        return toResponse(rideRepository.save(ride));
    }

    // ── FR-RI11 ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RideResponse getById(UUID rideId) {
        Ride ride = rideRepository.findByIdWithActors(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));
        return toResponse(ride);
    }

    // ── FR-RI06 ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RideResponse> listByStatus(RideStatus status) {
        List<Ride> rides = (status != null)
                ? rideRepository.findAllByStatusWithActors(status)
                : rideRepository.findAllWithActors();
        return rides.stream().map(RideService::toResponse).toList();
    }

    // ── FR-RI07 ──────────────────────────────────────────────

    public RideResponse acceptRide(UUID rideId, DriverActionRequest request) {
        Ride ride = rideRepository.findByIdWithActors(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new ConflictException(
                    "Ride is not in REQUESTED status (current: " + ride.getStatus() + ").");
        }

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + request.getDriverId()));

        if (!driver.isAvailable()) {
            throw new ConflictException("Driver is not available.");
        }

        boolean driverHasActiveRide = rideRepository.existsByDriverIdAndStatusIn(
                driver.getId(), List.of(RideStatus.ACCEPTED, RideStatus.IN_PROGRESS));
        if (driverHasActiveRide) {
            throw new ConflictException("Driver already has an active ride.");
        }

        ride.setDriver(driver);
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(LocalDateTime.now());
        driver.setAvailable(false);

        driverRepository.save(driver);
        return toResponse(rideRepository.save(ride));
    }

    // ── FR-RI08 ──────────────────────────────────────────────

    public RideResponse rejectRide(UUID rideId, DriverActionRequest request) {
        Ride ride = rideRepository.findByIdWithActors(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new ConflictException(
                    "Ride is not in REQUESTED status (current: " + ride.getStatus() + ").");
        }

        if (!driverRepository.existsById(request.getDriverId())) {
            throw new ResourceNotFoundException("Driver not found: " + request.getDriverId());
        }

        // No state change — ride remains REQUESTED for another driver to accept.
        return toResponse(ride);
    }

    // ── FR-RI09 ──────────────────────────────────────────────

    public RideResponse startRide(UUID rideId, DriverActionRequest request) {
        Ride ride = rideRepository.findByIdWithActors(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new ConflictException(
                    "Ride is not in ACCEPTED status (current: " + ride.getStatus() + ").");
        }

        if (!ride.getDriver().getId().equals(request.getDriverId())) {
            throw new ForbiddenException("Only the assigned driver can start this ride.");
        }

        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(LocalDateTime.now());
        return toResponse(rideRepository.save(ride));
    }

    // ── FR-RI10 ──────────────────────────────────────────────

    public RideResponse completeRide(UUID rideId, DriverActionRequest request) {
        Ride ride = rideRepository.findByIdWithActors(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new ConflictException(
                    "Ride is not in IN_PROGRESS status (current: " + ride.getStatus() + ").");
        }

        if (!ride.getDriver().getId().equals(request.getDriverId())) {
            throw new ForbiddenException("Only the assigned driver can complete this ride.");
        }

        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());

        Driver driver = ride.getDriver();
        driver.setAvailable(true);
        driverRepository.save(driver);

        return toResponse(rideRepository.save(ride));
    }

    // ── FR-C01 / FR-C02 / FR-C03 / FR-C04 ───────────────────

    public RideResponse cancelRide(UUID rideId, CancelRideRequest request) {
        Ride ride = rideRepository.findByIdWithActors(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found: " + rideId));

        if (ride.getStatus() != RideStatus.REQUESTED && ride.getStatus() != RideStatus.ACCEPTED) {
            throw new ConflictException(
                    "Ride cannot be cancelled in its current status: " + ride.getStatus() + ".");
        }

        if ("RIDER".equals(request.getCancelledBy())) {
            if (!ride.getRider().getId().equals(request.getActorId())) {
                throw new ForbiddenException("Rider is not authorized to cancel this ride.");
            }
        } else {
            // DRIVER: can only cancel a ride they have accepted
            if (ride.getStatus() != RideStatus.ACCEPTED) {
                throw new ForbiddenException("Driver can only cancel a ride that has been accepted.");
            }
            if (ride.getDriver() == null || !ride.getDriver().getId().equals(request.getActorId())) {
                throw new ForbiddenException("Driver is not authorized to cancel this ride.");
            }
            ride.getDriver().setAvailable(true);
            driverRepository.save(ride.getDriver());
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancellationReason(request.getReason());
        ride.setCancelledAt(LocalDateTime.now());

        return toResponse(rideRepository.save(ride));
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
