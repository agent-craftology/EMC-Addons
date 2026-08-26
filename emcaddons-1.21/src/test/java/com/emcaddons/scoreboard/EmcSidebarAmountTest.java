package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmcSidebarAmountTest {

    private static final double EPS = 1e-6;

    @Test
    void parsesSmallCapsK() {
        assertEquals(1200.0, EmcSidebar.parseAmount("1.2\u1D0B"), EPS);
        assertEquals(9900.0, EmcSidebar.parseAmount("9.9\u1D0B"), EPS);
        assertEquals(10000.0, EmcSidebar.parseAmount("10000"), EPS);
    }

    @Test
    void parsesSmallCapsMbt() {
        assertEquals(1_200_000.0, EmcSidebar.parseAmount("1.2\u1D0D"), EPS);
        assertEquals(1_200_000_000.0, EmcSidebar.parseAmount("1.2\u0299"), EPS);
        assertEquals(1_200_000_000_000.0, EmcSidebar.parseAmount("1.2\u1D1B"), EPS);
    }

    @Test
    void parsesAsciiK() {
        assertEquals(1200.0, EmcSidebar.parseAmount("1.2k"), EPS);
        assertEquals(1200.0, EmcSidebar.parseAmount("1.2K"), EPS);
    }

    @Test
    void stripsColorAndCommas() {
        assertEquals(1234.0, EmcSidebar.parseAmount("§a1,234"), EPS);
    }
}
