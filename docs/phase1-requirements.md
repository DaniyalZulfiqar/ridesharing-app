# Ridesharing App — Phase 1 SDLC Artifact
**Version:** 1.0
**Phase:** MVP
**Date:** 2026-02-25
**Status:** Draft

---

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Stakeholders & Roles](#2-stakeholders--roles)
3. [Functional Requirements](#3-functional-requirements)
4. [Non-Functional Requirements](#4-non-functional-requirements)
5. [Assumptions & Constraints](#5-assumptions--constraints)
6. [Tech Stack](#6-tech-stack)
7. [Domain Model](#7-domain-model)
8. [Ride State Machine](#8-ride-state-machine)
9. [Fare Calculation Logic](#9-fare-calculation-logic)
10. [API Contract](#10-api-contract)
11. [Error Handling](#11-error-handling)
12. [Out of Scope (Phase 1)](#12-out-of-scope-phase-1)

---

## 1. Project Overview

A backend REST API for a ridesharing platform where **Riders** can request rides and **Drivers** can accept and complete them. Phase 1 focuses on the core ride lifecycle with no authentication, no real-time tracking, and no payment processing.

**Primary Goal:** Establish a clean, well-structured backend that can be iteratively extended in future phases.

---

## 2. Stakeholders & Roles

| Role   | Description                                                         |
|--------|---------------------------------------------------------------------|
| Rider  | A registered user who requests rides from a pickup to a drop-off   |
| Driver | A registered user with a vehicle who accepts and fulfills rides     |
| System | The backend application responsible for matching, fare, and state   |

> Riders and Drivers are **separate entities** — a user cannot be both in Phase 1.

---

## 3. Functional Requirements

### 3.1 Rider Management
- **FR-R01:** A Rider can register with name, email, and phone number
- **FR-R02:** A Rider can retrieve their profile by ID
- **FR-R03:** A Rider can view their ride history

### 3.2 Driver Management
- **FR-D01:** A Driver can register with name, email, phone, and vehicle details
- **FR-D02:** A Driver can retrieve their profile by ID
- **FR-D03:** A Driver can toggle their availability (online / offline)
- **FR-D04:** A Driver can view their ride history

### 3.3 Ride Request & Matching
- **FR-RI01:** A Rider can request a ride by providing pickup location, drop-off location, and distance in km
- **FR-RI02:** The system must check that at least one Driver is available before creating the ride
- **FR-RI03:** If no Driver is available, the system returns an error immediately — no queuing
- **FR-RI04:** The system calculates and stores the estimated fare at the time of request
- **FR-RI05:** A Ride is created in `REQUESTED` status and remains open until a Driver acts

### 3.4 Driver Ride Actions
- **FR-RI06:** A Driver can view all rides in `REQUESTED` status
- **FR-RI07:** A Driver can accept a `REQUESTED` ride — status moves to `ACCEPTED`; Driver is locked to this ride
- **FR-RI08:** A Driver can reject a `REQUESTED` ride — ride remains `REQUESTED` for other drivers
- **FR-RI09:** A Driver can mark an `ACCEPTED` ride as started — status moves to `IN_PROGRESS`
- **FR-RI10:** A Driver can mark an `IN_PROGRESS` ride as completed — status moves to `COMPLETED`

### 3.5 Cancellation
- **FR-C01:** A Rider can cancel a ride that is in `REQUESTED` or `ACCEPTED` status
- **FR-C02:** A Driver can cancel a ride that is in `ACCEPTED` status
- **FR-C03:** Cancelled rides move to `CANCELLED` status with a cancellation reason
- **FR-C04:** When a Driver cancels, they become available again

### 3.6 Ride Retrieval
- **FR-RI11:** Any ride can be retrieved by its ID
- **FR-RI12:** Rider can fetch their full ride history
- **FR-RI13:** Driver can fetch their full ride history

---

## 4. Non-Functional Requirements

| ID     | Requirement                                                                 |
|--------|-----------------------------------------------------------------------------|
| NFR-01 | API must follow RESTful conventions with consistent JSON responses           |
| NFR-02 | All entity IDs must be UUIDs                                                 |
| NFR-03 | All timestamps must be in ISO-8601 UTC format                                |
| NFR-04 | Error responses must follow the `problem+json` standard structure            |
| NFR-05 | Fare amounts must use `BigDecimal` — never `float` or `double`               |
| NFR-06 | API must be versioned under `/api/v1/`                                       |
| NFR-07 | Application must be stateless — no server-side session                       |
| NFR-08 | All business logic must be covered by unit tests (JUnit 5 + Mockito)         |
| NFR-09 | Code must follow standard Java/Spring conventions and layered architecture   |

---

## 5. Assumptions & Constraints

| # | Assumption / Constraint |
|---|-------------------------|
| A1 | No authentication in Phase 1 — all endpoints are open |
| A2 | Locations are represented as plain address strings (no GPS coordinates) |
| A3 | Distance in km is provided by the Rider at request time — no routing engine |
| A4 | A Driver can only have one active ride at a time (ACCEPTED or IN_PROGRESS) |
| A5 | A Rider can only have one active ride at a time (REQUESTED or ACCEPTED or IN_PROGRESS) |
| A6 | Fare is calculated at request time and does not change |
| A7 | No notifications, emails, or webhooks in Phase 1 |
| A8 | All data is persisted in MySQL — no in-memory fallback |

---

## 6. Tech Stack

| Layer              | Technology                  | Version    |
|--------------------|-----------------------------|------------|
| Language           | Java                        | 21 (LTS)   |
| Framework          | Spring Boot                 | 3.x        |
| Build Tool         | Maven                       | 3.9+       |
| Database           | MySQL                       | 8.x        |
| ORM                | Spring Data JPA (Hibernate) | Latest     |
| Validation         | Jakarta Bean Validation     | Latest     |
| API Docs           | SpringDoc OpenAPI (Swagger) | Latest     |
| Unit Testing       | JUnit 5                     | Latest     |
| Mocking            | Mockito                     | Latest     |
| Integration Tests  | Spring Boot Test            | Latest     |
| Utilities          | Lombok                      | Latest     |

### Project Structure (Layered Architecture)
```
com.ridesharing
├── controller        # REST controllers (HTTP layer)
├── service           # Business logic
├── repository        # Spring Data JPA repositories
├── entity            # JPA entities
├── dto
│   ├── request       # Incoming request payloads
│   └── response      # Outgoing response payloads
├── enums             # RideStatus, etc.
├── exception         # Custom exceptions + global handler
└── util              # Fare calculator, helpers
```

---

## 7. Domain Model

### 7.1 Rider

| Field       | Type         | Constraints              |
|-------------|--------------|--------------------------|
| id          | UUID         | PK, auto-generated       |
| name        | String       | Not null, max 100 chars  |
| email       | String       | Not null, unique         |
| phone       | String       | Not null, max 15 chars   |
| createdAt   | LocalDateTime| Auto-set on creation     |

### 7.2 Driver

| Field          | Type          | Constraints             |
|----------------|---------------|-------------------------|
| id             | UUID          | PK, auto-generated      |
| name           | String        | Not null, max 100 chars |
| email          | String        | Not null, unique        |
| phone          | String        | Not null, max 15 chars  |
| vehicleMake    | String        | Not null e.g. Toyota    |
| vehicleModel   | String        | Not null e.g. Corolla   |
| licensePlate   | String        | Not null, unique        |
| available      | Boolean       | Default: false          |
| createdAt      | LocalDateTime | Auto-set on creation    |

### 7.3 Ride

| Field              | Type          | Constraints                              |
|--------------------|---------------|------------------------------------------|
| id                 | UUID          | PK, auto-generated                       |
| rider              | Rider         | FK, Not null                             |
| driver             | Driver        | FK, Nullable (assigned on acceptance)    |
| pickupLocation     | String        | Not null                                 |
| dropoffLocation    | String        | Not null                                 |
| distanceKm         | Double        | Not null, > 0                            |
| fareAmount         | BigDecimal    | Calculated at request time               |
| status             | RideStatus    | Enum, default: REQUESTED                 |
| cancellationReason | String        | Nullable                                 |
| requestedAt        | LocalDateTime | Auto-set on creation                     |
| acceptedAt         | LocalDateTime | Set when Driver accepts                  |
| startedAt          | LocalDateTime | Set when Driver starts ride              |
| completedAt        | LocalDateTime | Set when Driver completes ride           |
| cancelledAt        | LocalDateTime | Set on cancellation                      |

---

## 8. Ride State Machine

```
                    ┌─────────────┐
                    │  REQUESTED  │◄──── Rider requests ride
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
        Driver rejects   Rider/Driver   Driver accepts
        (stays open)     cancels        │
              │            │            ▼
              │            │      ┌───────────┐
              │            └─────►│ CANCELLED │
              │                   └───────────┘
              │
              ▼
       ┌─────────────┐
       │  ACCEPTED   │◄──── Driver accepts ride
       └──────┬──────┘
              │
    ┌─────────┼──────────┐
    │                    │
  Rider/Driver         Driver starts
  cancels               │
    │                   ▼
    │            ┌─────────────┐
    │            │  IN_PROGRESS│
    │            └──────┬──────┘
    │                   │
    │              Driver completes
    │                   │
    ▼                   ▼
┌───────────┐    ┌───────────────┐
│ CANCELLED │    │   COMPLETED   │
└───────────┘    └───────────────┘
```

**Valid Transitions:**

| From        | Action                   | To          | Actor  |
|-------------|--------------------------|-------------|--------|
| REQUESTED   | Driver accepts           | ACCEPTED    | Driver |
| REQUESTED   | Rider cancels            | CANCELLED   | Rider  |
| ACCEPTED    | Driver starts ride       | IN_PROGRESS | Driver |
| ACCEPTED    | Rider cancels            | CANCELLED   | Rider  |
| ACCEPTED    | Driver cancels           | CANCELLED   | Driver |
| IN_PROGRESS | Driver completes ride    | COMPLETED   | Driver |

---

## 9. Fare Calculation Logic

```
Total Fare = BASE_FARE + (distanceKm × PER_KM_RATE)
```

| Parameter    | Value  |
|--------------|--------|
| BASE_FARE    | $2.00  |
| PER_KM_RATE  | $1.50  |

**Examples:**
- 5 km → $2.00 + (5 × $1.50) = **$9.50**
- 10 km → $2.00 + (10 × $1.50) = **$17.00**
- 1 km → $2.00 + (1 × $1.50) = **$3.50**

> Fare is stored as `BigDecimal` with 2 decimal places. Rounding mode: `HALF_UP`.

---

## 10. API Contract

**Base URL:** `http://localhost:8080/api/v1`
**Content-Type:** `application/json`

---

### 10.1 Rider Endpoints

---

#### `POST /riders` — Register a Rider

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "phone": "+14155552671"
}
```

**Response `201 Created`:**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "phone": "+14155552671",
  "createdAt": "2026-02-25T10:00:00Z"
}
```

**Errors:**
| Status | Scenario |
|--------|----------|
| 400    | Missing/invalid fields |
| 409    | Email already registered |

---

#### `GET /riders/{riderId}` — Get Rider Profile

**Response `200 OK`:**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "phone": "+14155552671",
  "createdAt": "2026-02-25T10:00:00Z"
}
```

**Errors:**
| Status | Scenario |
|--------|----------|
| 404    | Rider not found |

---

#### `GET /riders/{riderId}/rides` — Get Rider's Ride History

**Response `200 OK`:**
```json
[
  {
    "id": "ride-uuid-1",
    "pickupLocation": "123 Main St, Springfield",
    "dropoffLocation": "456 Oak Ave, Shelbyville",
    "distanceKm": 8.5,
    "fareAmount": 14.75,
    "status": "COMPLETED",
    "requestedAt": "2026-02-25T10:00:00Z",
    "completedAt": "2026-02-25T10:35:00Z"
  }
]
```

---

### 10.2 Driver Endpoints

---

#### `POST /drivers` — Register a Driver

**Request Body:**
```json
{
  "name": "Jane Smith",
  "email": "jane.smith@example.com",
  "phone": "+14155559876",
  "vehicleMake": "Toyota",
  "vehicleModel": "Corolla",
  "licensePlate": "ABC-1234"
}
```

**Response `201 Created`:**
```json
{
  "id": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "name": "Jane Smith",
  "email": "jane.smith@example.com",
  "phone": "+14155559876",
  "vehicleMake": "Toyota",
  "vehicleModel": "Corolla",
  "licensePlate": "ABC-1234",
  "available": false,
  "createdAt": "2026-02-25T09:00:00Z"
}
```

**Errors:**
| Status | Scenario |
|--------|----------|
| 400    | Missing/invalid fields |
| 409    | Email or license plate already registered |

---

#### `GET /drivers/{driverId}` — Get Driver Profile

**Response `200 OK`:**
```json
{
  "id": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "name": "Jane Smith",
  "email": "jane.smith@example.com",
  "phone": "+14155559876",
  "vehicleMake": "Toyota",
  "vehicleModel": "Corolla",
  "licensePlate": "ABC-1234",
  "available": true,
  "createdAt": "2026-02-25T09:00:00Z"
}
```

---

#### `PATCH /drivers/{driverId}/availability` — Toggle Driver Availability

**Request Body:**
```json
{
  "available": true
}
```

**Response `200 OK`:**
```json
{
  "id": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
  "available": true
}
```

**Errors:**
| Status | Scenario |
|--------|----------|
| 400    | Driver has an active ride — cannot go offline |
| 404    | Driver not found |

---

#### `GET /drivers/available` — List All Available Drivers

**Response `200 OK`:**
```json
[
  {
    "id": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
    "name": "Jane Smith",
    "vehicleMake": "Toyota",
    "vehicleModel": "Corolla",
    "licensePlate": "ABC-1234"
  }
]
```

---

#### `GET /drivers/{driverId}/rides` — Get Driver's Ride History

**Response `200 OK`:**
```json
[
  {
    "id": "ride-uuid-1",
    "pickupLocation": "123 Main St, Springfield",
    "dropoffLocation": "456 Oak Ave, Shelbyville",
    "distanceKm": 8.5,
    "fareAmount": 14.75,
    "status": "COMPLETED",
    "requestedAt": "2026-02-25T10:00:00Z",
    "completedAt": "2026-02-25T10:35:00Z"
  }
]
```

---

### 10.3 Ride Endpoints

---

#### `POST /rides` — Request a Ride

**Request Body:**
```json
{
  "riderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "pickupLocation": "123 Main St, Springfield",
  "dropoffLocation": "456 Oak Ave, Shelbyville",
  "distanceKm": 8.5
}
```

**Response `201 Created`:**
```json
{
  "id": "r1i2d3e4-f5a6-7890-abcd-ef1234567890",
  "rider": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "name": "John Doe"
  },
  "driver": null,
  "pickupLocation": "123 Main St, Springfield",
  "dropoffLocation": "456 Oak Ave, Shelbyville",
  "distanceKm": 8.5,
  "fareAmount": 14.75,
  "status": "REQUESTED",
  "requestedAt": "2026-02-25T10:00:00Z",
  "acceptedAt": null,
  "startedAt": null,
  "completedAt": null,
  "cancelledAt": null,
  "cancellationReason": null
}
```

**Errors:**
| Status | Scenario |
|--------|----------|
| 400    | Missing/invalid fields, distanceKm <= 0 |
| 404    | Rider not found |
| 409    | Rider already has an active ride |
| 503    | No available drivers |

---

#### `GET /rides/{rideId}` — Get Ride Details

**Response `200 OK`:** *(same structure as above)*

**Errors:**
| Status | Scenario |
|--------|----------|
| 404    | Ride not found |

---

#### `GET /rides?status=REQUESTED` — List Rides by Status

**Query Params:** `status` (optional) — filter by `RideStatus` enum value

**Response `200 OK`:**
```json
[
  {
    "id": "r1i2d3e4-f5a6-7890-abcd-ef1234567890",
    "rider": { "id": "...", "name": "John Doe" },
    "pickupLocation": "123 Main St, Springfield",
    "dropoffLocation": "456 Oak Ave, Shelbyville",
    "distanceKm": 8.5,
    "fareAmount": 14.75,
    "status": "REQUESTED",
    "requestedAt": "2026-02-25T10:00:00Z"
  }
]
```

---

#### `POST /rides/{rideId}/accept` — Driver Accepts a Ride

**Request Body:**
```json
{
  "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890"
}
```

**Response `200 OK`:**
```json
{
  "id": "r1i2d3e4-f5a6-7890-abcd-ef1234567890",
  "driver": {
    "id": "d1e2f3a4-b5c6-7890-abcd-ef1234567890",
    "name": "Jane Smith"
  },
  "status": "ACCEPTED",
  "acceptedAt": "2026-02-25T10:05:00Z"
}
```

**Errors:**
| Status | Scenario |
|--------|----------|
| 404    | Ride or Driver not found |
| 409    | Ride not in REQUESTED status |
| 409    | Driver is not available or has active ride |

---

#### `POST /rides/{rideId}/reject` — Driver Rejects a Ride

**Request Body:**
```json
{
  "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890"
}
```

**Response `200 OK`:**
```json
{
  "id": "r1i2d3e4-f5a6-7890-abcd-ef1234567890",
  "status": "REQUESTED",
  "message": "Ride rejected. Still open for other drivers."
}
```

**Errors:**
| Status | Scenario |
|--------|----------|
| 404    | Ride or Driver not found |
| 409    | Ride not in REQUESTED status |

---

#### `POST /rides/{rideId}/start` — Driver Starts the Ride

**Request Body:**
```json
{
  "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890"
}
```

**Response `200 OK`:**
```json
{
  "id": "r1i2d3e4-f5a6-7890-abcd-ef1234567890",
  "status": "IN_PROGRESS",
  "startedAt": "2026-02-25T10:10:00Z"
}
```

**Errors:**
| Status | Scenario |
|--------|----------|
| 404    | Ride or Driver not found |
| 403    | Driver is not the assigned driver for this ride |
| 409    | Ride not in ACCEPTED status |

---

#### `POST /rides/{rideId}/complete` — Driver Completes the Ride

**Request Body:**
```json
{
  "driverId": "d1e2f3a4-b5c6-7890-abcd-ef1234567890"
}
```

**Response `200 OK`:**
```json
{
  "id": "r1i2d3e4-f5a6-7890-abcd-ef1234567890",
  "status": "COMPLETED",
  "fareAmount": 14.75,
  "completedAt": "2026-02-25T10:45:00Z"
}
```

**Errors:**
| Status | Scenario |
|--------|----------|
| 404    | Ride or Driver not found |
| 403    | Driver is not the assigned driver for this ride |
| 409    | Ride not in IN_PROGRESS status |

---

#### `POST /rides/{rideId}/cancel` — Cancel a Ride

**Request Body:**
```json
{
  "cancelledBy": "RIDER",
  "actorId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "reason": "Changed my plans"
}
```

> `cancelledBy`: `"RIDER"` or `"DRIVER"`
> `actorId`: The UUID of the Rider or Driver performing the cancellation

**Response `200 OK`:**
```json
{
  "id": "r1i2d3e4-f5a6-7890-abcd-ef1234567890",
  "status": "CANCELLED",
  "cancellationReason": "Changed my plans",
  "cancelledAt": "2026-02-25T10:08:00Z"
}
```

**Errors:**
| Status | Scenario |
|--------|----------|
| 404    | Ride not found |
| 403    | Actor is not authorized to cancel this ride |
| 409    | Ride is in a non-cancellable status (IN_PROGRESS or COMPLETED) |

---

## 11. Error Handling

All errors return a consistent `problem+json` structure:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Rider already has an active ride",
  "path": "/api/v1/rides",
  "timestamp": "2026-02-25T10:00:00Z"
}
```

| HTTP Status | When to Use |
|-------------|-------------|
| 400         | Validation failure, malformed request body |
| 403         | Action not permitted for this actor |
| 404         | Entity not found |
| 409         | Business rule / state conflict |
| 503         | No available drivers |

---

## 12. Out of Scope (Phase 1)

The following will be addressed in future phases:

| Feature | Target Phase |
|---------|-------------|
| Authentication (JWT) | Phase 3 |
| Driver auto-matching by proximity | Phase 2 |
| Ratings & reviews | Phase 2 |
| Real-time GPS / WebSockets | Phase 3 |
| Payment processing | Phase 3 |
| Surge pricing | Phase 2 |
| Multiple vehicle types | Phase 2 |
| Scheduled rides | Phase 3 |
| Microservices / Kafka | Phase 4 |
| Notifications (email/SMS) | Phase 3 |

---

*End of Phase 1 SDLC Artifact — v1.0*
