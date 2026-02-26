package com.ridesharing.controller;

import com.ridesharing.dto.request.CancelRideRequest;
import com.ridesharing.dto.request.DriverActionRequest;
import com.ridesharing.dto.request.RequestRideRequest;
import com.ridesharing.dto.response.RideResponse;
import com.ridesharing.enums.RideStatus;
import com.ridesharing.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    // POST /api/v1/rides
    @PostMapping
    public ResponseEntity<RideResponse> requestRide(@Valid @RequestBody RequestRideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rideService.requestRide(request));
    }

    // GET /api/v1/rides/{rideId}
    @GetMapping("/{rideId}")
    public ResponseEntity<RideResponse> getById(@PathVariable UUID rideId) {
        return ResponseEntity.ok(rideService.getById(rideId));
    }

    // GET /api/v1/rides?status=REQUESTED
    @GetMapping
    public ResponseEntity<List<RideResponse>> list(
            @RequestParam(required = false) RideStatus status) {
        return ResponseEntity.ok(rideService.listByStatus(status));
    }

    // POST /api/v1/rides/{rideId}/accept
    @PostMapping("/{rideId}/accept")
    public ResponseEntity<RideResponse> accept(
            @PathVariable UUID rideId,
            @Valid @RequestBody DriverActionRequest request) {
        return ResponseEntity.ok(rideService.acceptRide(rideId, request));
    }

    // POST /api/v1/rides/{rideId}/reject
    @PostMapping("/{rideId}/reject")
    public ResponseEntity<RideResponse> reject(
            @PathVariable UUID rideId,
            @Valid @RequestBody DriverActionRequest request) {
        return ResponseEntity.ok(rideService.rejectRide(rideId, request));
    }

    // POST /api/v1/rides/{rideId}/start
    @PostMapping("/{rideId}/start")
    public ResponseEntity<RideResponse> start(
            @PathVariable UUID rideId,
            @Valid @RequestBody DriverActionRequest request) {
        return ResponseEntity.ok(rideService.startRide(rideId, request));
    }

    // POST /api/v1/rides/{rideId}/complete
    @PostMapping("/{rideId}/complete")
    public ResponseEntity<RideResponse> complete(
            @PathVariable UUID rideId,
            @Valid @RequestBody DriverActionRequest request) {
        return ResponseEntity.ok(rideService.completeRide(rideId, request));
    }

    // POST /api/v1/rides/{rideId}/cancel
    @PostMapping("/{rideId}/cancel")
    public ResponseEntity<RideResponse> cancel(
            @PathVariable UUID rideId,
            @Valid @RequestBody CancelRideRequest request) {
        return ResponseEntity.ok(rideService.cancelRide(rideId, request));
    }
}
