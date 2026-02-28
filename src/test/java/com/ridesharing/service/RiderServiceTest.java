package com.ridesharing.service;

import com.ridesharing.dto.request.RegisterRiderRequest;
import com.ridesharing.dto.response.RideResponse;
import com.ridesharing.dto.response.RiderResponse;
import com.ridesharing.entity.Driver;
import com.ridesharing.entity.Ride;
import com.ridesharing.entity.Rider;
import com.ridesharing.enums.RideStatus;
import com.ridesharing.exception.ConflictException;
import com.ridesharing.exception.ResourceNotFoundException;
import com.ridesharing.repository.RideRepository;
import com.ridesharing.repository.RiderRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiderServiceTest {

    @Mock RiderRepository riderRepository;
    @Mock RideRepository rideRepository;

    @InjectMocks RiderService riderService;

    private UUID riderId;
    private Rider rider;

    @BeforeEach
    void setUp() {
        riderId = UUID.randomUUID();

        rider = new Rider();
        rider.setId(riderId);
        rider.setName("Alice");
        rider.setEmail("alice@example.com");
        rider.setPhone("555-0100");
        rider.setCreatedAt(LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────────
    // register
    // ─────────────────────────────────────────────────────────

    @Test
    void register_success() {
        RegisterRiderRequest req = new RegisterRiderRequest();
        req.setName("Alice");
        req.setEmail("alice@example.com");
        req.setPhone("555-0100");

        when(riderRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(riderRepository.save(any(Rider.class))).thenAnswer(inv -> {
            Rider r = inv.getArgument(0);
            r.setId(riderId);
            r.setCreatedAt(LocalDateTime.now());
            return r;
        });

        RiderResponse response = riderService.register(req);

        assertThat(response.getId()).isEqualTo(riderId);
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getPhone()).isEqualTo("555-0100");
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void register_duplicateEmail_throws409() {
        RegisterRiderRequest req = new RegisterRiderRequest();
        req.setName("Alice");
        req.setEmail("alice@example.com");
        req.setPhone("555-0100");

        when(riderRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> riderService.register(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("alice@example.com");

        verify(riderRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────
    // getById
    // ─────────────────────────────────────────────────────────

    @Test
    void getById_success() {
        when(riderRepository.findById(riderId)).thenReturn(Optional.of(rider));

        RiderResponse response = riderService.getById(riderId);

        assertThat(response.getId()).isEqualTo(riderId);
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void getById_notFound_throws404() {
        when(riderRepository.findById(riderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riderService.getById(riderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────────────────────────────────────
    // getRideHistory
    // ─────────────────────────────────────────────────────────

    @Test
    void getRideHistory_success_returnsRides() {
        Driver driver = new Driver();
        driver.setId(UUID.randomUUID());
        driver.setName("Bob");
        driver.setVehicleMake("Toyota");
        driver.setVehicleModel("Camry");
        driver.setLicensePlate("ABC-123");

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

        when(riderRepository.existsById(riderId)).thenReturn(true);
        when(rideRepository.findAllByRiderIdWithActors(riderId)).thenReturn(List.of(ride));

        List<RideResponse> history = riderService.getRideHistory(riderId);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(history.get(0).getRider().getId()).isEqualTo(riderId);
    }

    @Test
    void getRideHistory_emptyHistory_returnsEmptyList() {
        when(riderRepository.existsById(riderId)).thenReturn(true);
        when(rideRepository.findAllByRiderIdWithActors(riderId)).thenReturn(List.of());

        List<RideResponse> history = riderService.getRideHistory(riderId);

        assertThat(history).isEmpty();
    }

    @Test
    void getRideHistory_riderNotFound_throws404() {
        when(riderRepository.existsById(riderId)).thenReturn(false);

        assertThatThrownBy(() -> riderService.getRideHistory(riderId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(rideRepository, never()).findAllByRiderIdWithActors(any());
    }
}
