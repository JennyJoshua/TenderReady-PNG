package com.tenderready.png.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenderready.png.core.DateUtil
import com.tenderready.png.core.ItemInput
import com.tenderready.png.core.Money
import com.tenderready.png.core.QuotationForm
import com.tenderready.png.core.QuotationTotals
import com.tenderready.png.core.Validation
import com.tenderready.png.data.AppRepository
import com.tenderready.png.data.Quotation
import com.tenderready.png.data.QuotationItem
import com.tenderready.png.data.QuotationWithItems
import com.tenderready.png.data.SaveResult
import com.tenderready.png.data.businessContactLine
import kotlinx.coroutines.launch

class ItemDraft(
    val key: Long,
    description: String = "",
    quantity: String = "1",
    unit: String = "",
    unitPrice: String = ""
) {
    var description by mutableStateOf(description)
    var quantity by mutableStateOf(quantity)
    var unit by mutableStateOf(unit)
    var unitPrice by mutableStateOf(unitPrice)
}

/** Keeps the quotation being edited alive across rotation and navigation. Saved data lives in Room. */
class QuotationEditorViewModel(private val repo: AppRepository, quotationId: Long) : ViewModel() {
    var loaded by mutableStateOf(false); private set
    var savedId by mutableStateOf(quotationId); private set
    var number by mutableStateOf("")
    var issueDate by mutableStateOf("")
    var businessName by mutableStateOf("")
    var businessLocation by mutableStateOf("")
    var businessContact by mutableStateOf("")
    var clientName by mutableStateOf("")
    var clientContact by mutableStateOf("")
    var workDescription by mutableStateOf("")
    var chargesLabel by mutableStateOf("")
    var charges by mutableStateOf("")
    var discount by mutableStateOf("")
    var taxEnabled by mutableStateOf(false)
    var taxLabel by mutableStateOf("")
    var taxRate by mutableStateOf("")
    var validity by mutableStateOf("")
    var paymentTerms by mutableStateOf("")
    var notes by mutableStateOf("")
    var errors by mutableStateOf<List<String>>(emptyList()); private set
    var savedMessage by mutableStateOf<String?>(null); private set
    val items = mutableStateListOf<ItemDraft>()
    private var createdAt = 0L
    private var nextKey = 1L

    init {
        viewModelScope.launch { load(quotationId) }
    }

    private suspend fun load(id: Long) {
        if (id == 0L) {
            number = repo.nextQuotationNumber()
            issueDate = DateUtil.todayIso()
            val p = repo.getProfile()
            if (p != null) {
                businessName = p.businessName
                businessLocation = p.location
                businessContact = businessContactLine(p)
            }
            items.add(ItemDraft(nextKey++))
        } else {
            val qi = repo.getQuotation(id)
            if (qi != null) applySaved(qi)
        }
        loaded = true
    }

    private fun applySaved(qi: QuotationWithItems) {
        val q = qi.quotation
        createdAt = q.createdAt
        number = q.quotationNumber
        issueDate = q.issueDate
        businessName = q.businessName
        businessLocation = q.businessLocation
        businessContact = q.businessContact
        clientName = q.clientName
        clientContact = q.clientContact
        workDescription = q.workDescription
        chargesLabel = q.additionalChargesLabel
        charges = if (q.additionalChargesMinor > 0) Money.toInput(q.additionalChargesMinor) else ""
        discount = if (q.discountMinor > 0) Money.toInput(q.discountMinor) else ""
        taxEnabled = q.taxEnabled
        taxLabel = q.taxLabel
        taxRate = if (q.taxEnabled) Money.rateToInput(q.taxRateBp) else ""
        validity = if (q.validityDays > 0) q.validityDays.toString() else ""
        paymentTerms = q.paymentTerms
        notes = q.notes
        items.clear()
        qi.items.sortedBy { it.position }.forEach {
            items.add(
                ItemDraft(nextKey++, it.description, Money.formatQuantity(it.quantityMilli), it.unit, Money.toInput(it.unitPriceMinor))
            )
        }
    }

    fun addItem() { items.add(ItemDraft(nextKey++)) }
    fun removeItem(item: ItemDraft) { items.remove(item) }

    private fun form() = QuotationForm(
        number, issueDate, clientName, validity, charges, discount, taxEnabled, taxRate,
        items.map { ItemInput(it.description, it.quantity, it.unit, it.unitPrice) }
    )

    /** Live totals shown on screen. Null until every amount is readable. */
    fun previewTotals(): QuotationTotals? = Validation.evaluateQuotation(form()).totals

    /** Validates, then saves to the database. Returns the saved id, or null when something must be fixed. */
    suspend fun saveNow(): Long? {
        savedMessage = null
        val result = Validation.evaluateQuotation(form())
        if (result.errors.isNotEmpty()) {
            errors = result.errors
            return null
        }
        val q = Quotation(
            id = savedId,
            quotationNumber = number.trim(),
            businessName = businessName.trim(),
            businessLocation = businessLocation.trim(),
            businessContact = businessContact.trim(),
            issueDate = issueDate.trim(),
            clientName = clientName.trim(),
            clientContact = clientContact.trim(),
            workDescription = workDescription.trim(),
            additionalChargesLabel = chargesLabel.trim(),
            additionalChargesMinor = if (charges.isBlank()) 0L else Money.parseMinor(charges) ?: 0L,
            discountMinor = if (discount.isBlank()) 0L else Money.parseMinor(discount) ?: 0L,
            taxEnabled = taxEnabled,
            taxLabel = taxLabel.trim(),
            taxRateBp = if (taxEnabled) Money.parseRateBp(taxRate) ?: 0 else 0,
            validityDays = validity.trim().toIntOrNull() ?: 0,
            paymentTerms = paymentTerms.trim(),
            notes = notes.trim(),
            createdAt = createdAt
        )
        val rows = items.map {
            QuotationItem(
                description = it.description.trim(),
                quantityMilli = Money.parseQuantityMilli(it.quantity) ?: 0L,
                unit = it.unit.trim(),
                unitPriceMinor = Money.parseMinor(it.unitPrice) ?: 0L
            )
        }
        return when (val r = repo.saveQuotation(q, rows)) {
            is SaveResult.Saved -> {
                errors = emptyList()
                savedId = r.id
                number = number.trim().uppercase()
                savedMessage = "Quotation saved."
                r.id
            }
            is SaveResult.Failed -> {
                errors = listOf(r.message)
                null
            }
        }
    }

    fun save() {
        viewModelScope.launch { saveNow() }
    }

    suspend fun loadSaved(): QuotationWithItems? = if (savedId == 0L) null else repo.getQuotation(savedId)
}
