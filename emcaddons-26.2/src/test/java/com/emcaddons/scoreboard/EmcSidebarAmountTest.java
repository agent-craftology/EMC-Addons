package com.emcaddons.scoreboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmcSidebarAmountTest {
    @Test
    void smallCapsKIsThousands() {
        assertEquals(1200.0, EmcSidebar.parseAmount("1.2\u1D0B"));
    }

    @Test
    void abbreviatedAndFullAgreeAroundTenThousand() {
        assertEquals(9900.0, EmcSidebar.parseAmount("9.9\u1D0B"));
        assertEquals(10000.0, EmcSidebar.parseAmount("10000"));
    }

    @Test
    void smallCapsMillionBillionTrillion() {
        assertEquals(1_500_000.0, EmcSidebar.parseAmount("1.5\u1D0D"));
        assertEquals(2_000_000_000.0, EmcSidebar.parseAmount("2\u0299"));
        assertEquals(3_000_000_000_000.0, EmcSidebar.parseAmount("3\u1D1B"));
    }

    @Test
    void asciiKSuffixStillWorks() {
        assertEquals(1200.0, EmcSidebar.parseAmount("1.2k"));
        assertEquals(1200.0, EmcSidebar.parseAmount("1.2K"));
    }

    @Test
    void colorAndCommaFormatting() {
        assertEquals(1234.0, EmcSidebar.parseAmount("§a1,234"));
    }
}
