package com.ridesharing.repository;

import com.ridesharing.entity.Ride;
import com.ridesharing.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RideRepository extends JpaRepository<Ride, UUID> {

    // ── Single-entity lookups (lazy loading is fine) ──────────

    // Overrides the default findById to eagerly fetch associations,
    // preventing LazyInitializationException when mapping outside a transaction.
    @Query("SELECT r FROM Ride r LEFT JOIN FETCH r.rider LEFT JOIN FETCH r.driver WHERE r.id = :id")
    Optional<Ride> findByIdWithActors(@Param("id") UUID id);

    // ── List queries (JOIN FETCH prevents N+1) ────────────────

    @Query("SELECT r FROM Ride r LEFT JOIN FETCH r.rider LEFT JOIN FETCH r.driver WHERE r.status = :status")
    List<Ride> findAllByStatusWithActors(@Param("status") RideStatus status);

    @Query("SELECT r FROM Ride r LEFT JOIN FETCH r.rider LEFT JOIN FETCH r.driver")
    List<Ride> findAllWithActors();

    @Query("SELECT r FROM Ride r LEFT JOIN FETCH r.rider LEFT JOIN FETCH r.driver WHERE r.rider.id = :riderId")
    List<Ride> findAllByRiderIdWithActors(@Param("riderId") UUID riderId);

    @Query("SELECT r FROM Ride r LEFT JOIN FETCH r.rider LEFT JOIN FETCH r.driver WHERE r.driver.id = :driverId")
    List<Ride> findAllByDriverIdWithActors(@Param("driverId") UUID driverId);

    // ── Existence checks (no fetch needed) ───────────────────

    boolean existsByRiderIdAndStatusIn(UUID riderId, List<RideStatus> statuses);

    boolean existsByDriverIdAndStatusIn(UUID driverId, List<RideStatus> statuses);
}
