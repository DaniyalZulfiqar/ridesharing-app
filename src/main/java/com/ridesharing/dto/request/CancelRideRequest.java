package com.ridesharing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class CancelRideRequest {

    @NotNull
    @Pattern(regexp = "RIDER|DRIVER", message = "must be RIDER or DRIVER")
    private String cancelledBy;

    @NotNull
    private UUID actorId;

    @NotBlank
    private String reason;
}
