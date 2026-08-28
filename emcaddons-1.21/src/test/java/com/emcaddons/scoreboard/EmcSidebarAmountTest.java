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

    @Test
    void parsePlainCountSmallCapsCreditsLine() {
        assertEquals(3076.0, EmcSidebar.parsePlainCount(
                "│ 3,076 \u1D04\u0280\u1D07\u1D05\u026A\u1D1B\uA731"), EPS);
    }

    @Test
    void parsePlainCountCreditsIgnoresSuffixLetters() {
        assertEquals(1234.0, EmcSidebar.parsePlainCount("1,234 credits"), EPS);
        assertEquals(3076.0, EmcSidebar.parsePlainCount("3076 credits"), EPS);
    }
}
