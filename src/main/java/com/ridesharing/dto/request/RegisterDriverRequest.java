package com.ridesharing.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDriverRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(max = 15)
    private String phone;

    @NotBlank
    private String vehicleMake;

    @NotBlank
    private String vehicleModel;

    @NotBlank
    @Size(max = 20)
    private String licensePlate;
}
