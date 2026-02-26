package com.ridesharing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class RequestRideRequest {

    @NotNull
    private UUID riderId;

    @NotBlank
    private String pickupLocation;

    @NotBlank
    private String dropoffLocation;

    @NotNull
    @Positive
    private Double distanceKm;
}
