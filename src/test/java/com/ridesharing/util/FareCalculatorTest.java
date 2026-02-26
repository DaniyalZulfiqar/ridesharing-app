package com.ridesharing.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FareCalculatorTest {

    private FareCalculator fareCalculator;

    @BeforeEach
    void setUp() {
        fareCalculator = new FareCalculator();
    }

    @Test
    void calculate_5km_returns9_50() {
        assertThat(fareCalculator.calculate(5.0))
                .isEqualByComparingTo(new BigDecimal("9.50"));
    }

    @Test
    void calculate_10km_returns17_00() {
        assertThat(fareCalculator.calculate(10.0))
                .isEqualByComparingTo(new BigDecimal("17.00"));
    }

    @Test
    void calculate_1km_returns3_50() {
        assertThat(fareCalculator.calculate(1.0))
                .isEqualByComparingTo(new BigDecimal("3.50"));
    }

    @Test
    void calculate_usesBaseFareAndPerKmRate() {
        // Verifies constants, not just examples
        BigDecimal expected = FareCalculator.BASE_FARE
                .add(BigDecimal.valueOf(7.5).multiply(FareCalculator.PER_KM_RATE));
        assertThat(fareCalculator.calculate(7.5))
                .isEqualByComparingTo(expected);
    }
}
