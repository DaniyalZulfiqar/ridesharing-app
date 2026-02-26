package com.ridesharing.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Used for: accept, reject, start, complete ride actions.
 */
@Data
public class DriverActionRequest {

    @NotNull
    private UUID driverId;
}
