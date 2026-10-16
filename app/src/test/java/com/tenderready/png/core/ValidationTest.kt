package com.tenderready.png.core

import com.tenderready.png.data.BusinessProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTest {
    private fun item(desc: String = "Work", qty: String = "1", unit: String = "day", price: String = "100") =
        ItemInput(desc, qty, unit, price)

    private fun form(
        number: String = "QT-0001", date: String = "2026-10-17", client: String = "Client Ltd",
        validity: String = "", charges: String = "", discount: String = "",
        tax: Boolean = false, rate: String = "", items: List<ItemInput> = listOf(item())
    ) = QuotationForm(number, date, client, validity, charges, discount, tax, rate, items)

    private fun errorsOf(f: QuotationForm) = Validation.evaluateQuotation(f).errors

    // ---- Dates
    @Test fun dates() {
        assertTrue(DateUtil.isValidIso("2024-02-29"))
        assertFalse(DateUtil.isValidIso("2023-02-29"))
        assertFalse(DateUtil.isValidIso("2026-13-01"))
        assertFalse(DateUtil.isValidIso("2026-04-31"))
        assertFalse(DateUtil.isValidIso("26-01-01"))
        assertFalse(DateUtil.isValidIso(""))
        assertEquals("17 Oct 2026", DateUtil.display("2026-10-17"))
    }

    // ---- Profile
    @Test fun emptyProfileCanBeSaved() {
        assertTrue(Validation.profileErrors(BusinessProfile()).isEmpty())
    }

    @Test fun profileFormatErrors() {
        val e = Validation.profileErrors(BusinessProfile(email = "nope", phone = "abc", yearsExperience = "81"))
        assertNotNull(e["email"])
        assertNotNull(e["phone"])
        assertNotNull(e["years"])
        assertTrue(Validation.profileErrors(BusinessProfile(email = "a@b.pg", phone = "+675 7000 1234", yearsExperience = "5")).isEmpty())
        assertNotNull(Validation.profileErrors(BusinessProfile(yearsExperience = "abc"))["years"])
    }

    @Test fun missingBusinessInformation() {
        assertEquals(4, Validation.profileMissing(null).size)
        assertEquals(listOf("Location", "Contact person", "Phone number"), Validation.profileMissing(BusinessProfile(businessName = "A")))
        assertTrue(
            Validation.profileMissing(BusinessProfile(businessName = "A", location = "B", contactPerson = "C", phone = "123456")).isEmpty()
        )
    }

    // ---- Quotation form
    @Test fun validQuotation() {
        val r = Validation.evaluateQuotation(form())
        assertTrue(r.errors.isEmpty())
        assertEquals(10000L, r.totals?.totalMinor)
    }

    @Test fun requiredFields() {
        assertTrue(errorsOf(form(number = " ")).any { it.contains("quotation number") })
        assertTrue(errorsOf(form(client = "")).any { it.contains("client name") })
    }

    @Test fun invalidDate() {
        assertTrue(errorsOf(form(date = "2026-02-30")).any { it.contains("Issue date") })
    }

    @Test fun emptyLineItems() {
        assertTrue(errorsOf(form(items = emptyList())).any { it.contains("at least one line item") })
        assertTrue(errorsOf(form(items = listOf(item(desc = " ")))).any { it.contains("Item 1") })
    }

    @Test fun invalidAmounts() {
        assertTrue(errorsOf(form(items = listOf(item(qty = "0")))).any { it.contains("quantity") })
        assertTrue(errorsOf(form(items = listOf(item(qty = "-2")))).any { it.contains("quantity") })
        assertTrue(errorsOf(form(items = listOf(item(price = "-5")))).any { it.contains("unit price") })
        assertTrue(errorsOf(form(items = listOf(item(price = "abc")))).any { it.contains("unit price") })
        assertNull(Validation.evaluateQuotation(form(items = listOf(item(price = "abc")))).totals)
    }

    @Test fun discountTooLarge() {
        assertTrue(errorsOf(form(discount = "500")).any { it.contains("Discount cannot be more") })
    }

    @Test fun taxRules() {
        assertTrue(errorsOf(form(tax = true, rate = "")).any { it.contains("tax rate") })
        assertTrue(errorsOf(form(tax = false, rate = "garbage")).isEmpty()) // rate ignored when tax is off
        assertEquals(1000L, Validation.evaluateQuotation(form(tax = true, rate = "10")).totals?.taxMinor)
    }

    @Test fun validityDays() {
        assertTrue(errorsOf(form(validity = "0")).any { it.contains("Validity") })
        assertTrue(errorsOf(form(validity = "30")).isEmpty())
    }

    // ---- Opportunities and checklists
    @Test fun opportunityAndRequirementValidation() {
        assertFalse(Validation.opportunityErrors("", "").isEmpty())
        assertFalse(Validation.opportunityErrors("Road repair", "2026-99-99").isEmpty())
        assertTrue(Validation.opportunityErrors("Road repair", "").isEmpty())
        assertFalse(Validation.requirementErrors("", "").isEmpty())
        assertTrue(Validation.requirementErrors("Tax letter", "2026-12-31").isEmpty())
        assertFalse(Validation.readinessErrors(" ").isEmpty())
        assertFalse(Validation.capabilityErrors("").isEmpty())
    }
}
