package com.tenderready.png.core

import java.math.BigDecimal
import java.math.RoundingMode

data class LineInput(val quantityMilli: Long, val unitPriceMinor: Long)

data class QuotationTotals(
    val lineTotalsMinor: List<Long>,
    val subtotalMinor: Long,
    val additionalChargesMinor: Long,
    val discountMinor: Long,
    val taxableMinor: Long,
    val taxMinor: Long,
    val totalMinor: Long
)

/**
 * Rounding rules (also documented in docs/data-model.md):
 *  1. line total = quantity x unit price, rounded HALF_UP to the nearest toea (0.01 kina).
 *  2. subtotal = sum of the already-rounded line totals.
 *  3. taxable amount = subtotal + additional charges - discount (discount is a fixed amount).
 *  4. tax = taxable amount x rate, rounded HALF_UP to the nearest toea, calculated once (not per line).
 *  5. total = taxable amount + tax. Tax is only applied when taxEnabled is true.
 * No floating point is used. Invalid input throws IllegalArgumentException.
 */
object QuotationCalculator {

    fun lineTotal(quantityMilli: Long, unitPriceMinor: Long): Long {
        require(quantityMilli >= 0) { "Quantity cannot be negative." }
        require(unitPriceMinor >= 0) { "Price cannot be negative." }
        try {
            return BigDecimal.valueOf(quantityMilli, 3)
                .multiply(BigDecimal.valueOf(unitPriceMinor))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        } catch (e: ArithmeticException) {
            throw IllegalArgumentException("Amount is too large.")
        }
    }

    fun calculate(
        lines: List<LineInput>,
        additionalChargesMinor: Long = 0L,
        discountMinor: Long = 0L,
        taxEnabled: Boolean = false,
        taxRateBp: Int = 0
    ): QuotationTotals {
        require(additionalChargesMinor >= 0) { "Additional charges cannot be negative." }
        require(discountMinor >= 0) { "Discount cannot be negative." }
        require(taxRateBp in 0..10000) { "Tax rate must be between 0% and 100%." }
        try {
            val lineTotals = lines.map { lineTotal(it.quantityMilli, it.unitPriceMinor) }
            val subtotal = lineTotals.fold(0L) { a, b -> Math.addExact(a, b) }
            val beforeDiscount = Math.addExact(subtotal, additionalChargesMinor)
            require(discountMinor <= beforeDiscount) {
                "Discount cannot be more than the subtotal plus additional charges."
            }
            val taxable = beforeDiscount - discountMinor
            val tax = if (taxEnabled) {
                BigDecimal.valueOf(taxable)
                    .multiply(BigDecimal.valueOf(taxRateBp.toLong()))
                    .divide(BigDecimal.valueOf(10000L), 0, RoundingMode.HALF_UP)
                    .longValueExact()
            } else 0L
            val total = Math.addExact(taxable, tax)
            return QuotationTotals(lineTotals, subtotal, additionalChargesMinor, discountMinor, taxable, tax, total)
        } catch (e: ArithmeticException) {
            throw IllegalArgumentException("Amount is too large.")
        }
    }
}
