package com.tenderready.png.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class QuotationCalculatorTest {
    private fun line(qtyMilli: Long, priceMinor: Long) = LineInput(qtyMilli, priceMinor)

    private fun assertInvalid(block: () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test fun oneLineItem() {
        val t = QuotationCalculator.calculate(listOf(line(1000, 10000)))
        assertEquals(listOf(10000L), t.lineTotalsMinor)
        assertEquals(10000L, t.subtotalMinor)
        assertEquals(10000L, t.totalMinor)
        assertEquals(0L, t.taxMinor)
    }

    @Test fun multipleLineItems() {
        val t = QuotationCalculator.calculate(listOf(line(2000, 5000), line(3000, 2550)))
        assertEquals(listOf(10000L, 7650L), t.lineTotalsMinor)
        assertEquals(17650L, t.subtotalMinor)
        assertEquals(17650L, t.totalMinor)
    }

    @Test fun decimalQuantities() {
        assertEquals(2500L, QuotationCalculator.lineTotal(2500, 1000))
    }

    @Test fun zeroPricedItem() {
        val t = QuotationCalculator.calculate(listOf(line(1000, 0), line(1000, 500)))
        assertEquals(listOf(0L, 500L), t.lineTotalsMinor)
        assertEquals(500L, t.totalMinor)
    }

    @Test fun lineRoundingIsHalfUp() {
        assertEquals(33L, QuotationCalculator.lineTotal(333, 100)) // 33.3
        assertEquals(33L, QuotationCalculator.lineTotal(334, 100)) // 33.4
        assertEquals(34L, QuotationCalculator.lineTotal(335, 100)) // 33.5
    }

    @Test fun noFloatingPointDrift() {
        // 3 x K0.10 must be exactly K0.30
        assertEquals(30L, QuotationCalculator.lineTotal(3000, 10))
    }

    @Test fun additionalCharges() {
        val t = QuotationCalculator.calculate(listOf(line(1000, 10000)), additionalChargesMinor = 2500)
        assertEquals(12500L, t.totalMinor)
    }

    @Test fun discount() {
        val t = QuotationCalculator.calculate(listOf(line(1000, 10000)), discountMinor = 1500)
        assertEquals(8500L, t.totalMinor)
    }

    @Test fun discountCannotExceedAmount() {
        assertInvalid { QuotationCalculator.calculate(listOf(line(1000, 10000)), discountMinor = 10001) }
    }

    @Test fun taxDisabledIgnoresRate() {
        val t = QuotationCalculator.calculate(listOf(line(1000, 10000)), taxEnabled = false, taxRateBp = 1000)
        assertEquals(0L, t.taxMinor)
        assertEquals(10000L, t.totalMinor)
    }

    @Test fun taxEnabled() {
        val t = QuotationCalculator.calculate(listOf(line(1000, 10000)), taxEnabled = true, taxRateBp = 1000)
        assertEquals(1000L, t.taxMinor)
        assertEquals(11000L, t.totalMinor)
    }

    @Test fun taxRoundingHalfUp() {
        assertEquals(101L, QuotationCalculator.calculate(listOf(line(1000, 1005)), taxEnabled = true, taxRateBp = 1000).taxMinor) // 100.5
        assertEquals(100L, QuotationCalculator.calculate(listOf(line(1000, 1004)), taxEnabled = true, taxRateBp = 1000).taxMinor) // 100.4
    }

    @Test fun taxAppliesAfterChargesAndDiscount() {
        val t = QuotationCalculator.calculate(
            listOf(line(1000, 10000)), additionalChargesMinor = 2000, discountMinor = 1000,
            taxEnabled = true, taxRateBp = 750
        )
        assertEquals(11000L, t.taxableMinor)
        assertEquals(825L, t.taxMinor)
        assertEquals(11825L, t.totalMinor)
    }

    @Test fun negativeQuantityRejected() {
        assertInvalid { QuotationCalculator.calculate(listOf(line(-1000, 100))) }
    }

    @Test fun negativePriceRejected() {
        assertInvalid { QuotationCalculator.calculate(listOf(line(1000, -100))) }
    }

    @Test fun negativeChargesAndDiscountRejected() {
        assertInvalid { QuotationCalculator.calculate(emptyList(), additionalChargesMinor = -1) }
        assertInvalid { QuotationCalculator.calculate(emptyList(), discountMinor = -1) }
    }

    @Test fun invalidTaxRateRejected() {
        assertInvalid { QuotationCalculator.calculate(emptyList(), taxEnabled = true, taxRateBp = 10001) }
    }

    @Test fun emptyQuotationIsZero() {
        val t = QuotationCalculator.calculate(emptyList())
        assertEquals(0L, t.subtotalMinor)
        assertEquals(0L, t.totalMinor)
        assertTrue(t.lineTotalsMinor.isEmpty())
    }

    @Test fun largeTotals() {
        // 1,000,000 units at K1,000,000.00 each = K1,000,000,000,000.00
        val t = QuotationCalculator.calculate(listOf(line(1_000_000_000L, 100_000_000L)))
        assertEquals(100_000_000_000_000L, t.totalMinor)
    }

    @Test fun overflowIsReportedNotWrapped() {
        assertInvalid { QuotationCalculator.lineTotal(Long.MAX_VALUE, Long.MAX_VALUE) }
        assertInvalid {
            QuotationCalculator.calculate(listOf(line(1000, Long.MAX_VALUE), line(1000, Long.MAX_VALUE)))
        }
    }
}
