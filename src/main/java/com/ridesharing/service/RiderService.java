package com.ridesharing.service;

import com.ridesharing.dto.request.RegisterRiderRequest;
import com.ridesharing.dto.response.RiderResponse;
import com.ridesharing.dto.response.RideResponse;
import com.ridesharing.entity.Rider;
import com.ridesharing.exception.ConflictException;
import com.ridesharing.exception.ResourceNotFoundException;
import com.ridesharing.repository.RiderRepository;
import com.ridesharing.repository.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RiderService {

    private final RiderRepository riderRepository;
    private final RideRepository rideRepository;

    public RiderService(RiderRepository riderRepository, RideRepository rideRepository) {
        this.riderRepository = riderRepository;
        this.rideRepository = rideRepository;
    }

    // ── FR-R01 ───────────────────────────────────────────────

    public RiderResponse register(RegisterRiderRequest request) {
        if (riderRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        Rider rider = Rider.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();

        return toResponse(riderRepository.save(rider));
    }

    // ── FR-R02 ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RiderResponse getById(UUID riderId) {
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider not found: " + riderId));
        return toResponse(rider);
    }

    // ── FR-R03 / FR-RI12 ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RideResponse> getRideHistory(UUID riderId) {
        if (!riderRepository.existsById(riderId)) {
            throw new ResourceNotFoundException("Rider not found: " + riderId);
        }
        return rideRepository.findAllByRiderIdWithActors(riderId)
                .stream()
                .map(RideService::toResponse)
                .toList();
    }

    // ── mapping helper ───────────────────────────────────────
    static RiderResponse toResponse(Rider rider) {
        return RiderResponse.builder()
                .id(rider.getId())
                .name(rider.getName())
                .email(rider.getEmail())
                .phone(rider.getPhone())
                .createdAt(rider.getCreatedAt())
                .build();
    }
}
