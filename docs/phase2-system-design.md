# Ridesharing App — System Design
**Version:** 1.0
**SDLC Phase:** 2 — System Design
**Product Phase:** 1 — MVP
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

The application follows a strict **layered architecture** with a unidirectional dependency rule. `RideService` is the central service — it reaches into all three repositories within a single `@Transactional` boundary, which is what allows operations like `acceptRide` to atomically update both the ride and the driver.

```
  Rider Client ──────┐          Driver Client ──────┐
                     │                              │
                     ▼                              ▼
         ┌───────────────────────────────────────────────────┐
         │                  @RestControllerAdvice            │
         │               GlobalExceptionHandler              │◄──────────────┐
         │  400 Validation | 403 Forbidden | 404 Not Found   │               │
         │  409 Conflict   | 409 OptimLock | 503 No Drivers  │               │
         └────────────────────────────────────────────────────┘          exceptions
                                                                         bubble up
         ┌──────────────────────────────────────────────────────┐            │
         │                  Controller Layer                    │            │
         │    @Valid runs Bean Validation on every request      │────────────┘
         │                                                      │
         │  RiderController    DriverController   RideController│
         │  ─────────────────  ──────────────────  ────────────│
         │  POST /riders       POST /drivers        POST /rides │
         │  GET  /riders/{id}  GET  /drivers/{id}   GET  /rides │
         │  GET  /riders/{id}  PATCH .../avail.     GET  /rides/{id}
         │        /rides       GET  .../available   POST /rides/{id}/accept
         │                     GET  /drivers/{id}         ...../reject
         │                           /rides               ...../start
         │                                                ...../complete
         │                                                ...../cancel
         └────────┬──────────────────┬───────────────────┬──────┘
                  │                  │                   │
                  ▼                  ▼                   ▼
         ┌──────────────────────────────────────────────────────┐
         │                   Service Layer                      │
         │              @Transactional on all writes            │
         │                                                      │
         │  RiderService        DriverService      RideService  │
         │  ─────────────       ─────────────      ──────────── │
         │  register            register           requestRide  │
         │  getById             getById            getById      │
         │  getRideHistory      updateAvailability listByStatus │
         │                      getAvailableDrivers acceptRide  │
         │                      getRideHistory     rejectRide   │
         │                                         startRide    │
         │                                         completeRide │
         │                                         cancelRide   │
         │                                              │        │
         │                                         FareCalculator
         │                                         $2.00 + $1.50/km
         └────────┬─────────────────┬──────────────────┼────────┘
                  │                 │            ┌──────┴──────┐
                  │                 │            │  RideService │
                  │                 │            │ uses all 3  │
                  ▼                 ▼            ▼  repos      │
         ┌─────────────┐  ┌──────────────┐  ┌──────────────┐  │
         │RiderRepository  │DriverRepository  │RideRepository│◄─┘
         │─────────────│  │──────────────│  │──────────────│
         │existsByEmail│  │existsByEmail │  │findByIdWith  │
         │findById     │  │existsByPlate │  │  Actors      │
         │             │  │findAllByAvail│  │findAllByStatus
         │             │  │  ableTrue    │  │  WithActors  │
         │             │  │              │  │findAllByRider│
         │             │  │              │  │  IdWithActors│
         │             │  │              │  │existsByRider │
         │             │  │              │  │  IdAndStatus │
         └──────┬──────┘  └──────┬───────┘  └──────┬───────┘
                │                │                  │
                └────────────────┴──────────────────┘
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │        MySQL 8.x        │
                    │  ┌──────────────────┐  │
                    │  │     riders       │  │
                    │  │  id  name  email │  │
                    │  │  phone  created  │  │
                    │  └──────────────────┘  │
                    │  ┌──────────────────┐  │
                    │  │     drivers      │  │
                    │  │  id  name  email │  │
                    │  │  vehicle details │  │
                    │  │  available       │  │
                    │  └──────────────────┘  │
                    │  ┌──────────────────┐  │
                    │  │      rides       │  │
                    │  │  id  rider_id FK │  │
                    │  │  driver_id FK    │  │
                    │  │  status  fare    │  │
                    │  │  version ← lock  │  │
                    │  └──────────────────┘  │
                    │  ── Flyway manages ─── │
                    └────────────────────────┘
```

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
│     version             BIGINT     DEFAULT 0       │  ← optimistic lock
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
    version             BIGINT        NOT NULL DEFAULT 0,
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

**`version` column:** Used by Hibernate's `@Version` optimistic locking. Starts at `0`, incremented by Hibernate on every `UPDATE`. Prevents concurrent acceptance of the same ride — see §5.9.

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
│                                     existsByRiderIdAndStatusIn, existsByDriverIdAndStatusIn,
│                                     findAllByStatusWithActors (JOIN FETCH — prevents N+1)
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

### 5.9 Optimistic Locking on `Ride` — `@Version`
Concurrent ride acceptance is a real race condition: two drivers can both read a ride as `REQUESTED`, pass the status check, and both successfully write `ACCEPTED` — the second save silently overwrites the first.

Fix: Hibernate's `@Version` mechanism. The `Ride` entity carries a `version BIGINT` field. On every `UPDATE`, Hibernate adds `WHERE id = ? AND version = ?` and increments `version`. If the row has already been updated by another transaction, the `WHERE` clause matches zero rows → Hibernate throws `ObjectOptimisticLockingFailureException` → `GlobalExceptionHandler` maps this to `409 Conflict`.

No external locking infrastructure required — one field, one exception handler.

### 5.10 N+1 Prevention — `JOIN FETCH` on List Queries
`RideService.toResponse()` accesses `ride.getRider()` and `ride.getDriver()`, which are lazily loaded by default. For single-ride lookups this is fine. For list queries (`listByStatus`, `getRideHistory`), each item would trigger up to 2 additional `SELECT` statements — an N+1 problem.

Fix: a `@Query` with `LEFT JOIN FETCH` on the `RideRepository` methods used by list endpoints. Rider and Driver data is loaded in a single query alongside the ride. The standard `findById` keeps lazy loading (no overhead for single-entity lookups).

### 5.11 Driver Availability Guard (both directions)
`PATCH /drivers/{driverId}/availability` guards both transitions:
- Going **offline** (`available=false`): blocked if driver has a ride in `ACCEPTED` or `IN_PROGRESS` → `400`
- Going **online** (`available=true`): blocked if driver already has an active ride → `400`

The second guard prevents a driver from manually marking themselves available while mid-ride, which would allow them to be double-booked.

### 5.12 `rejectRide` Returns Standard `RideResponse`
The original API contract spec included a `message` field in the reject response. This is dropped — the standard `RideResponse` with `status: REQUESTED` is self-explanatory, and adding a `message` field only to `RideResponse` for one edge case would pollute the shared DTO.

### 5.13 `GET /rides` with No Status Filter Returns All Rides
`GET /rides?status=REQUESTED` filters by status. If the `status` query param is omitted, all rides are returned. This acts as a simple admin-level view. Since Phase 1 has no authentication, restricting this endpoint is deferred to Phase 3 (JWT + roles).

---

## 6. Request / Response Flow

### 6.1 Rider Requests a Ride — `POST /api/v1/rides`

```
Rider Client           RideController          RideService              MySQL
     │                       │                      │                     │
     │── POST /rides ────────►│                      │                     │
     │   {riderId,            │                      │                     │
     │    pickup,             ├─ @Valid ─────────────────────────────────► │
     │    dropoff,            │  • riderId: not null │                     │
     │    distanceKm}         │  • pickup: not blank │                     │
     │                        │  • distanceKm: > 0   │                     │
     │◄── 400 Bad Request ────│  (fails fast before  │                     │
     │    field: msg; ...     │   reaching service)  │                     │
     │                        │                      │                     │
     │                        │── requestRide() ─────►│                     │
     │                        │                      │── findById(riderId) ►│
     │                        │                      │◄─ Rider entity ─────│
     │◄── 404 Not Found ──────│◄── ResourceNotFound──│   (or null → throw) │
     │    "Rider not found"   │                      │                     │
     │                        │                      │                     │
     │                        │                      │── existsByRiderIdAnd►│
     │                        │                      │   StatusIn(          │
     │                        │                      │   [REQUESTED,        │
     │                        │                      │    ACCEPTED,         │
     │                        │                      │    IN_PROGRESS])     │
     │◄── 409 Conflict ───────│◄── ConflictException─│◄─ true              │
     │    "Rider already has  │                      │                     │
     │     an active ride"    │                      │                     │
     │                        │                      │                     │
     │                        │                      │── findAllByAvailable►│
     │                        │                      │   True()            │
     │◄── 503 Unavailable ────│◄── NoDriverAvailable─│◄─ [] empty          │
     │    "No available       │                      │                     │
     │     drivers..."        │                      │                     │
     │                        │                      │                     │
     │                        │                      │  fareCalculator      │
     │                        │                      │  .calculate(distKm)  │
     │                        │                      │  = $2.00+dist×$1.50  │
     │                        │                      │                     │
     │                        │                      │── save(new Ride) ───►│
     │                        │                      │   status=REQUESTED   │
     │                        │                      │   version=0          │
     │                        │                      │◄─ saved Ride ───────│
     │◄── 201 Created ────────│◄─ RideResponse ──────│                     │
     │    {id, rider{},       │                      │                     │
     │     driver: null,      │                      │                     │
     │     status: REQUESTED, │                      │                     │
     │     fareAmount: X.XX}  │                      │                     │
```

### 6.2 Driver Accepts a Ride — `POST /api/v1/rides/{rideId}/accept`

This flow demonstrates two key design decisions working together: the `@Transactional` boundary that atomically updates both the ride and the driver, and `@Version` optimistic locking that prevents two drivers from simultaneously accepting the same ride.

```
Driver Client          RideController          RideService              MySQL
     │                       │                      │                     │
     │── POST /accept ───────►│                      │                     │
     │   {driverId}           ├─ @Valid ─────────────────────────────────► │
     │                        │  • driverId: not null│                     │
     │◄── 400 Bad Request ────│                      │                     │
     │                        │                      │                     │
     │                        │── acceptRide() ──────►│                     │
     │                        │                      │── findByIdWithActors►│
     │                        │                      │   (JOIN FETCH rider  │
     │                        │                      │    + driver, 1 SQL)  │
     │◄── 404 Not Found ──────│◄── ResourceNotFound ─│◄─ null              │
     │                        │                      │                     │
     │                        │                      │  ride.status        │
     │◄── 409 Conflict ───────│◄── ConflictException─│  != REQUESTED       │
     │    "Ride not in        │                      │                     │
     │     REQUESTED status"  │                      │                     │
     │                        │                      │                     │
     │                        │                      │── findById(driverId)►│
     │◄── 404 Not Found ──────│◄── ResourceNotFound ─│◄─ null              │
     │                        │                      │                     │
     │                        │                      │  driver.available   │
     │◄── 409 Conflict ───────│◄── ConflictException─│  == false OR        │
     │    "Driver not avail." │                      │  has active ride    │
     │                        │                      │                     │
     │                        │              ┌───────┴──────────┐          │
     │                        │              │  @Transactional  │          │
     │                        │              │  ─────────────── │          │
     │                        │              │  ride.status     │          │
     │                        │              │    = ACCEPTED    │          │
     │                        │              │  ride.driver     │          │
     │                        │              │    = driver      │          │
     │                        │              │  ride.acceptedAt │          │
     │                        │              │    = now()       │          │
     │                        │              │  driver.available│          │
     │                        │              │    = false       │          │
     │                        │              └───────┬──────────┘          │
     │                        │                      │── UPDATE rides ─────►│
     │                        │                      │   SET status=ACCEPTED│
     │                        │                      │   WHERE id=?         │
     │                        │                      │   AND version=N      │
     │                        │                      │                  ◄───── version
     │◄── 409 Conflict ───────│◄── OptimisticLock ───│◄─ 0 rows updated    │  mismatch
     │    "Ride was just      │    FailureException  │   (another driver   │  (race
     │     updated. Retry."   │                      │    got there first) │  condition)
     │                        │                      │                     │
     │                        │                      │◄─ 1 row updated ────│  happy path
     │                        │                      │   version = N+1     │
     │                        │                      │── UPDATE drivers ───►│
     │                        │                      │   SET available=false│
     │◄── 200 OK ─────────────│◄─ RideResponse ──────│◄─ committed ────────│
     │    {status: ACCEPTED,  │                      │                     │
     │     driver: {...},     │                      │                     │
     │     acceptedAt: ...}   │                      │                     │
```

**Two things these flows make explicit:**
- `@Valid` is a silent first gate — bad requests are rejected before `RideService` is ever called, so service methods can trust their inputs are structurally valid
- `RideService` owns the transaction boundary — when `acceptRide` updates both ride and driver, both writes happen inside one `@Transactional` call; if either fails, both roll back

---

*End of Phase 2 System Design — v1.0*
