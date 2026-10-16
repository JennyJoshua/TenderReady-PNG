package com.tenderready.png.data

import com.tenderready.png.core.ChecklistProgress
import com.tenderready.png.core.LineInput
import com.tenderready.png.core.QuotationCalculator
import com.tenderready.png.core.QuotationTotals

/** Totals are always calculated from the saved items, never stored, so they cannot go out of sync. */
fun QuotationWithItems.totals(): QuotationTotals? = try {
    QuotationCalculator.calculate(
        items.sortedBy { it.position }.map { LineInput(it.quantityMilli, it.unitPriceMinor) },
        quotation.additionalChargesMinor,
        quotation.discountMinor,
        quotation.taxEnabled,
        quotation.taxRateBp
    )
} catch (e: IllegalArgumentException) {
    null
}

fun requirementProgress(list: List<TenderRequirement>): ChecklistProgress = ChecklistProgress(
    total = list.size,
    complete = list.count { RequirementStatus.from(it.status) == RequirementStatus.COMPLETE },
    notApplicable = list.count { RequirementStatus.from(it.status) == RequirementStatus.NOT_APPLICABLE }
)

fun readinessProgress(list: List<ReadinessItem>): ChecklistProgress = ChecklistProgress(
    total = list.size,
    complete = list.count { ReadinessStatus.from(it.status) == ReadinessStatus.COMPLETE },
    notApplicable = list.count { ReadinessStatus.from(it.status) == ReadinessStatus.NOT_APPLICABLE }
)

fun businessContactLine(p: BusinessProfile): String = listOf(
    p.contactPerson.takeIf { it.isNotBlank() }?.let { "Contact: $it" },
    p.phone.takeIf { it.isNotBlank() }?.let { "Phone: $it" },
    p.email.takeIf { it.isNotBlank() }?.let { "Email: $it" }
).filterNotNull().joinToString("\n")
