package com.ridesharing.controller;

import com.ridesharing.AbstractIntegrationTest;
import com.ridesharing.dto.request.RegisterRiderRequest;
import com.ridesharing.dto.response.RideResponse;
import com.ridesharing.dto.response.RiderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RiderControllerIT extends AbstractIntegrationTest {

    // ─────────────────────────────────────────────────────────
    // POST /api/v1/riders
    // ─────────────────────────────────────────────────────────

    @Test
    void register_validRequest_returns201WithBody() {
        RegisterRiderRequest req = buildRegisterRequest();

        ResponseEntity<RiderResponse> response = restTemplate.postForEntity(
                "/api/v1/riders", req, RiderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        RiderResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();
        assertThat(body.getName()).isEqualTo("Alice");
        assertThat(body.getEmail()).isEqualTo(req.getEmail());
        assertThat(body.getPhone()).isEqualTo("555-0100");
        assertThat(body.getCreatedAt()).isNotNull();
    }

    @Test
    void register_duplicateEmail_returns409() {
        RegisterRiderRequest req = buildRegisterRequest();
        restTemplate.postForEntity("/api/v1/riders", req, String.class);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/riders", req, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_blankName_returns400() {
        RegisterRiderRequest req = buildRegisterRequest();
        req.setName("");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/riders", req, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_invalidEmail_returns400() {
        RegisterRiderRequest req = buildRegisterRequest();
        req.setEmail("not-an-email");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/riders", req, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/riders/{riderId}
    // ─────────────────────────────────────────────────────────

    @Test
    void getById_existingRider_returns200WithBody() {
        RegisterRiderRequest req = buildRegisterRequest();
        RiderResponse created = restTemplate.postForEntity(
                "/api/v1/riders", req, RiderResponse.class).getBody();

        ResponseEntity<RiderResponse> response = restTemplate.getForEntity(
                "/api/v1/riders/" + created.getId(), RiderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(created.getId());
        assertThat(response.getBody().getEmail()).isEqualTo(req.getEmail());
    }

    @Test
    void getById_unknownId_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/riders/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/riders/{riderId}/rides
    // ─────────────────────────────────────────────────────────

    @Test
    void getRideHistory_noRides_returns200EmptyList() {
        RegisterRiderRequest req = buildRegisterRequest();
        RiderResponse created = restTemplate.postForEntity(
                "/api/v1/riders", req, RiderResponse.class).getBody();

        ResponseEntity<RideResponse[]> response = restTemplate.getForEntity(
                "/api/v1/riders/" + created.getId() + "/rides", RideResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getRideHistory_unknownRider_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/riders/" + UUID.randomUUID() + "/rides", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────

    private RegisterRiderRequest buildRegisterRequest() {
        RegisterRiderRequest req = new RegisterRiderRequest();
        req.setName("Alice");
        req.setEmail("alice-" + UUID.randomUUID() + "@example.com");
        req.setPhone("555-0100");
        return req;
    }
}
