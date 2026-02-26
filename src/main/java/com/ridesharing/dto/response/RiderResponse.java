package com.ridesharing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RiderResponse {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private LocalDateTime createdAt;
}
