package com.ridesharing.controller;

import com.ridesharing.dto.request.RegisterRiderRequest;
import com.ridesharing.dto.response.RiderResponse;
import com.ridesharing.dto.response.RideResponse;
import com.ridesharing.service.RiderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/riders")
public class RiderController {

    private final RiderService riderService;

    public RiderController(RiderService riderService) {
        this.riderService = riderService;
    }

    // POST /api/v1/riders
    @PostMapping
    public ResponseEntity<RiderResponse> register(@Valid @RequestBody RegisterRiderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(riderService.register(request));
    }

    // GET /api/v1/riders/{riderId}
    @GetMapping("/{riderId}")
    public ResponseEntity<RiderResponse> getById(@PathVariable UUID riderId) {
        return ResponseEntity.ok(riderService.getById(riderId));
    }

    // GET /api/v1/riders/{riderId}/rides
    @GetMapping("/{riderId}/rides")
    public ResponseEntity<List<RideResponse>> getRideHistory(@PathVariable UUID riderId) {
        return ResponseEntity.ok(riderService.getRideHistory(riderId));
    }
}
