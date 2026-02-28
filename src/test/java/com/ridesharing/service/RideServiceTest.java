package com.ridesharing.service;

import com.ridesharing.dto.request.CancelRideRequest;
import com.ridesharing.dto.request.DriverActionRequest;
import com.ridesharing.dto.request.RequestRideRequest;
import com.ridesharing.dto.response.RideResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock RideRepository rideRepository;
    @Mock RiderRepository riderRepository;
    @Mock DriverRepository driverRepository;
    @Mock FareCalculator fareCalculator;

    @InjectMocks RideService rideService;

    private UUID riderId;
    private UUID driverId;
    private UUID rideId;
    private Rider rider;
    private Driver driver;
    private Ride ride;

    @BeforeEach
    void setUp() {
        riderId  = UUID.randomUUID();
        driverId = UUID.randomUUID();
        rideId   = UUID.randomUUID();

        rider = new Rider();
        rider.setId(riderId);
        rider.setName("Alice");

        driver = new Driver();
        driver.setId(driverId);
        driver.setName("Bob");
        driver.setVehicleMake("Toyota");
        driver.setVehicleModel("Camry");
        driver.setLicensePlate("ABC-123");
        driver.setAvailable(true);

        ride = new Ride();
        ride.setId(rideId);
        ride.setRider(rider);
        ride.setPickupLocation("123 Main St");
        ride.setDropoffLocation("456 Oak Ave");
        ride.setDistanceKm(5.0);
        ride.setFareAmount(new BigDecimal("9.50"));
        ride.setStatus(RideStatus.REQUESTED);
        ride.setRequestedAt(LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────────
    // requestRide
    // ─────────────────────────────────────────────────────────

    @Test
    void requestRide_success() {
        RequestRideRequest req = new RequestRideRequest();
        req.setRiderId(riderId);
        req.setPickupLocation("123 Main St");
        req.setDropoffLocation("456 Oak Ave");
        req.setDistanceKm(5.0);

        when(riderRepository.findById(riderId)).thenReturn(Optional.of(rider));
        when(rideRepository.existsByRiderIdAndStatusIn(eq(riderId), anyList())).thenReturn(false);
        when(driverRepository.findAllByAvailableTrue()).thenReturn(List.of(driver));
        when(fareCalculator.calculate(5.0)).thenReturn(new BigDecimal("9.50"));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> {
            Ride r = inv.getArgument(0);
            r.setId(rideId);
            r.setStatus(RideStatus.REQUESTED);
            r.setRequestedAt(LocalDateTime.now());
            return r;
        });

        RideResponse response = rideService.requestRide(req);

        assertThat(response.getId()).isEqualTo(rideId);
        assertThat(response.getFareAmount()).isEqualByComparingTo("9.50");
        assertThat(response.getStatus()).isEqualTo(RideStatus.REQUESTED);
        assertThat(response.getDriver()).isNull();
    }

    @Test
    void requestRide_riderNotFound_throws404() {
        RequestRideRequest req = new RequestRideRequest();
        req.setRiderId(riderId);

        when(riderRepository.findById(riderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.requestRide(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requestRide_riderHasActiveRide_throws409() {
        RequestRideRequest req = new RequestRideRequest();
        req.setRiderId(riderId);

        when(riderRepository.findById(riderId)).thenReturn(Optional.of(rider));
        when(rideRepository.existsByRiderIdAndStatusIn(eq(riderId), anyList())).thenReturn(true);

        assertThatThrownBy(() -> rideService.requestRide(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active ride");
    }

    @Test
    void requestRide_noDriversAvailable_throws503() {
        RequestRideRequest req = new RequestRideRequest();
        req.setRiderId(riderId);
        req.setDistanceKm(5.0);

        when(riderRepository.findById(riderId)).thenReturn(Optional.of(rider));
        when(rideRepository.existsByRiderIdAndStatusIn(eq(riderId), anyList())).thenReturn(false);
        when(driverRepository.findAllByAvailableTrue()).thenReturn(List.of());

        assertThatThrownBy(() -> rideService.requestRide(req))
                .isInstanceOf(NoDriverAvailableException.class);
    }

    // ─────────────────────────────────────────────────────────
    // getById
    // ─────────────────────────────────────────────────────────

    @Test
    void getById_success() {
        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));

        RideResponse response = rideService.getById(rideId);

        assertThat(response.getId()).isEqualTo(rideId);
        assertThat(response.getStatus()).isEqualTo(RideStatus.REQUESTED);
    }

    @Test
    void getById_notFound_throws404() {
        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.getById(rideId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────────────────────────────────────
    // listByStatus
    // ─────────────────────────────────────────────────────────

    @Test
    void listByStatus_withFilter_returnsFilteredRides() {
        when(rideRepository.findAllByStatusWithActors(RideStatus.REQUESTED)).thenReturn(List.of(ride));

        List<RideResponse> responses = rideService.listByStatus(RideStatus.REQUESTED);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo(RideStatus.REQUESTED);
        verify(rideRepository, never()).findAllWithActors();
    }

    @Test
    void listByStatus_noFilter_returnsAllRides() {
        when(rideRepository.findAllWithActors()).thenReturn(List.of(ride));

        List<RideResponse> responses = rideService.listByStatus(null);

        assertThat(responses).hasSize(1);
        verify(rideRepository, never()).findAllByStatusWithActors(any());
    }

    // ─────────────────────────────────────────────────────────
    // acceptRide
    // ─────────────────────────────────────────────────────────

    @Test
    void acceptRide_success() {
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(rideRepository.existsByDriverIdAndStatusIn(eq(driverId), anyList())).thenReturn(false);
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.acceptRide(rideId, req);

        assertThat(response.getStatus()).isEqualTo(RideStatus.ACCEPTED);
        assertThat(response.getDriver()).isNotNull();
        assertThat(response.getDriver().getId()).isEqualTo(driverId);
        assertThat(driver.isAvailable()).isFalse();
    }

    @Test
    void acceptRide_rideNotFound_throws404() {
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.acceptRide(rideId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void acceptRide_rideNotRequested_throws409() {
        ride.setStatus(RideStatus.ACCEPTED);
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.acceptRide(rideId, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("REQUESTED");
    }

    @Test
    void acceptRide_driverNotFound_throws404() {
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));
        when(driverRepository.findById(driverId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.acceptRide(rideId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void acceptRide_driverNotAvailable_throws409() {
        driver.setAvailable(false);
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> rideService.acceptRide(rideId, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void acceptRide_driverHasActiveRide_throws409() {
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(rideRepository.existsByDriverIdAndStatusIn(eq(driverId), anyList())).thenReturn(true);

        assertThatThrownBy(() -> rideService.acceptRide(rideId, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active ride");
    }

    // ─────────────────────────────────────────────────────────
    // rejectRide
    // ─────────────────────────────────────────────────────────

    @Test
    void rejectRide_success_rideRemainsRequested() {
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));
        when(driverRepository.existsById(driverId)).thenReturn(true);

        RideResponse response = rideService.rejectRide(rideId, req);

        assertThat(response.getStatus()).isEqualTo(RideStatus.REQUESTED);
        verify(rideRepository, never()).save(any());
    }

    @Test
    void rejectRide_rideNotFound_throws404() {
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.rejectRide(rideId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectRide_rideNotRequested_throws409() {
        ride.setStatus(RideStatus.IN_PROGRESS);
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.rejectRide(rideId, req))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectRide_driverNotFound_throws404() {
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));
        when(driverRepository.existsById(driverId)).thenReturn(false);

        assertThatThrownBy(() -> rideService.rejectRide(rideId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────────────────────────────────────
    // startRide
    // ─────────────────────────────────────────────────────────

    @Test
    void startRide_success() {
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setDriver(driver);
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.startRide(rideId, req);

        assertThat(response.getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
    }

    @Test
    void startRide_rideNotFound_throws404() {
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.startRide(rideId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void startRide_rideNotAccepted_throws409() {
        // ride is still REQUESTED
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.startRide(rideId, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ACCEPTED");
    }

    @Test
    void startRide_wrongDriver_throws403() {
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setDriver(driver);
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(UUID.randomUUID()); // different driver

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.startRide(rideId, req))
                .isInstanceOf(ForbiddenException.class);
    }

    // ─────────────────────────────────────────────────────────
    // completeRide
    // ─────────────────────────────────────────────────────────

    @Test
    void completeRide_success() {
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setDriver(driver);
        driver.setAvailable(false);
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.completeRide(rideId, req);

        assertThat(response.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(driver.isAvailable()).isTrue();
        verify(driverRepository).save(driver);
    }

    @Test
    void completeRide_rideNotFound_throws404() {
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.completeRide(rideId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void completeRide_rideNotInProgress_throws409() {
        // ride is ACCEPTED, not IN_PROGRESS
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setDriver(driver);
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(driverId);

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.completeRide(rideId, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("IN_PROGRESS");
    }

    @Test
    void completeRide_wrongDriver_throws403() {
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setDriver(driver);
        DriverActionRequest req = new DriverActionRequest();
        req.setDriverId(UUID.randomUUID());

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.completeRide(rideId, req))
                .isInstanceOf(ForbiddenException.class);
    }

    // ─────────────────────────────────────────────────────────
    // cancelRide
    // ─────────────────────────────────────────────────────────

    @Test
    void cancelRide_riderCancelsRequested_success() {
        CancelRideRequest req = new CancelRideRequest();
        req.setCancelledBy("RIDER");
        req.setActorId(riderId);
        req.setReason("Changed my mind");

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.cancelRide(rideId, req);

        assertThat(response.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(response.getCancellationReason()).isEqualTo("Changed my mind");
    }

    @Test
    void cancelRide_riderCancelsAccepted_success() {
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setDriver(driver);

        CancelRideRequest req = new CancelRideRequest();
        req.setCancelledBy("RIDER");
        req.setActorId(riderId);
        req.setReason("Emergency");

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.cancelRide(rideId, req);

        assertThat(response.getStatus()).isEqualTo(RideStatus.CANCELLED);
    }

    @Test
    void cancelRide_riderUnauthorized_throws403() {
        CancelRideRequest req = new CancelRideRequest();
        req.setCancelledBy("RIDER");
        req.setActorId(UUID.randomUUID()); // not this ride's rider
        req.setReason("Test");

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.cancelRide(rideId, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancelRide_driverCancelsAccepted_success_driverFreed() {
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setDriver(driver);
        driver.setAvailable(false);

        CancelRideRequest req = new CancelRideRequest();
        req.setCancelledBy("DRIVER");
        req.setActorId(driverId);
        req.setReason("Car trouble");

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        RideResponse response = rideService.cancelRide(rideId, req);

        assertThat(response.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(driver.isAvailable()).isTrue();
        verify(driverRepository).save(driver);
    }

    @Test
    void cancelRide_driverCancelsRequested_throws403() {
        // Driver cannot cancel a ride that is only REQUESTED (not yet assigned)
        CancelRideRequest req = new CancelRideRequest();
        req.setCancelledBy("DRIVER");
        req.setActorId(driverId);
        req.setReason("Test");

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.cancelRide(rideId, req))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("accepted");
    }

    @Test
    void cancelRide_driverUnauthorized_throws403() {
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setDriver(driver);

        CancelRideRequest req = new CancelRideRequest();
        req.setCancelledBy("DRIVER");
        req.setActorId(UUID.randomUUID()); // different driver
        req.setReason("Test");

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.cancelRide(rideId, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancelRide_completedRide_throws409() {
        ride.setStatus(RideStatus.COMPLETED);

        CancelRideRequest req = new CancelRideRequest();
        req.setCancelledBy("RIDER");
        req.setActorId(riderId);
        req.setReason("Test");

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.cancelRide(rideId, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void cancelRide_inProgressRide_throws409() {
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setDriver(driver);

        CancelRideRequest req = new CancelRideRequest();
        req.setCancelledBy("RIDER");
        req.setActorId(riderId);
        req.setReason("Test");

        when(rideRepository.findByIdWithActors(rideId)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.cancelRide(rideId, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("IN_PROGRESS");
    }
}
