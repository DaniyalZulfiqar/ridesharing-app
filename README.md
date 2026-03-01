# Ridesharing App

Phase 1 MVP — REST API for a ridesharing platform built with Java 21 and Spring Boot 3.3.5.

## Prerequisites

| Tool | Version | Install |
|---|---|---|
| Java (OpenJDK) | 21 | `brew install openjdk@21` |
| Maven | 3.9+ | `brew install maven` |
| MySQL | 8.x | `brew install mysql` |
| Docker Desktop | latest | [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) — required for integration tests only |

After installing OpenJDK 21, set it as the active JDK for this project:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
```

To make it permanent, add both lines to your `~/.zshrc`.

## Database Setup

```sql
CREATE DATABASE ridesharing_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

The schema is managed by **Flyway** — migrations in `src/main/resources/db/migration/` run automatically on startup. Hibernate is set to `ddl-auto: validate` (it never modifies the schema).

Default JDBC config (`application.yml`):

```
url:      jdbc:mysql://localhost:3306/ridesharing_db
username: root
password: password
```

Override any of these at runtime with environment variables:

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://... \
SPRING_DATASOURCE_USERNAME=myuser \
SPRING_DATASOURCE_PASSWORD=mysecret \
./run-tests.sh spring-boot:run
```

## Running the Application

Make sure MySQL is running and `ridesharing_db` exists, then:

```bash
./run-tests.sh spring-boot:run
```

The server starts on **http://localhost:8080**.

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger UI (interactive API docs) |
| `http://localhost:8080/api-docs` | Raw OpenAPI JSON |

## Running Tests

### Unit Tests

Unit tests use Mockito + AssertJ — **no database or running server required**.

```bash
# All unit tests
./run-tests.sh test

# Specific test class
./run-tests.sh test -Dtest=RideServiceTest

# Multiple classes
./run-tests.sh test -Dtest=RideServiceTest,FareCalculatorTest

# Skip tests (e.g. for a quick build)
./run-tests.sh package -DskipTests
```

### Integration Tests

Integration tests spin up a real MySQL 8 container via **Testcontainers** and exercise the full HTTP → Service → DB stack.

**Prerequisite:** Docker Desktop must be running before you execute the command below.

```bash
# Run unit tests + integration tests
./run-tests.sh verify

# Run a specific IT class only
./run-tests.sh verify -Dit.test=RideControllerIT

# Run multiple IT classes
./run-tests.sh verify -Dit.test=RiderControllerIT+DriverControllerIT
```

What happens under the hood:
1. Surefire runs the 59 unit tests (`*Test.java`) during the `test` phase.
2. A single `mysql:8.0` Docker container is started (shared across all IT classes).
3. Flyway applies `V1__init_schema.sql` to the containerised database.
4. Failsafe runs the 39 integration tests (`*IT.java`) during the `integration-test` phase.
5. The container is torn down automatically when the JVM exits.

Current test count: **98 tests, 0 failures** (59 unit + 39 integration).

`run-tests.sh` is a thin wrapper that sets `JAVA_HOME` and `PATH` before delegating to `mvn`. All standard Maven flags work as-is.

## Manual Validation Walkthrough

The full lifecycle involves two actors — a **Rider** and a **Driver** — and a **Ride** that moves through a state machine: `REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED` (or `CANCELLED`).

All examples use `curl`. Copy the `id` values from each response and substitute them in subsequent commands.

### Step 1 — Register a Rider

```bash
curl -s -X POST http://localhost:8080/api/v1/riders \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Alice",
    "email": "alice@example.com",
    "phone": "555-0100"
  }' | jq .
```

Save the returned `id` as `RIDER_ID`.

### Step 2 — Register a Driver

```bash
curl -s -X POST http://localhost:8080/api/v1/drivers \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Bob",
    "email": "bob@example.com",
    "phone": "555-0200",
    "vehicleMake": "Toyota",
    "vehicleModel": "Camry",
    "licensePlate": "XYZ-1234"
  }' | jq .
```

Save the returned `id` as `DRIVER_ID`. Drivers are created with `available: false` by default.

### Step 3 — Driver Goes Online

```bash
curl -s -X PATCH http://localhost:8080/api/v1/drivers/$DRIVER_ID/availability \
  -H 'Content-Type: application/json' \
  -d '{"available": true}' | jq .
```

### Step 4 — Confirm Driver is Available

```bash
curl -s http://localhost:8080/api/v1/drivers/available | jq .
```

The driver should appear in the list.

### Step 5 — Rider Requests a Ride

```bash
curl -s -X POST http://localhost:8080/api/v1/rides \
  -H 'Content-Type: application/json' \
  -d "{
    \"riderId\": \"$RIDER_ID\",
    \"pickupLocation\": \"123 Main St\",
    \"dropoffLocation\": \"456 Oak Ave\",
    \"distanceKm\": 8.5
  }" | jq .
```

Save the returned `id` as `RIDE_ID`. The fare is calculated automatically: `$2.00 base + $1.50 × distanceKm`.

Requesting a ride requires at least one available driver — the service returns **503** otherwise.

### Step 6 — Driver Accepts the Ride

```bash
curl -s -X POST http://localhost:8080/api/v1/rides/$RIDE_ID/accept \
  -H 'Content-Type: application/json' \
  -d "{\"driverId\": \"$DRIVER_ID\"}" | jq .
```

Status changes to `ACCEPTED`. The driver is marked unavailable.

### Step 7 — Driver Starts the Ride

```bash
curl -s -X POST http://localhost:8080/api/v1/rides/$RIDE_ID/start \
  -H 'Content-Type: application/json' \
  -d "{\"driverId\": \"$DRIVER_ID\"}" | jq .
```

Status changes to `IN_PROGRESS`.

### Step 8 — Driver Completes the Ride

```bash
curl -s -X POST http://localhost:8080/api/v1/rides/$RIDE_ID/complete \
  -H 'Content-Type: application/json' \
  -d "{\"driverId\": \"$DRIVER_ID\"}" | jq .
```

Status changes to `COMPLETED`. The driver is marked available again.

### Step 9 — Check Ride History

```bash
# Rider's history
curl -s http://localhost:8080/api/v1/riders/$RIDER_ID/rides | jq .

# Driver's history
curl -s http://localhost:8080/api/v1/drivers/$DRIVER_ID/rides | jq .

# All rides, or filter by status
curl -s http://localhost:8080/api/v1/rides | jq .
curl -s "http://localhost:8080/api/v1/rides?status=COMPLETED" | jq .
```

---

### Alternative Flows

#### Driver Rejects a Ride

A driver can reject a `REQUESTED` ride without claiming it. The ride stays `REQUESTED` so another driver can accept.

```bash
curl -s -X POST http://localhost:8080/api/v1/rides/$RIDE_ID/reject \
  -H 'Content-Type: application/json' \
  -d "{\"driverId\": \"$DRIVER_ID\"}" | jq .
```

#### Rider Cancels a Ride

A rider can cancel a ride that is `REQUESTED` or `ACCEPTED`.

```bash
curl -s -X POST http://localhost:8080/api/v1/rides/$RIDE_ID/cancel \
  -H 'Content-Type: application/json' \
  -d "{
    \"cancelledBy\": \"RIDER\",
    \"actorId\": \"$RIDER_ID\",
    \"reason\": \"Changed my plans\"
  }" | jq .
```

#### Driver Cancels an Accepted Ride

A driver can cancel a ride they have already accepted (not one they haven't accepted yet).

```bash
curl -s -X POST http://localhost:8080/api/v1/rides/$RIDE_ID/cancel \
  -H 'Content-Type: application/json' \
  -d "{
    \"cancelledBy\": \"DRIVER\",
    \"actorId\": \"$DRIVER_ID\",
    \"reason\": \"Vehicle breakdown\"
  }" | jq .
```

When a driver cancels, they are automatically returned to `available: true`.

---

### Error Responses

All errors follow a consistent JSON shape:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Rider already has an active ride."
}
```

| HTTP Status | When it occurs |
|---|---|
| `400` | Missing or invalid request fields |
| `403` | Actor is not authorised for this action (e.g. wrong driver ID) |
| `404` | Rider, driver, or ride not found |
| `409` | Business rule conflict (duplicate email, wrong state, actor already busy) |
| `503` | No available drivers when requesting a ride |

## Building

```bash
# Compile and package (produces target/ridesharing-app-0.0.1-SNAPSHOT.jar)
./run-tests.sh package

# Run the packaged jar directly
java -jar target/ridesharing-app-0.0.1-SNAPSHOT.jar
```

## Project Structure

```
src/
├── main/java/com/ridesharing/
│   ├── controller/          # REST endpoints (RiderController, DriverController, RideController)
│   ├── service/             # Business logic (RiderService, DriverService, RideService)
│   ├── repository/          # Spring Data JPA repositories
│   ├── entity/              # JPA entities (Rider, Driver, Ride)
│   ├── dto/
│   │   ├── request/         # Validated request bodies
│   │   └── response/        # API response shapes
│   ├── enums/               # RideStatus
│   ├── exception/           # Typed exceptions + GlobalExceptionHandler
│   └── util/                # FareCalculator
├── main/resources/
│   ├── application.yml
│   └── db/migration/        # Flyway SQL migrations (V1__init_schema.sql, ...)
└── test/java/com/ridesharing/
    ├── controller/          # RiderControllerIT, DriverControllerIT, RideControllerIT
    ├── service/             # RideServiceTest, RiderServiceTest, DriverServiceTest
    └── util/                # FareCalculatorTest
docs/
├── phase1-requirements.md
└── phase2-system-design.md
```

## API Overview

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/riders` | Register a rider |
| `GET` | `/api/v1/riders/{id}` | Get rider profile |
| `GET` | `/api/v1/riders/{id}/rides` | Rider ride history |
| `POST` | `/api/v1/drivers` | Register a driver |
| `GET` | `/api/v1/drivers/{id}` | Get driver profile |
| `PATCH` | `/api/v1/drivers/{id}/availability` | Toggle driver availability |
| `GET` | `/api/v1/drivers/available` | List available drivers |
| `GET` | `/api/v1/drivers/{id}/rides` | Driver ride history |
| `POST` | `/api/v1/rides` | Request a ride |
| `GET` | `/api/v1/rides` | List rides (optional `?status=` filter) |
| `GET` | `/api/v1/rides/{id}` | Get ride details |
| `POST` | `/api/v1/rides/{id}/accept` | Driver accepts a ride |
| `POST` | `/api/v1/rides/{id}/reject` | Driver rejects a ride |
| `POST` | `/api/v1/rides/{id}/start` | Driver starts a ride |
| `POST` | `/api/v1/rides/{id}/complete` | Driver completes a ride |
| `POST` | `/api/v1/rides/{id}/cancel` | Rider or driver cancels a ride |

Full request/response shapes are in the Swagger UI or `docs/phase2-system-design.md`.

## Git Workflow

- `main` is protected — direct pushes are blocked
- All changes go through a feature branch → PR → review → merge
- Branch naming: `feature/<topic>`, `fix/<topic>`, `review/<topic>`
