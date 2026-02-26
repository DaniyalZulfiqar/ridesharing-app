package com.ridesharing.controller;

import com.ridesharing.dto.request.RegisterDriverRequest;
import com.ridesharing.dto.request.UpdateAvailabilityRequest;
import com.ridesharing.dto.response.DriverResponse;
import com.ridesharing.dto.response.DriverSummaryResponse;
import com.ridesharing.dto.response.RideResponse;
import com.ridesharing.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    // POST /api/v1/drivers
    @PostMapping
    public ResponseEntity<DriverResponse> register(@Valid @RequestBody RegisterDriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverService.register(request));
    }

    // GET /api/v1/drivers/{driverId}
    @GetMapping("/{driverId}")
    public ResponseEntity<DriverResponse> getById(@PathVariable UUID driverId) {
        return ResponseEntity.ok(driverService.getById(driverId));
    }

    // PATCH /api/v1/drivers/{driverId}/availability
    @PatchMapping("/{driverId}/availability")
    public ResponseEntity<DriverResponse> updateAvailability(
            @PathVariable UUID driverId,
            @Valid @RequestBody UpdateAvailabilityRequest request) {
        return ResponseEntity.ok(driverService.updateAvailability(driverId, request));
    }

    // GET /api/v1/drivers/available
    @GetMapping("/available")
    public ResponseEntity<List<DriverSummaryResponse>> getAvailableDrivers() {
        return ResponseEntity.ok(driverService.getAvailableDrivers());
    }

    // GET /api/v1/drivers/{driverId}/rides
    @GetMapping("/{driverId}/rides")
    public ResponseEntity<List<RideResponse>> getRideHistory(@PathVariable UUID driverId) {
        return ResponseEntity.ok(driverService.getRideHistory(driverId));
    }
}
