package com.ridesharing.controller;

import com.ridesharing.AbstractIntegrationTest;
import com.ridesharing.dto.request.*;
import com.ridesharing.dto.response.DriverResponse;
import com.ridesharing.dto.response.RideResponse;
import com.ridesharing.dto.response.RiderResponse;
import com.ridesharing.enums.RideStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RideControllerIT extends AbstractIntegrationTest {

    // ─────────────────────────────────────────────────────────
    // POST /api/v1/rides
    // ─────────────────────────────────────────────────────────

    @Test
    void requestRide_noDriverAvailable_returns503() {
        RiderResponse rider = registerRider();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/rides", buildRideRequest(rider.getId()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void requestRide_driverAvailable_returns201WithBody() {
        RiderResponse rider = registerRider();
        makeDriverAvailable();

        ResponseEntity<RideResponse> response = restTemplate.postForEntity(
                "/api/v1/rides", buildRideRequest(rider.getId()), RideResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        RideResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();
        assertThat(body.getStatus()).isEqualTo(RideStatus.REQUESTED);
        assertThat(body.getRider().getId()).isEqualTo(rider.getId());
        assertThat(body.getFareAmount()).isNotNull();
        assertThat(body.getPickupLocation()).isEqualTo("123 Main St");
        assertThat(body.getDropoffLocation()).isEqualTo("456 Oak Ave");
        assertThat(body.getRequestedAt()).isNotNull();
    }

    @Test
    void requestRide_riderAlreadyHasActiveRide_returns409() {
        RiderResponse rider = registerRider();
        makeDriverAvailable();
        restTemplate.postForEntity("/api/v1/rides", buildRideRequest(rider.getId()), String.class);
        makeDriverAvailable(); // another available driver for the second attempt

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/rides", buildRideRequest(rider.getId()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void requestRide_unknownRider_returns404() {
        makeDriverAvailable();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/rides", buildRideRequest(UUID.randomUUID()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void requestRide_missingRequiredFields_returns400() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/rides", new RequestRideRequest(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/rides/{rideId}
    // ─────────────────────────────────────────────────────────

    @Test
    void getById_existingRide_returns200() {
        RideResponse created = createRide();

        ResponseEntity<RideResponse> response = restTemplate.getForEntity(
                "/api/v1/rides/" + created.getId(), RideResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(created.getId());
        assertThat(response.getBody().getStatus()).isEqualTo(RideStatus.REQUESTED);
    }

    @Test
    void getById_unknownId_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/rides/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/rides?status=...
    // ─────────────────────────────────────────────────────────

    @Test
    void list_noFilter_includesCreatedRide() {
        RideResponse created = createRide();

        ResponseEntity<RideResponse[]> response = restTemplate.getForEntity(
                "/api/v1/rides", RideResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        boolean found = java.util.Arrays.stream(response.getBody())
                .anyMatch(r -> r.getId().equals(created.getId()));
        assertThat(found).as("Newly created ride must appear in the list").isTrue();
    }

    @Test
    void list_filteredByStatus_returnsOnlyMatchingStatus() {
        createRide(); // creates a REQUESTED ride

        ResponseEntity<RideResponse[]> response = restTemplate.getForEntity(
                "/api/v1/rides?status=REQUESTED", RideResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        for (RideResponse r : response.getBody()) {
            assertThat(r.getStatus()).isEqualTo(RideStatus.REQUESTED);
        }
    }

    // ─────────────────────────────────────────────────────────
    // Full lifecycle: REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED
    // ─────────────────────────────────────────────────────────

    @Test
    void fullLifecycle_requestAcceptStartComplete_allStatusTransitionsCorrect() {
        RiderResponse rider = registerRider();
        DriverResponse driver = makeDriverAvailable();

        // request
        RideResponse ride = restTemplate.postForEntity(
                "/api/v1/rides", buildRideRequest(rider.getId()), RideResponse.class).getBody();
        assertThat(ride.getStatus()).isEqualTo(RideStatus.REQUESTED);

        DriverActionRequest driverReq = new DriverActionRequest();
        driverReq.setDriverId(driver.getId());

        // accept
        ResponseEntity<RideResponse> accepted = restTemplate.postForEntity(
                "/api/v1/rides/" + ride.getId() + "/accept", driverReq, RideResponse.class);
        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(accepted.getBody().getStatus()).isEqualTo(RideStatus.ACCEPTED);
        assertThat(accepted.getBody().getDriver().getId()).isEqualTo(driver.getId());
        assertThat(accepted.getBody().getAcceptedAt()).isNotNull();

        // start
        ResponseEntity<RideResponse> started = restTemplate.postForEntity(
                "/api/v1/rides/" + ride.getId() + "/start", driverReq, RideResponse.class);
        assertThat(started.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(started.getBody().getStatus()).isEqualTo(RideStatus.IN_PROGRESS);
        assertThat(started.getBody().getStartedAt()).isNotNull();

        // complete
        ResponseEntity<RideResponse> completed = restTemplate.postForEntity(
                "/api/v1/rides/" + ride.getId() + "/complete", driverReq, RideResponse.class);
        assertThat(completed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(completed.getBody().getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(completed.getBody().getCompletedAt()).isNotNull();

        // driver ride history should now contain the completed ride
        ResponseEntity<RideResponse[]> driverHistory = restTemplate.getForEntity(
                "/api/v1/drivers/" + driver.getId() + "/rides", RideResponse[].class);
        assertThat(driverHistory.getBody()).hasSize(1);
        assertThat(driverHistory.getBody()[0].getId()).isEqualTo(ride.getId());

        // rider ride history should also contain it
        ResponseEntity<RideResponse[]> riderHistory = restTemplate.getForEntity(
                "/api/v1/riders/" + rider.getId() + "/rides", RideResponse[].class);
        assertThat(riderHistory.getBody()).hasSize(1);
        assertThat(riderHistory.getBody()[0].getStatus()).isEqualTo(RideStatus.COMPLETED);
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/v1/rides/{rideId}/reject
    // ─────────────────────────────────────────────────────────

    @Test
    void rejectRide_requestedRide_returns200StillRequested() {
        RiderResponse rider = registerRider();
        DriverResponse driver = makeDriverAvailable();
        RideResponse ride = restTemplate.postForEntity(
                "/api/v1/rides", buildRideRequest(rider.getId()), RideResponse.class).getBody();

        DriverActionRequest driverReq = new DriverActionRequest();
        driverReq.setDriverId(driver.getId());

        ResponseEntity<RideResponse> response = restTemplate.postForEntity(
                "/api/v1/rides/" + ride.getId() + "/reject", driverReq, RideResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Rejecting a ride does NOT cancel it — it stays REQUESTED so another driver can accept.
        assertThat(response.getBody().getStatus()).isEqualTo(RideStatus.REQUESTED);
    }

    @Test
    void rejectRide_unknownRide_returns404() {
        DriverResponse driver = makeDriverAvailable();
        DriverActionRequest driverReq = new DriverActionRequest();
        driverReq.setDriverId(driver.getId());

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/rides/" + UUID.randomUUID() + "/reject", driverReq, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─────────────────────────────────────────────────────────
    // POST /api/v1/rides/{rideId}/cancel
    // ─────────────────────────────────────────────────────────

    @Test
    void cancelRide_byRider_beforeAccept_returns200Cancelled() {
        RiderResponse rider = registerRider();
        makeDriverAvailable();
        RideResponse ride = restTemplate.postForEntity(
                "/api/v1/rides", buildRideRequest(rider.getId()), RideResponse.class).getBody();

        ResponseEntity<RideResponse> response = restTemplate.postForEntity(
                "/api/v1/rides/" + ride.getId() + "/cancel",
                buildCancelRequest("RIDER", rider.getId()),
                RideResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(response.getBody().getCancelledAt()).isNotNull();
        assertThat(response.getBody().getCancellationReason()).isEqualTo("Changed my mind");
    }

    @Test
    void cancelRide_byDriver_afterAccept_returns200Cancelled() {
        RiderResponse rider = registerRider();
        DriverResponse driver = makeDriverAvailable();
        RideResponse ride = restTemplate.postForEntity(
                "/api/v1/rides", buildRideRequest(rider.getId()), RideResponse.class).getBody();

        // accept first
        DriverActionRequest driverReq = new DriverActionRequest();
        driverReq.setDriverId(driver.getId());
        restTemplate.postForEntity("/api/v1/rides/" + ride.getId() + "/accept",
                driverReq, RideResponse.class);

        ResponseEntity<RideResponse> response = restTemplate.postForEntity(
                "/api/v1/rides/" + ride.getId() + "/cancel",
                buildCancelRequest("DRIVER", driver.getId()),
                RideResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(RideStatus.CANCELLED);
    }

    @Test
    void cancelRide_wrongRiderId_returns403() {
        RiderResponse rider = registerRider();
        makeDriverAvailable();
        RideResponse ride = restTemplate.postForEntity(
                "/api/v1/rides", buildRideRequest(rider.getId()), RideResponse.class).getBody();

        // Different rider tries to cancel
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/rides/" + ride.getId() + "/cancel",
                buildCancelRequest("RIDER", UUID.randomUUID()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void cancelRide_alreadyCompleted_returns409() {
        RiderResponse rider = registerRider();
        DriverResponse driver = makeDriverAvailable();
        RideResponse ride = restTemplate.postForEntity(
                "/api/v1/rides", buildRideRequest(rider.getId()), RideResponse.class).getBody();

        DriverActionRequest driverReq = new DriverActionRequest();
        driverReq.setDriverId(driver.getId());
        restTemplate.postForEntity("/api/v1/rides/" + ride.getId() + "/accept", driverReq, String.class);
        restTemplate.postForEntity("/api/v1/rides/" + ride.getId() + "/start", driverReq, String.class);
        restTemplate.postForEntity("/api/v1/rides/" + ride.getId() + "/complete", driverReq, String.class);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/rides/" + ride.getId() + "/cancel",
                buildCancelRequest("RIDER", rider.getId()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void acceptRide_driverNotAvailable_returns409() {
        RiderResponse rider = registerRider();
        DriverResponse driver = makeDriverAvailable();
        RideResponse ride = restTemplate.postForEntity(
                "/api/v1/rides", buildRideRequest(rider.getId()), RideResponse.class).getBody();

        // Set driver offline before accepting
        UpdateAvailabilityRequest offline = new UpdateAvailabilityRequest();
        offline.setAvailable(false);
        restTemplate.exchange("/api/v1/drivers/" + driver.getId() + "/availability",
                HttpMethod.PATCH, new HttpEntity<>(offline), DriverResponse.class);

        DriverActionRequest driverReq = new DriverActionRequest();
        driverReq.setDriverId(driver.getId());

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/rides/" + ride.getId() + "/accept", driverReq, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ─────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────

    private RiderResponse registerRider() {
        RegisterRiderRequest req = new RegisterRiderRequest();
        req.setName("Alice");
        req.setEmail("alice-" + UUID.randomUUID() + "@example.com");
        req.setPhone("555-0100");
        return restTemplate.postForEntity("/api/v1/riders", req, RiderResponse.class).getBody();
    }

    private DriverResponse makeDriverAvailable() {
        RegisterDriverRequest req = new RegisterDriverRequest();
        req.setName("Bob");
        req.setEmail("bob-" + UUID.randomUUID() + "@example.com");
        req.setPhone("555-0200");
        req.setVehicleMake("Toyota");
        req.setVehicleModel("Camry");
        req.setLicensePlate(UUID.randomUUID().toString().substring(0, 7).toUpperCase());
        DriverResponse driver = restTemplate.postForEntity(
                "/api/v1/drivers", req, DriverResponse.class).getBody();

        UpdateAvailabilityRequest avail = new UpdateAvailabilityRequest();
        avail.setAvailable(true);
        restTemplate.exchange("/api/v1/drivers/" + driver.getId() + "/availability",
                HttpMethod.PATCH, new HttpEntity<>(avail), DriverResponse.class);

        return driver;
    }

    private RideResponse createRide() {
        RiderResponse rider = registerRider();
        makeDriverAvailable();
        return restTemplate.postForEntity(
                "/api/v1/rides", buildRideRequest(rider.getId()), RideResponse.class).getBody();
    }

    private RequestRideRequest buildRideRequest(UUID riderId) {
        RequestRideRequest req = new RequestRideRequest();
        req.setRiderId(riderId);
        req.setPickupLocation("123 Main St");
        req.setDropoffLocation("456 Oak Ave");
        req.setDistanceKm(5.0);
        return req;
    }

    private CancelRideRequest buildCancelRequest(String cancelledBy, UUID actorId) {
        CancelRideRequest req = new CancelRideRequest();
        req.setCancelledBy(cancelledBy);
        req.setActorId(actorId);
        req.setReason("Changed my mind");
        return req;
    }
}
