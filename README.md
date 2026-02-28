# Ridesharing App

Phase 1 MVP — REST API for a ridesharing platform built with Java 21 and Spring Boot 3.3.5.

## Prerequisites

| Tool | Version | Install |
|---|---|---|
| Java (OpenJDK) | 21 | `brew install openjdk@21` |
| Maven | 3.9+ | `brew install maven` |
| MySQL | 8.x | `brew install mysql` |

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

```bash
./run-tests.sh spring-boot:run
```

The server starts on **http://localhost:8080**.

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger UI (interactive API docs) |
| `http://localhost:8080/api-docs` | Raw OpenAPI JSON |

## Running Tests

Unit tests use Mockito + AssertJ — **no database or running server required**.

```bash
# All tests
./run-tests.sh test

# Specific test class
./run-tests.sh test -Dtest=RideServiceTest

# Multiple classes
./run-tests.sh test -Dtest=RideServiceTest,FareCalculatorTest

# Skip tests (e.g. for a quick build)
./run-tests.sh package -DskipTests
```

`run-tests.sh` is a thin wrapper that sets `JAVA_HOME` and `PATH` before delegating to `mvn`. All standard Maven flags work as-is.

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
    ├── service/             # RideServiceTest, ...
    └── util/                # FareCalculatorTest
docs/
├── phase1-requirements.md
└── phase2-system-design.md
```

## API Overview

| Method | Path | Description |
|---|---|---|
| `POST` | `/riders` | Register a rider |
| `GET` | `/riders/{id}` | Get rider profile |
| `GET` | `/riders/{id}/rides` | Rider ride history |
| `POST` | `/drivers` | Register a driver |
| `GET` | `/drivers/{id}` | Get driver profile |
| `PATCH` | `/drivers/{id}/availability` | Toggle driver availability |
| `GET` | `/drivers/available` | List available drivers |
| `GET` | `/drivers/{id}/rides` | Driver ride history |
| `POST` | `/rides` | Request a ride |
| `GET` | `/rides` | List rides (optional `?status=` filter) |
| `GET` | `/rides/{id}` | Get ride details |
| `POST` | `/rides/{id}/accept` | Driver accepts a ride |
| `POST` | `/rides/{id}/reject` | Driver rejects a ride |
| `POST` | `/rides/{id}/start` | Driver starts a ride |
| `POST` | `/rides/{id}/complete` | Driver completes a ride |
| `POST` | `/rides/{id}/cancel` | Rider or driver cancels a ride |

Full request/response shapes are in the Swagger UI or `docs/phase2-system-design.md`.

## Git Workflow

- `main` is protected — direct pushes are blocked
- All changes go through a feature branch → PR → review → merge
- Branch naming: `feature/<topic>`, `fix/<topic>`, `review/<topic>`
