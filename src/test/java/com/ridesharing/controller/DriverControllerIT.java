package com.ridesharing.controller;

import com.ridesharing.AbstractIntegrationTest;
import com.ridesharing.dto.request.RegisterDriverRequest;
import com.ridesharing.dto.request.UpdateAvailabilityRequest;
import com.ridesharing.dto.response.DriverResponse;
import com.ridesharing.dto.response.DriverSummaryResponse;
import com.ridesharing.dto.response.RideResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DriverControllerIT extends AbstractIntegrationTest {

    // ─────────────────────────────────────────────────────────
    // POST /api/v1/drivers
    // ─────────────────────────────────────────────────────────

    @Test
    void register_validRequest_returns201WithBody() {
        RegisterDriverRequest req = buildRegisterRequest(uniqueEmail(), uniquePlate());

        ResponseEntity<DriverResponse> response = restTemplate.postForEntity(
                "/api/v1/drivers", req, DriverResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        DriverResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();
        assertThat(body.getName()).isEqualTo("Bob");
        assertThat(body.getEmail()).isEqualTo(req.getEmail());
        assertThat(body.getVehicleMake()).isEqualTo("Toyota");
        assertThat(body.getLicensePlate()).isEqualTo(req.getLicensePlate());
        assertThat(body.isAvailable()).isFalse();
        assertThat(body.getCreatedAt()).isNotNull();
    }

    @Test
    void register_duplicateEmail_returns409() {
        String email = uniqueEmail();
        restTemplate.postForEntity("/api/v1/drivers",
                buildRegisterRequest(email, uniquePlate()), String.class);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/drivers",
                buildRegisterRequest(email, uniquePlate()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_duplicateLicensePlate_returns409() {
        String plate = uniquePlate();
        restTemplate.postForEntity("/api/v1/drivers",
                buildRegisterRequest(uniqueEmail(), plate), String.class);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/drivers",
                buildRegisterRequest(uniqueEmail(), plate), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_blankVehicleMake_returns400() {
        RegisterDriverRequest req = buildRegisterRequest(uniqueEmail(), uniquePlate());
        req.setVehicleMake("");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/drivers", req, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/drivers/{driverId}
    // ─────────────────────────────────────────────────────────

    @Test
    void getById_existingDriver_returns200() {
        DriverResponse created = registerDriver();

        ResponseEntity<DriverResponse> response = restTemplate.getForEntity(
                "/api/v1/drivers/" + created.getId(), DriverResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(created.getId());
        assertThat(response.getBody().getLicensePlate()).isEqualTo(created.getLicensePlate());
    }

    @Test
    void getById_unknownId_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/drivers/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─────────────────────────────────────────────────────────
    // PATCH /api/v1/drivers/{driverId}/availability
    // ─────────────────────────────────────────────────────────

    @Test
    void updateAvailability_setOnline_returns200Available() {
        DriverResponse created = registerDriver();
        UpdateAvailabilityRequest req = new UpdateAvailabilityRequest();
        req.setAvailable(true);

        ResponseEntity<DriverResponse> response = restTemplate.exchange(
                "/api/v1/drivers/" + created.getId() + "/availability",
                HttpMethod.PATCH,
                new HttpEntity<>(req),
                DriverResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isAvailable()).isTrue();
    }

    @Test
    void updateAvailability_setOffline_returns200Unavailable() {
        DriverResponse created = registerDriver();
        // First go online
        UpdateAvailabilityRequest online = new UpdateAvailabilityRequest();
        online.setAvailable(true);
        restTemplate.exchange("/api/v1/drivers/" + created.getId() + "/availability",
                HttpMethod.PATCH, new HttpEntity<>(online), DriverResponse.class);
        // Then go offline
        UpdateAvailabilityRequest offline = new UpdateAvailabilityRequest();
        offline.setAvailable(false);

        ResponseEntity<DriverResponse> response = restTemplate.exchange(
                "/api/v1/drivers/" + created.getId() + "/availability",
                HttpMethod.PATCH,
                new HttpEntity<>(offline),
                DriverResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isAvailable()).isFalse();
    }

    @Test
    void updateAvailability_missingAvailableField_returns400() {
        DriverResponse created = registerDriver();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/drivers/" + created.getId() + "/availability",
                HttpMethod.PATCH,
                new HttpEntity<>("{}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateAvailability_unknownDriver_returns404() {
        UpdateAvailabilityRequest req = new UpdateAvailabilityRequest();
        req.setAvailable(true);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/drivers/" + UUID.randomUUID() + "/availability",
                HttpMethod.PATCH,
                new HttpEntity<>(req),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/drivers/available
    // ─────────────────────────────────────────────────────────

    @Test
    void getAvailableDrivers_driverMarkedOnline_appearsInList() {
        DriverResponse created = registerDriver();
        UpdateAvailabilityRequest req = new UpdateAvailabilityRequest();
        req.setAvailable(true);
        restTemplate.exchange("/api/v1/drivers/" + created.getId() + "/availability",
                HttpMethod.PATCH, new HttpEntity<>(req), DriverResponse.class);

        ResponseEntity<DriverSummaryResponse[]> response = restTemplate.getForEntity(
                "/api/v1/drivers/available", DriverSummaryResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        boolean found = Arrays.stream(response.getBody())
                .anyMatch(d -> d.getId().equals(created.getId()));
        assertThat(found).as("Online driver must appear in available list").isTrue();
    }

    @Test
    void getAvailableDrivers_driverOffline_doesNotAppearInList() {
        DriverResponse created = registerDriver(); // available=false by default

        ResponseEntity<DriverSummaryResponse[]> response = restTemplate.getForEntity(
                "/api/v1/drivers/available", DriverSummaryResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        boolean found = response.getBody() != null && Arrays.stream(response.getBody())
                .anyMatch(d -> d.getId().equals(created.getId()));
        assertThat(found).as("Offline driver must not appear in available list").isFalse();
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/drivers/{driverId}/rides
    // ─────────────────────────────────────────────────────────

    @Test
    void getRideHistory_noRides_returns200EmptyList() {
        DriverResponse created = registerDriver();

        ResponseEntity<RideResponse[]> response = restTemplate.getForEntity(
                "/api/v1/drivers/" + created.getId() + "/rides", RideResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getRideHistory_unknownDriver_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/drivers/" + UUID.randomUUID() + "/rides", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────

    private DriverResponse registerDriver() {
        return restTemplate.postForEntity("/api/v1/drivers",
                buildRegisterRequest(uniqueEmail(), uniquePlate()),
                DriverResponse.class).getBody();
    }

    private RegisterDriverRequest buildRegisterRequest(String email, String plate) {
        RegisterDriverRequest req = new RegisterDriverRequest();
        req.setName("Bob");
        req.setEmail(email);
        req.setPhone("555-0200");
        req.setVehicleMake("Toyota");
        req.setVehicleModel("Camry");
        req.setLicensePlate(plate);
        return req;
    }

    private String uniqueEmail() {
        return "bob-" + UUID.randomUUID() + "@example.com";
    }

    private String uniquePlate() {
        return UUID.randomUUID().toString().substring(0, 7).toUpperCase();
    }
}
