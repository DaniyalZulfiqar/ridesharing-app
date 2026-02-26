package com.ridesharing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Used in two places:
 *  1. Embedded inside RideResponse for the assigned driver.
 *  2. Items in the GET /drivers/available list response.
 */
@Data
@Builder
public class DriverSummaryResponse {

    private UUID id;
    private String name;
    private String vehicleMake;
    private String vehicleModel;
    private String licensePlate;
}
