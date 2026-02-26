package com.ridesharing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DriverResponse {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String vehicleMake;
    private String vehicleModel;
    private String licensePlate;
    private boolean available;
    private LocalDateTime createdAt;
}
