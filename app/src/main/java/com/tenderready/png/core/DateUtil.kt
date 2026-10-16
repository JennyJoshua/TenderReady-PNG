package com.tenderready.png.core

import java.util.Calendar

/** Dates are stored as "YYYY-MM-DD" text. No java.time so it works on minSdk 24 without desugaring. */
object DateUtil {
    private val dateRegex = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")
    private val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    fun isLeap(y: Int) = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0

    fun daysIn(y: Int, m: Int): Int = when (m) {
        2 -> if (isLeap(y)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    fun isValidIso(text: String): Boolean {
        val m = dateRegex.matchEntire(text.trim()) ?: return false
        val y = m.groupValues[1].toInt()
        val mo = m.groupValues[2].toInt()
        val d = m.groupValues[3].toInt()
        return y in 1900..2200 && mo in 1..12 && d in 1..daysIn(y, mo)
    }

    fun todayIso(): String {
        val c = Calendar.getInstance()
        return "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    /** "2026-10-17" -> "17 Oct 2026". Returns the input unchanged if it is not a valid date. */
    fun display(text: String): String {
        if (!isValidIso(text)) return text
        val p = text.trim().split("-")
        return "${p[2].toInt()} ${months[p[1].toInt() - 1]} ${p[0]}"
    }

    fun displayMillis(millis: Long): String {
        if (millis <= 0L) return ""
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return "${c.get(Calendar.DAY_OF_MONTH)} ${months[c.get(Calendar.MONTH)]} ${c.get(Calendar.YEAR)}"
    }
}
