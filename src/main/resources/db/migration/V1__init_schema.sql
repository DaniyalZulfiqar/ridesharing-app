-- ============================================================
-- V1__init_schema.sql
-- Initial schema for Ridesharing App — Phase 1 MVP
-- ============================================================

CREATE TABLE IF NOT EXISTS riders (
    id           CHAR(36)     NOT NULL,
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    phone        VARCHAR(15)  NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_riders_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------

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

-- -------------------------------------------------------------

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
