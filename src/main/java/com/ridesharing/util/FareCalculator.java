package com.ridesharing.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates ride fare.
 *
 * Formula: BASE_FARE + (distanceKm × PER_KM_RATE)
 * Example: 5 km → $2.00 + (5 × $1.50) = $9.50
 */
@Component
public class FareCalculator {

    static final BigDecimal BASE_FARE    = new BigDecimal("2.00");
    static final BigDecimal PER_KM_RATE  = new BigDecimal("1.50");

    public BigDecimal calculate(Double distanceKm) {
        BigDecimal distance = BigDecimal.valueOf(distanceKm);
        return BASE_FARE
                .add(distance.multiply(PER_KM_RATE))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
