package com.ridesharing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Embedded inside RideResponse to represent the rider without all fields.
 */
@Data
@Builder
public class RiderSummaryResponse {

    private UUID id;
    private String name;
}
