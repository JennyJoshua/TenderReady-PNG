package com.tenderready.png.core

/**
 * Progress for a checklist. Items marked "not applicable" are left out of the percentage.
 * percent = complete x 100 / applicable, rounded down. With no applicable items the percentage is 0.
 * This is a personal progress indicator only. It is NOT an official compliance score.
 */
data class ChecklistProgress(val total: Int, val complete: Int, val notApplicable: Int) {
    val applicable: Int get() = total - notApplicable
    val outstanding: Int get() = applicable - complete
    val percent: Int get() = if (applicable <= 0) 0 else (complete * 100) / applicable
}
