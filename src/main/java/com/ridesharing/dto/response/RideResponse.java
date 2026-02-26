package com.ridesharing.dto.response;

import com.ridesharing.enums.RideStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RideResponse {

    private UUID id;
    private RiderSummaryResponse rider;
    private DriverSummaryResponse driver;   // null until a driver accepts
    private String pickupLocation;
    private String dropoffLocation;
    private Double distanceKm;
    private BigDecimal fareAmount;
    private RideStatus status;
    private String cancellationReason;
    private LocalDateTime requestedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
}
