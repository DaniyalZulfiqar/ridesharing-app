package com.ridesharing.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAvailabilityRequest {

    @NotNull
    private Boolean available;
}
