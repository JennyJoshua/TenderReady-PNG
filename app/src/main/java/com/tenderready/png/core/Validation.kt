package com.tenderready.png.core

import com.tenderready.png.data.BusinessProfile

data class ItemInput(val description: String, val quantity: String, val unit: String, val unitPrice: String)

/** Everything the quotation editor holds, as the raw text the user typed. */
data class QuotationForm(
    val number: String,
    val issueDate: String,
    val clientName: String,
    val validityDays: String,
    val charges: String,
    val discount: String,
    val taxEnabled: Boolean,
    val taxRate: String,
    val items: List<ItemInput>
)

/** totals is null when an amount could not be read; errors lists everything the user must fix. */
data class QuotationFormResult(val totals: QuotationTotals?, val errors: List<String>)

object Validation {
    private val emailRegex = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")
    private val phoneRegex = Regex("""^[+0-9 ()\-]{6,20}$""")

    /** Profile can be saved incomplete. Only values that are present AND badly formatted are errors. */
    fun profileErrors(p: BusinessProfile): Map<String, String> {
        val e = mutableMapOf<String, String>()
        if (p.email.isNotBlank() && !emailRegex.matches(p.email.trim())) e["email"] = "Enter a valid email address, or leave it empty."
        if (p.phone.isNotBlank() && !phoneRegex.matches(p.phone.trim())) e["phone"] = "Enter a valid phone number (digits, +, spaces), or leave it empty."
        val y = p.yearsExperience.trim()
        if (y.isNotEmpty()) {
            val n = y.toIntOrNull()
            if (n == null || n !in 0..80) e["years"] = "Enter years as a whole number from 0 to 80, or leave it empty."
        }
        return e
    }

    /** Names of key business details that are still empty. Used to warn before creating documents. */
    fun profileMissing(p: BusinessProfile?): List<String> {
        if (p == null) return listOf("Business name", "Location", "Contact person", "Phone number")
        val out = mutableListOf<String>()
        if (p.businessName.isBlank()) out += "Business name"
        if (p.location.isBlank()) out += "Location"
        if (p.contactPerson.isBlank()) out += "Contact person"
        if (p.phone.isBlank()) out += "Phone number"
        return out
    }

    fun capabilityErrors(businessName: String): List<String> =
        if (businessName.isBlank()) listOf("Enter the business name.") else emptyList()

    fun opportunityErrors(title: String, deadline: String): List<String> {
        val out = mutableListOf<String>()
        if (title.isBlank()) out += "Enter the opportunity title."
        if (deadline.isNotBlank() && !DateUtil.isValidIso(deadline)) out += "Deadline must be a real date in the form YYYY-MM-DD, or empty."
        return out
    }

    fun requirementErrors(title: String, deadline: String): List<String> {
        val out = mutableListOf<String>()
        if (title.isBlank()) out += "Enter the requirement title."
        if (deadline.isNotBlank() && !DateUtil.isValidIso(deadline)) out += "Deadline must be a real date in the form YYYY-MM-DD, or empty."
        return out
    }

    fun readinessErrors(title: String): List<String> =
        if (title.isBlank()) listOf("Enter the item title.") else emptyList()

    /** Single place where the quotation form is read, validated and calculated. */
    fun evaluateQuotation(f: QuotationForm): QuotationFormResult {
        val errors = mutableListOf<String>()
        if (f.number.isBlank()) errors += "Enter a quotation number."
        if (!DateUtil.isValidIso(f.issueDate)) errors += "Issue date must be a real date in the form YYYY-MM-DD."
        if (f.clientName.isBlank()) errors += "Enter the client name."
        val v = f.validityDays.trim()
        if (v.isNotEmpty()) {
            val n = v.toIntOrNull()
            if (n == null || n !in 1..3650) errors += "Validity must be a whole number of days from 1 to 3650, or empty."
        }
        if (f.items.isEmpty()) errors += "Add at least one line item."

        var numericOk = true
        val lines = mutableListOf<LineInput>()
        f.items.forEachIndexed { i, item ->
            val n = i + 1
            if (item.description.isBlank()) errors += "Item $n: enter a description."
            val q = Money.parseQuantityMilli(item.quantity)
            val p = Money.parseMinor(item.unitPrice)
            if (q == null || q <= 0L) {
                errors += "Item $n: quantity must be a number above 0 (up to 3 decimals, no minus sign)."
                numericOk = false
            }
            if (p == null) {
                errors += "Item $n: unit price must be 0 or more (up to 2 decimals, no minus sign)."
                numericOk = false
            }
            if (q != null && p != null && q > 0L) lines += LineInput(q, p)
        }

        val charges = if (f.charges.isBlank()) 0L else Money.parseMinor(f.charges)
        if (charges == null) {
            errors += "Additional charges must be an amount of 0 or more."
            numericOk = false
        }
        val discount = if (f.discount.isBlank()) 0L else Money.parseMinor(f.discount)
        if (discount == null) {
            errors += "Discount must be an amount of 0 or more."
            numericOk = false
        }
        var rate = 0
        if (f.taxEnabled) {
            val r = Money.parseRateBp(f.taxRate)
            if (r == null) {
                errors += "Enter the tax rate as a percentage from 0 to 100."
                numericOk = false
            } else rate = r
        }

        var totals: QuotationTotals? = null
        if (numericOk && charges != null && discount != null) {
            try {
                totals = QuotationCalculator.calculate(lines, charges, discount, f.taxEnabled, rate)
            } catch (e: IllegalArgumentException) {
                errors += e.message ?: "Totals could not be calculated."
            }
        }
        return QuotationFormResult(totals, errors)
    }
}
