package com.tenderready.png.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tenderready.png.SimpleFactory
import com.tenderready.png.core.Money
import com.tenderready.png.pdf.DocumentBuilders
import com.tenderready.png.pdf.PdfExporter

@Composable
fun QuotationScreen(vm: MainViewModel, id: Long, onBack: () -> Unit) {
    val editor: QuotationEditorViewModel = viewModel(
        key = "quotation-$id",
        factory = SimpleFactory { QuotationEditorViewModel(vm.repo, id) }
    )
    ScreenScaffold("Quotation", onBack) {
        if (!editor.loaded) {
            Text("Loading...")
            return@ScreenScaffold
        }
        SectionTitle("Your business")
        Hint("Copied from your profile. You can change it for this quotation only.")
        FormField("Business name", editor.businessName, { editor.businessName = it })
        FormField("Business location", editor.businessLocation, { editor.businessLocation = it })
        FormField("Contact details", editor.businessContact, { editor.businessContact = it }, singleLine = false, minLines = 2)

        SectionTitle("Quotation details")
        FormField("Quotation number", editor.number, { editor.number = it }, hint = "Must be different from every other quotation on this phone")
        FormField("Issue date", editor.issueDate, { editor.issueDate = it }, hint = "Format: YYYY-MM-DD", keyboard = KeyboardType.Number)
        FormField("Valid for (days, optional)", editor.validity, { editor.validity = it }, keyboard = KeyboardType.Number)

        SectionTitle("Client")
        FormField("Client name", editor.clientName, { editor.clientName = it })
        FormField("Client contact details", editor.clientContact, { editor.clientContact = it }, singleLine = false, minLines = 2)
        FormField("Description of work or goods", editor.workDescription, { editor.workDescription = it }, singleLine = false, minLines = 3)

        SectionTitle("Line items")
        val totals = editor.previewTotals()
        editor.items.forEachIndexed { index, item ->
            key(item.key) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Item ${index + 1}", style = MaterialTheme.typography.titleSmall)
                        FormField("Description", item.description, { item.description = it })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FormField("Quantity", item.quantity, { item.quantity = it }, modifier = Modifier.weight(1f), keyboard = KeyboardType.Decimal)
                            FormField("Unit", item.unit, { item.unit = it }, modifier = Modifier.weight(1f), hint = "e.g. day, bag")
                        }
                        FormField("Unit price (K)", item.unitPrice, { item.unitPrice = it }, keyboard = KeyboardType.Decimal)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            val line = totals?.lineTotalsMinor?.getOrNull(index)
                            Text(if (line != null) "Line total: ${Money.formatKina(line)}" else "Line total: enter valid amounts")
                            OutlinedButton(onClick = { editor.removeItem(item) }) { Text("Remove") }
                        }
                    }
                }
            }
        }
        BigButton("Add line item", onClick = { editor.addItem() }, outlined = true)

        SectionTitle("Charges, discount and tax")
        FormField("Additional charges label (optional)", editor.chargesLabel, { editor.chargesLabel = it }, hint = "e.g. Delivery")
        FormField("Additional charges (K)", editor.charges, { editor.charges = it }, keyboard = KeyboardType.Decimal)
        FormField("Discount (K)", editor.discount, { editor.discount = it }, keyboard = KeyboardType.Decimal)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Add tax to this quotation", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = editor.taxEnabled, onCheckedChange = { editor.taxEnabled = it })
        }
        Hint("Tax is off by default. Only turn it on if tax applies to your business, and enter the rate you are required to use.")
        if (editor.taxEnabled) {
            FormField("Tax name", editor.taxLabel, { editor.taxLabel = it }, hint = "As it should appear on the quotation")
            FormField("Tax rate (%)", editor.taxRate, { editor.taxRate = it }, keyboard = KeyboardType.Decimal)
        }

        InfoCard {
            Text("Totals", style = MaterialTheme.typography.titleMedium)
            if (totals == null) {
                Text("Totals appear when all amounts are valid.")
            } else {
                Text("Subtotal: ${Money.formatKina(totals.subtotalMinor)}")
                if (totals.additionalChargesMinor > 0) Text("Additional charges: ${Money.formatKina(totals.additionalChargesMinor)}")
                if (totals.discountMinor > 0) Text("Discount: -${Money.formatKina(totals.discountMinor)}")
                if (editor.taxEnabled) Text("Tax: ${Money.formatKina(totals.taxMinor)}")
                Text("Total: ${Money.formatKina(totals.totalMinor)}", style = MaterialTheme.typography.titleLarge)
            }
        }

        SectionTitle("Terms")
        FormField("Payment terms", editor.paymentTerms, { editor.paymentTerms = it }, singleLine = false, minLines = 2)
        FormField("Notes", editor.notes, { editor.notes = it }, singleLine = false, minLines = 2)

        ErrorList(editor.errors)
        BigButton("Save quotation", onClick = { editor.save() })
        val m = editor.savedMessage
        if (m != null) Text(m)

        SectionTitle("Create PDF")
        ExportBar(
            fileName = PdfExporter.fileName("quotation", editor.number.ifBlank { "draft" }),
            prepare = {
                val savedId = editor.saveNow()
                if (savedId == null) null else editor.loadSaved()?.let { DocumentBuilders.quotation(it) }
            }
        )
        Hint("Unsaved changes are lost if you leave this screen. The PDF is made from the saved quotation.")
    }
}
