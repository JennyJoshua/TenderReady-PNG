package com.tenderready.png.pdf

import com.tenderready.png.core.DateUtil
import com.tenderready.png.core.Money
import com.tenderready.png.data.CapabilityStatement
import com.tenderready.png.data.QuotationWithItems
import com.tenderready.png.data.totals

/** Turns saved database records into PDF blocks. Empty sections are skipped; nothing is invented. */
object DocumentBuilders {
    const val CAPABILITY_DISCLAIMER =
        "This capability statement is based on information provided by the business owner. " +
            "It has not been verified by TenderReady PNG and does not confirm registration, tax status, " +
            "licences, certification or eligibility for any tender or contract."

    const val QUOTATION_DISCLAIMER =
        "Amounts are in Papua New Guinea Kina (PGK). This quotation is based on information entered by the business."

    fun capabilityStatement(c: CapabilityStatement): List<Block> {
        val b = mutableListOf<Block>()
        b += Block.Text("CAPABILITY STATEMENT", TextStyle.SMALL)
        b += Block.Text(c.businessName.ifBlank { "(Business name not provided)" }, TextStyle.TITLE)
        b += Block.Rule
        fun section(title: String, body: String) {
            if (body.isNotBlank()) {
                b += Block.Space(8f)
                b += Block.Text(title, TextStyle.HEADING)
                b += Block.Text(body.trim())
            }
        }
        section("Business overview", c.overview)
        section("Products and services", c.productsServices)
        section("Experience", c.experience)
        section("Qualifications", c.qualifications)
        section("References and previous projects", c.references)
        section("Contact details", c.contactDetails)
        b += Block.Space(14f)
        b += Block.Rule
        b += Block.Text(CAPABILITY_DISCLAIMER, TextStyle.SMALL)
        return b
    }

    fun quotation(qi: QuotationWithItems): List<Block> {
        val q = qi.quotation
        val b = mutableListOf<Block>()
        b += Block.Text("QUOTATION", TextStyle.SMALL)
        b += Block.Text(q.businessName.ifBlank { "(Business name not provided)" }, TextStyle.TITLE)
        if (q.businessLocation.isNotBlank()) b += Block.Text(q.businessLocation.trim())
        if (q.businessContact.isNotBlank()) b += Block.Text(q.businessContact.trim())
        b += Block.Rule
        b += Block.Field("Quotation no.", q.quotationNumber)
        b += Block.Field("Date", DateUtil.display(q.issueDate))
        if (q.validityDays > 0) b += Block.Field("Valid for", "${q.validityDays} days from the date above")
        b += Block.Space(8f)
        b += Block.Text("Client", TextStyle.HEADING)
        b += Block.Text(q.clientName, TextStyle.BOLD)
        if (q.clientContact.isNotBlank()) b += Block.Text(q.clientContact.trim())
        if (q.workDescription.isNotBlank()) {
            b += Block.Space(8f)
            b += Block.Text("Description of work or goods", TextStyle.HEADING)
            b += Block.Text(q.workDescription.trim())
        }
        b += Block.Space(8f)
        b += Block.Text("Items", TextStyle.HEADING)
        b += Block.Row(listOf("Description", "Qty", "Unit", "Unit price (K)", "Total (K)"), header = true)
        val totals = qi.totals()
        val items = qi.items.sortedBy { it.position }
        items.forEachIndexed { i, it ->
            b += Block.Row(
                listOf(
                    it.description,
                    Money.formatQuantity(it.quantityMilli),
                    it.unit,
                    Money.formatPlain(it.unitPriceMinor),
                    if (totals != null) Money.formatPlain(totals.lineTotalsMinor[i]) else ""
                )
            )
        }
        b += Block.Space(6f)
        if (totals == null) {
            b += Block.Text("Totals could not be calculated. Please check the amounts and try again.", TextStyle.BOLD)
        } else {
            b += Block.Total("Subtotal", Money.formatKina(totals.subtotalMinor))
            if (totals.additionalChargesMinor > 0) {
                b += Block.Total(q.additionalChargesLabel.ifBlank { "Additional charges" }, Money.formatKina(totals.additionalChargesMinor))
            }
            if (totals.discountMinor > 0) b += Block.Total("Discount", "-" + Money.formatKina(totals.discountMinor))
            if (q.taxEnabled) {
                b += Block.Total("${q.taxLabel.ifBlank { "Tax" }} (${Money.formatRate(q.taxRateBp)})", Money.formatKina(totals.taxMinor))
            }
            b += Block.Total("TOTAL (PGK)", Money.formatKina(totals.totalMinor), bold = true)
        }
        if (q.paymentTerms.isNotBlank()) {
            b += Block.Space(10f)
            b += Block.Text("Payment terms", TextStyle.HEADING)
            b += Block.Text(q.paymentTerms.trim())
        }
        if (q.notes.isNotBlank()) {
            b += Block.Space(8f)
            b += Block.Text("Notes", TextStyle.HEADING)
            b += Block.Text(q.notes.trim())
        }
        b += Block.Space(14f)
        b += Block.Rule
        b += Block.Text(QUOTATION_DISCLAIMER, TextStyle.SMALL)
        return b
    }
}
