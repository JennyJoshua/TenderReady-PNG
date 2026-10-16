package com.tenderready.png.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test fun parseMinor_valid() {
        assertEquals(123450L, Money.parseMinor("1,234.50"))
        assertEquals(0L, Money.parseMinor("0"))
        assertEquals(1250L, Money.parseMinor("12.5"))
        assertEquals(5L, Money.parseMinor("0.05"))
    }

    @Test fun parseMinor_invalid() {
        assertNull(Money.parseMinor(""))
        assertNull(Money.parseMinor("-5"))
        assertNull(Money.parseMinor("1.234"))
        assertNull(Money.parseMinor("abc"))
        assertNull(Money.parseMinor("5."))
    }

    @Test fun parseQuantity() {
        assertEquals(2500L, Money.parseQuantityMilli("2.5"))
        assertEquals(1L, Money.parseQuantityMilli("0.001"))
        assertEquals(3000L, Money.parseQuantityMilli("3"))
        assertNull(Money.parseQuantityMilli("1.2345"))
        assertNull(Money.parseQuantityMilli("-1"))
    }

    @Test fun parseRate() {
        assertEquals(750, Money.parseRateBp("7.5"))
        assertEquals(1000, Money.parseRateBp("10%"))
        assertEquals(10000, Money.parseRateBp("100"))
        assertNull(Money.parseRateBp("100.01"))
        assertNull(Money.parseRateBp("101"))
        assertNull(Money.parseRateBp("-1"))
    }

    @Test fun formatting() {
        assertEquals("K1,234,567.89", Money.formatKina(123456789L))
        assertEquals("K0.05", Money.formatKina(5L))
        assertEquals("-K10.50", Money.formatKina(-1050L))
        assertEquals("1,000.00", Money.formatPlain(100000L))
        assertEquals("2.5", Money.formatQuantity(2500L))
        assertEquals("3", Money.formatQuantity(3000L))
        assertEquals("0.001", Money.formatQuantity(1L))
        assertEquals("7.5%", Money.formatRate(750))
        assertEquals("10%", Money.formatRate(1000))
        assertEquals("0.05%", Money.formatRate(5))
        assertEquals("1234.50", Money.toInput(123450L))
    }
}
