package com.ridesharing.service;

import com.ridesharing.dto.request.RegisterDriverRequest;
import com.ridesharing.dto.request.UpdateAvailabilityRequest;
import com.ridesharing.dto.response.DriverResponse;
import com.ridesharing.dto.response.DriverSummaryResponse;
import com.ridesharing.dto.response.RideResponse;
import com.ridesharing.entity.Driver;
import com.ridesharing.entity.Ride;
import com.ridesharing.entity.Rider;
import com.ridesharing.enums.RideStatus;
import com.ridesharing.exception.ConflictException;
import com.ridesharing.exception.ResourceNotFoundException;
import com.ridesharing.repository.DriverRepository;
import com.ridesharing.repository.RideRepository;
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
class DriverServiceTest {

    @Mock DriverRepository driverRepository;
    @Mock RideRepository rideRepository;

    @InjectMocks DriverService driverService;

    private UUID driverId;
    private Driver driver;

    @BeforeEach
    void setUp() {
        driverId = UUID.randomUUID();

        driver = new Driver();
        driver.setId(driverId);
        driver.setName("Bob");
        driver.setEmail("bob@example.com");
        driver.setPhone("555-0200");
        driver.setVehicleMake("Toyota");
        driver.setVehicleModel("Camry");
        driver.setLicensePlate("ABC-123");
        driver.setAvailable(false);
        driver.setCreatedAt(LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────────
    // register
    // ─────────────────────────────────────────────────────────

    @Test
    void register_success() {
        RegisterDriverRequest req = buildRegisterRequest();

        when(driverRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(driverRepository.existsByLicensePlate("ABC-123")).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> {
            Driver d = inv.getArgument(0);
            d.setId(driverId);
            d.setCreatedAt(LocalDateTime.now());
            return d;
        });

        DriverResponse response = driverService.register(req);

        assertThat(response.getId()).isEqualTo(driverId);
        assertThat(response.getName()).isEqualTo("Bob");
        assertThat(response.getEmail()).isEqualTo("bob@example.com");
        assertThat(response.getLicensePlate()).isEqualTo("ABC-123");
        assertThat(response.isAvailable()).isFalse();
    }

    @Test
    void register_duplicateEmail_throws409() {
        RegisterDriverRequest req = buildRegisterRequest();

        when(driverRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThatThrownBy(() -> driverService.register(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("bob@example.com");

        verify(driverRepository, never()).save(any());
    }

    @Test
    void register_duplicateLicensePlate_throws409() {
        RegisterDriverRequest req = buildRegisterRequest();

        when(driverRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(driverRepository.existsByLicensePlate("ABC-123")).thenReturn(true);

        assertThatThrownBy(() -> driverService.register(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ABC-123");

        verify(driverRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────
    // getById
    // ─────────────────────────────────────────────────────────

    @Test
    void getById_success() {
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));

        DriverResponse response = driverService.getById(driverId);

        assertThat(response.getId()).isEqualTo(driverId);
        assertThat(response.getVehicleMake()).isEqualTo("Toyota");
    }

    @Test
    void getById_notFound_throws404() {
        when(driverRepository.findById(driverId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.getById(driverId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────────────────────────────────────
    // updateAvailability
    // ─────────────────────────────────────────────────────────

    @Test
    void updateAvailability_goOnline_success() {
        UpdateAvailabilityRequest req = new UpdateAvailabilityRequest();
        req.setAvailable(true);

        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(rideRepository.existsByDriverIdAndStatusIn(eq(driverId), anyList())).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverResponse response = driverService.updateAvailability(driverId, req);

        assertThat(response.isAvailable()).isTrue();
    }

    @Test
    void updateAvailability_goOffline_success() {
        driver.setAvailable(true);
        UpdateAvailabilityRequest req = new UpdateAvailabilityRequest();
        req.setAvailable(false);

        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(rideRepository.existsByDriverIdAndStatusIn(eq(driverId), anyList())).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        DriverResponse response = driverService.updateAvailability(driverId, req);

        assertThat(response.isAvailable()).isFalse();
    }

    @Test
    void updateAvailability_activeRide_throws409() {
        UpdateAvailabilityRequest req = new UpdateAvailabilityRequest();
        req.setAvailable(false);

        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(rideRepository.existsByDriverIdAndStatusIn(eq(driverId), anyList())).thenReturn(true);

        assertThatThrownBy(() -> driverService.updateAvailability(driverId, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active ride");

        verify(driverRepository, never()).save(any());
    }

    @Test
    void updateAvailability_driverNotFound_throws404() {
        UpdateAvailabilityRequest req = new UpdateAvailabilityRequest();
        req.setAvailable(true);

        when(driverRepository.findById(driverId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.updateAvailability(driverId, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────────────────────────────────────
    // getAvailableDrivers
    // ─────────────────────────────────────────────────────────

    @Test
    void getAvailableDrivers_returnsOnlyAvailable() {
        driver.setAvailable(true);
        when(driverRepository.findAllByAvailableTrue()).thenReturn(List.of(driver));

        List<DriverSummaryResponse> result = driverService.getAvailableDrivers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Bob");
        assertThat(result.get(0).getLicensePlate()).isEqualTo("ABC-123");
    }

    @Test
    void getAvailableDrivers_noneAvailable_returnsEmpty() {
        when(driverRepository.findAllByAvailableTrue()).thenReturn(List.of());

        List<DriverSummaryResponse> result = driverService.getAvailableDrivers();

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────
    // getRideHistory
    // ─────────────────────────────────────────────────────────

    @Test
    void getRideHistory_success_returnsRides() {
        Rider rider = new Rider();
        rider.setId(UUID.randomUUID());
        rider.setName("Alice");

        Ride ride = new Ride();
        ride.setId(UUID.randomUUID());
        ride.setRider(rider);
        ride.setDriver(driver);
        ride.setPickupLocation("A");
        ride.setDropoffLocation("B");
        ride.setDistanceKm(5.0);
        ride.setFareAmount(new BigDecimal("9.50"));
        ride.setStatus(RideStatus.COMPLETED);
        ride.setRequestedAt(LocalDateTime.now());

        when(driverRepository.existsById(driverId)).thenReturn(true);
        when(rideRepository.findAllByDriverIdWithActors(driverId)).thenReturn(List.of(ride));

        List<RideResponse> history = driverService.getRideHistory(driverId);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(history.get(0).getDriver().getId()).isEqualTo(driverId);
    }

    @Test
    void getRideHistory_emptyHistory_returnsEmptyList() {
        when(driverRepository.existsById(driverId)).thenReturn(true);
        when(rideRepository.findAllByDriverIdWithActors(driverId)).thenReturn(List.of());

        List<RideResponse> history = driverService.getRideHistory(driverId);

        assertThat(history).isEmpty();
    }

    @Test
    void getRideHistory_driverNotFound_throws404() {
        when(driverRepository.existsById(driverId)).thenReturn(false);

        assertThatThrownBy(() -> driverService.getRideHistory(driverId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(rideRepository, never()).findAllByDriverIdWithActors(any());
    }

    // ─────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────

    private RegisterDriverRequest buildRegisterRequest() {
        RegisterDriverRequest req = new RegisterDriverRequest();
        req.setName("Bob");
        req.setEmail("bob@example.com");
        req.setPhone("555-0200");
        req.setVehicleMake("Toyota");
        req.setVehicleModel("Camry");
        req.setLicensePlate("ABC-123");
        return req;
    }
}
