package com.tenderready.png.core

/**
 * Money is stored as Long "minor units" (toea). 100 toea = K1.00 (PGK).
 * Quantities are stored as Long "milli-units" (1.5 -> 1500) so decimal quantities never use floating point.
 * Tax rates are stored as Int "basis points" (10.00% -> 1000).
 */
object Money {
    private val moneyRegex = Regex("""^(\d{1,12})(?:\.(\d{1,2}))?$""")
    private val quantityRegex = Regex("""^(\d{1,9})(?:\.(\d{1,3}))?$""")
    private val rateRegex = Regex("""^(\d{1,3})(?:\.(\d{1,2}))?$""")

    /** Parses "1,234.50" into 123450. Returns null when invalid (negative, letters, 3+ decimals...). */
    fun parseMinor(text: String): Long? {
        val t = text.trim().replace(",", "").replace(" ", "")
        val m = moneyRegex.matchEntire(t) ?: return null
        val whole = m.groupValues[1].toLong()
        val frac = m.groupValues[2].padEnd(2, '0').toLong()
        return whole * 100 + frac
    }

    /** Parses "2.5" into 2500 (milli-units). Up to 3 decimals. Returns null when invalid. */
    fun parseQuantityMilli(text: String): Long? {
        val t = text.trim().replace(",", "").replace(" ", "")
        val m = quantityRegex.matchEntire(t) ?: return null
        val whole = m.groupValues[1].toLong()
        val frac = m.groupValues[2].padEnd(3, '0').toLong()
        return whole * 1000 + frac
    }

    /** Parses a percentage such as "10" or "7.5" into basis points (1000 / 750). Range 0..100%. */
    fun parseRateBp(text: String): Int? {
        val t = text.trim().replace("%", "").replace(" ", "")
        val m = rateRegex.matchEntire(t) ?: return null
        val whole = m.groupValues[1].toInt()
        val frac = m.groupValues[2].padEnd(2, '0').toInt()
        val bp = whole * 100 + frac
        return if (bp in 0..10000) bp else null
    }

    private fun group(n: Long): String {
        val s = n.toString()
        val sb = StringBuilder()
        s.forEachIndexed { i, c ->
            if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
            sb.append(c)
        }
        return sb.toString()
    }

    /** "1,234.50" (no currency symbol), used in table columns. */
    fun formatPlain(minor: Long): String {
        val negative = minor < 0
        val abs = if (negative) -minor else minor
        val s = group(abs / 100) + "." + (abs % 100).toString().padStart(2, '0')
        return if (negative) "-$s" else s
    }

    /** "K1,234.50" */
    fun formatKina(minor: Long): String {
        val p = formatPlain(minor)
        return if (p.startsWith("-")) "-K" + p.substring(1) else "K$p"
    }

    /** "2.5" or "3" */
    fun formatQuantity(milli: Long): String {
        val whole = milli / 1000
        val frac = (milli % 1000).toString().padStart(3, '0').trimEnd('0')
        return if (frac.isEmpty()) "$whole" else "$whole.$frac"
    }

    /** "10%" or "7.5%" */
    fun formatRate(bp: Int): String {
        val whole = bp / 100
        val frac = (bp % 100).toString().padStart(2, '0').trimEnd('0')
        return if (frac.isEmpty()) "$whole%" else "$whole.$frac%"
    }

    /** Text for an edit field: "1234.50" */
    fun toInput(minor: Long): String = "${minor / 100}." + (minor % 100).toString().padStart(2, '0')

    /** Rate text for an edit field: "10" or "7.5" */
    fun rateToInput(bp: Int): String = formatRate(bp).removeSuffix("%")
}
