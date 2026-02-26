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
        // TODO: Phase 3 — implement
        // 1. Check riderRepository.existsByEmail → ConflictException if duplicate
        // 2. Build and save Rider entity
        // 3. Map to RiderResponse and return
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ── FR-R02 ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public RiderResponse getById(UUID riderId) {
        // TODO: Phase 3 — implement
        // 1. riderRepository.findById → ResourceNotFoundException if absent
        // 2. Map to RiderResponse and return
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ── FR-R03 / FR-RI12 ─────────────────────────────────────
    @Transactional(readOnly = true)
    public List<RideResponse> getRideHistory(UUID riderId) {
        // TODO: Phase 3 — implement
        // 1. Verify rider exists → ResourceNotFoundException if absent
        // 2. rideRepository.findAllByRiderId(riderId)
        // 3. Map each Ride to RideResponse and return
        throw new UnsupportedOperationException("Not yet implemented");
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
