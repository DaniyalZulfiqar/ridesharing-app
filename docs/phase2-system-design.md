# Ridesharing App — Phase 2: System Design
**Version:** 1.0
**Phase:** MVP
**Date:** 2026-02-25
**Status:** Draft

---

## Table of Contents
1. [Architecture Overview](#1-architecture-overview)
2. [Entity-Relationship Diagram](#2-entity-relationship-diagram)
3. [Database Schema (DDL)](#3-database-schema-ddl)
4. [Package & Class Structure](#4-package--class-structure)
5. [Key Design Decisions](#5-key-design-decisions)
6. [Request / Response Flow](#6-request--response-flow)

---

## 1. Architecture Overview

The application follows a strict **layered architecture** with a unidirectional dependency rule:

```
HTTP Client
    │
    ▼
┌─────────────────────────────────────────────────────┐
│  Controller Layer  (@RestController)                │
│  • Validates HTTP input                             │
│  • Delegates to service                             │
│  • Returns HTTP response                            │
└─────────────────────────┬───────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────┐
│  Service Layer  (@Service)                          │
│  • Owns all business logic                          │
│  • Enforces state machine transitions               │
│  • Maps entities ↔ DTOs                             │
│  • Throws typed exceptions on rule violations       │
└─────────────────────────┬───────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────┐
│  Repository Layer  (JpaRepository)                  │
│  • CRUD + custom query methods                      │
│  • Zero business logic                              │
└─────────────────────────┬───────────────────────────┘
                          │
                          ▼
                     MySQL 8.x
```

**Cross-cutting concerns:**
- `GlobalExceptionHandler` — maps exceptions to `problem+json` responses
- `FareCalculator` — pure utility, injected into `RideService`
- Flyway — manages schema versioning

---

## 2. Entity-Relationship Diagram

```
┌────────────────────────────────┐
│             riders             │
├────────────────────────────────┤
│ PK  id            CHAR(36)     │
│     name          VARCHAR(100) │
│ UQ  email         VARCHAR(255) │
│     phone         VARCHAR(15)  │
│     created_at    DATETIME     │
└──────────────────┬─────────────┘
                   │ 1
                   │
                   │ (rider_id FK)
                   │ N
┌──────────────────▼─────────────────────────────────┐
│                       rides                        │
├────────────────────────────────────────────────────┤
│ PK  id                  CHAR(36)                   │
│ FK  rider_id            CHAR(36)   NOT NULL        │
│ FK  driver_id           CHAR(36)   NULL            │
│     pickup_location     VARCHAR(500)               │
│     dropoff_location    VARCHAR(500)               │
│     distance_km         DOUBLE                     │
│     fare_amount         DECIMAL(10,2)              │
│     status              VARCHAR(20)                │
│     cancellation_reason VARCHAR(500)               │
│     requested_at        DATETIME                   │
│     accepted_at         DATETIME   NULL            │
│     started_at          DATETIME   NULL            │
│     completed_at        DATETIME   NULL            │
│     cancelled_at        DATETIME   NULL            │
└────────────────────────────────────────────────────┘
                   │ N
                   │ (driver_id FK)
                   │
                   │ 1
┌──────────────────▼─────────────────────────────────┐
│                      drivers                       │
├────────────────────────────────────────────────────┤
│ PK  id             CHAR(36)                        │
│     name           VARCHAR(100)                    │
│ UQ  email          VARCHAR(255)                    │
│     phone          VARCHAR(15)                     │
│     vehicle_make   VARCHAR(100)                    │
│     vehicle_model  VARCHAR(100)                    │
│ UQ  license_plate  VARCHAR(20)                     │
│     available      TINYINT(1)  DEFAULT 0           │
│     created_at     DATETIME                        │
└────────────────────────────────────────────────────┘
```

**Cardinality:**
- One `Rider` → many `Rides` (a rider accumulates ride history)
- One `Driver` → many `Rides` (a driver accumulates ride history)
- A `Ride` has exactly one `Rider` and at most one `Driver` (assigned on acceptance)

**Constraint enforcement:**
- Business rules (one active ride per actor) are enforced in the **Service layer**, not via DB constraints — this keeps the DB schema clean and avoids obscure constraint violation errors surfacing to the API

---

## 3. Database Schema (DDL)

> Managed via Flyway. Migration file: `src/main/resources/db/migration/V1__init_schema.sql`

```sql
CREATE TABLE IF NOT EXISTS riders (
    id           CHAR(36)     NOT NULL,
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    phone        VARCHAR(15)  NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_riders_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS drivers (
    id            CHAR(36)     NOT NULL,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    phone         VARCHAR(15)  NOT NULL,
    vehicle_make  VARCHAR(100) NOT NULL,
    vehicle_model VARCHAR(100) NOT NULL,
    license_plate VARCHAR(20)  NOT NULL,
    available     TINYINT(1)   NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_drivers_email (email),
    UNIQUE KEY uk_drivers_license_plate (license_plate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rides (
    id                  CHAR(36)      NOT NULL,
    rider_id            CHAR(36)      NOT NULL,
    driver_id           CHAR(36)          NULL,
    pickup_location     VARCHAR(500)  NOT NULL,
    dropoff_location    VARCHAR(500)  NOT NULL,
    distance_km         DOUBLE        NOT NULL,
    fare_amount         DECIMAL(10,2) NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'REQUESTED',
    cancellation_reason VARCHAR(500)      NULL,
    requested_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at         DATETIME          NULL,
    started_at          DATETIME          NULL,
    completed_at        DATETIME          NULL,
    cancelled_at        DATETIME          NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rides_rider  FOREIGN KEY (rider_id)  REFERENCES riders(id),
    CONSTRAINT fk_rides_driver FOREIGN KEY (driver_id) REFERENCES drivers(id),
    INDEX idx_rides_status    (status),
    INDEX idx_rides_rider_id  (rider_id),
    INDEX idx_rides_driver_id (driver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Index rationale:**
| Index | Query it supports |
|-------|-------------------|
| `idx_rides_status` | `GET /rides?status=REQUESTED` (drivers browsing open rides) |
| `idx_rides_rider_id` | `GET /riders/{id}/rides` (rider history) |
| `idx_rides_driver_id` | `GET /drivers/{id}/rides` (driver history) |

---

## 4. Package & Class Structure

```
com.ridesharing
│
├── RidesharingApplication.java
│
├── controller/
│   ├── RiderController.java          POST /riders, GET /riders/{id}, GET /riders/{id}/rides
│   ├── DriverController.java         POST /drivers, GET /drivers/{id}, PATCH /drivers/{id}/availability,
│   │                                 GET /drivers/available, GET /drivers/{id}/rides
│   └── RideController.java           POST /rides, GET /rides/{id}, GET /rides,
│                                     POST /rides/{id}/accept|reject|start|complete|cancel
│
├── service/
│   ├── RiderService.java             register, getById, getRideHistory
│   ├── DriverService.java            register, getById, updateAvailability, getAvailable, getRideHistory
│   └── RideService.java              requestRide, getById, listByStatus, accept, reject,
│                                     start, complete, cancel
│
├── repository/
│   ├── RiderRepository.java          existsByEmail, findByEmail
│   ├── DriverRepository.java         existsByEmail, existsByLicensePlate, findAllByAvailableTrue
│   └── RideRepository.java           findAllByStatus, findAllByRiderId, findAllByDriverId,
│                                     existsByRiderIdAndStatusIn, existsByDriverIdAndStatusIn
│
├── entity/
│   ├── Rider.java
│   ├── Driver.java
│   └── Ride.java
│
├── dto/
│   ├── request/
│   │   ├── RegisterRiderRequest.java
│   │   ├── RegisterDriverRequest.java
│   │   ├── RequestRideRequest.java
│   │   ├── DriverActionRequest.java    (accept / reject / start / complete)
│   │   ├── UpdateAvailabilityRequest.java
│   │   └── CancelRideRequest.java
│   └── response/
│       ├── RiderResponse.java          full rider payload
│       ├── RiderSummaryResponse.java   embedded in RideResponse
│       ├── DriverResponse.java         full driver payload
│       ├── DriverSummaryResponse.java  embedded in RideResponse + available list
│       ├── RideResponse.java           full ride payload
│       └── ErrorResponse.java          problem+json
│
├── enums/
│   └── RideStatus.java               REQUESTED | ACCEPTED | IN_PROGRESS | COMPLETED | CANCELLED
│
├── exception/
│   ├── ResourceNotFoundException.java → 404
│   ├── ConflictException.java         → 409
│   ├── ForbiddenException.java        → 403
│   ├── NoDriverAvailableException.java → 503
│   └── GlobalExceptionHandler.java    (@RestControllerAdvice)
│
└── util/
    └── FareCalculator.java            BASE_FARE=$2.00, PER_KM_RATE=$1.50
```

---

## 5. Key Design Decisions

### 5.1 UUID Storage — `CHAR(36)` over `BINARY(16)`
`CHAR(36)` stores the standard hyphenated UUID string (e.g., `a1b2c3d4-e5f6-7890-abcd-ef1234567890`). While `BINARY(16)` is ~58% more space-efficient, `CHAR(36)` is chosen for Phase 1 because:
- Directly readable in `SELECT` output — accelerates debugging
- No need for `UUID_TO_BIN` / `BIN_TO_UUID` wrappers in queries
- Negligible performance difference at Phase 1 scale
- Can be migrated to `BINARY(16)` in a later phase if needed

### 5.2 UUID Generation — Hibernate 6 `@UuidGenerator`
Spring Boot 3 ships with Hibernate 6, which natively supports `@UuidGenerator`. UUID is generated by the application (not the DB), ensuring IDs are available before the `INSERT` — important for response bodies and event logs.

### 5.3 Flyway over `ddl-auto=create`
`spring.jpa.hibernate.ddl-auto=validate` is set. Flyway owns schema evolution. This enforces:
- Reproducible schema across environments
- Controlled, auditable migrations
- Safe production deployments in future phases

### 5.4 `LocalDateTime` over `Instant` / `ZonedDateTime`
Timestamps are stored as `DATETIME` in MySQL and `LocalDateTime` in Java. The DB timezone is forced to UTC via the JDBC URL (`serverTimezone=UTC`). ISO-8601 serialization is configured globally — API consumers always receive UTC timestamps.

### 5.5 Business Rule Enforcement in Service Layer
The constraint "one active ride per actor" is **not** a DB-level constraint. It is enforced in `RideService` with a `SELECT EXISTS(...)` query before creating a ride. Rationale:
- DB constraints produce generic `DataIntegrityViolationException` — hard to map to meaningful API errors
- Service-layer enforcement produces clear, typed exceptions (`ConflictException`) that map directly to the problem+json error contract

### 5.6 `BigDecimal` for Fare
`fareAmount` is stored as `DECIMAL(10,2)` in MySQL and `BigDecimal` in Java with `RoundingMode.HALF_UP`. This eliminates floating-point drift in monetary calculations.

### 5.7 State Transitions are Explicit Checks
`RideService` validates the current status before every transition. Invalid transitions throw `ConflictException(409)`. This mirrors the state machine in the requirements exactly.

### 5.8 No MapStruct (Phase 1)
Entity ↔ DTO mapping is done manually in service methods. MapStruct would be appropriate for Phase 2+ when the model grows. Keeping it manual here reduces build complexity and makes the data flow transparent for learning and review.

---

## 6. Request / Response Flow

**Example: Rider requests a ride**

```
POST /api/v1/rides
        │
        ▼
RideController.requestRide(@RequestBody @Valid RequestRideRequest)
        │
        ├─ Validates request body (Bean Validation)
        │
        ▼
RideService.requestRide(request)
        │
        ├─ riderRepository.findById(riderId)       → 404 if not found
        ├─ rideRepository.existsByRiderIdAndStatusIn([REQUESTED,ACCEPTED,IN_PROGRESS])
        │                                           → 409 if active ride exists
        ├─ driverRepository.findAllByAvailableTrue()
        │                                           → 503 if empty
        ├─ fareCalculator.calculate(distanceKm)
        ├─ ride = new Ride(...) with status=REQUESTED
        ├─ rideRepository.save(ride)
        └─ return RideResponse (mapped from saved entity)
        │
        ▼
RideController returns ResponseEntity<RideResponse> 201 Created
```

---

*End of Phase 2 System Design — v1.0*
