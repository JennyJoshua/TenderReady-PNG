package com.tenderready.png.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenderready.png.core.DateUtil
import com.tenderready.png.core.Money
import com.tenderready.png.data.BusinessProfile
import com.tenderready.png.data.CapabilityStatement
import com.tenderready.png.data.OpportunityWithRequirements
import com.tenderready.png.data.QuotationWithItems
import com.tenderready.png.data.ReadinessItem
import com.tenderready.png.data.readinessProgress
import com.tenderready.png.data.requirementProgress
import com.tenderready.png.data.totals

private sealed class Pending(val title: String, val text: String) {
    class Capability(val id: Long, name: String) : Pending("Delete capability statement?", "\"$name\" will be deleted from this phone. PDFs you already exported are not affected.")
    class Quote(val id: Long, number: String) : Pending("Delete quotation?", "Quotation $number and its line items will be deleted from this phone. PDFs you already exported are not affected.")
    class Tender(val id: Long, name: String) : Pending("Delete opportunity?", "\"$name\" and all its requirements will be deleted from this phone.")
}

@Composable
fun SavedScreen(vm: MainViewModel, go: (String) -> Unit, onBack: () -> Unit) {
    val profile by vm.profile.collectAsStateWithLifecycle(initialValue = null as BusinessProfile?)
    val caps by vm.capabilities.collectAsStateWithLifecycle(initialValue = emptyList<CapabilityStatement>())
    val quotes by vm.quotations.collectAsStateWithLifecycle(initialValue = emptyList<QuotationWithItems>())
    val tenders by vm.opportunities.collectAsStateWithLifecycle(initialValue = emptyList<OpportunityWithRequirements>())
    val readiness by vm.readiness.collectAsStateWithLifecycle(initialValue = emptyList<ReadinessItem>())
    var pending by remember { mutableStateOf<Pending?>(null) }

    ScreenScaffold("Saved documents", onBack) {
        SectionTitle("Business profile")
        RecordCard(
            title = profile?.businessName?.ifBlank { "Profile (no name yet)" } ?: "No profile yet",
            lines = listOf(if (profile == null) "Tap to create it." else "Updated ${DateUtil.displayMillis(profile?.updatedAt ?: 0L)}"),
            onOpen = { go("profile") },
            onDelete = null
        )

        SectionTitle("Capability statements")
        if (caps.isEmpty()) Hint("None yet. Create one from the home screen.")
        caps.forEach { c ->
            RecordCard(
                title = c.businessName.ifBlank { "Capability statement" },
                lines = listOf("Updated ${DateUtil.displayMillis(c.updatedAt)}"),
                onOpen = { go("capability/${c.id}") },
                onDelete = { pending = Pending.Capability(c.id, c.businessName.ifBlank { "Capability statement" }) }
            )
        }
        TextButton(onClick = { go("capability/0") }) { Text("New capability statement") }

        SectionTitle("Quotations")
        if (quotes.isEmpty()) Hint("None yet. Create one from the home screen.")
        quotes.forEach { qi ->
            val q = qi.quotation
            val total = qi.totals()?.let { Money.formatKina(it.totalMinor) } ?: "-"
            RecordCard(
                title = "${q.quotationNumber}  -  ${q.clientName}",
                lines = listOf("Total $total   Date ${DateUtil.display(q.issueDate)}"),
                onOpen = { go("quotation/${q.id}") },
                onDelete = { pending = Pending.Quote(q.id, q.quotationNumber) }
            )
        }
        TextButton(onClick = { go("quotation/0") }) { Text("New quotation") }

        SectionTitle("Tender opportunities")
        if (tenders.isEmpty()) Hint("None yet. Add one from the tender checklist.")
        tenders.forEach { t ->
            val p = requirementProgress(t.requirements)
            RecordCard(
                title = t.opportunity.title,
                lines = listOf("${p.complete} of ${p.applicable} requirements done (${p.percent}%)"),
                onOpen = { go("tender/${t.opportunity.id}") },
                onDelete = { pending = Pending.Tender(t.opportunity.id, t.opportunity.title) }
            )
        }
        TextButton(onClick = { go("tender/0") }) { Text("New tender opportunity") }

        SectionTitle("Readiness checklist")
        val rp = readinessProgress(readiness)
        RecordCard(
            title = "Business readiness checklist",
            lines = listOf(if (readiness.isEmpty()) "Tap to open it." else "${rp.percent}% complete (${rp.complete} of ${rp.applicable})"),
            onOpen = { go("readiness") },
            onDelete = null
        )
    }

    pending?.let { p ->
        ConfirmDialog(
            title = p.title,
            text = p.text,
            confirmLabel = "Delete",
            onConfirm = {
                when (p) {
                    is Pending.Capability -> vm.deleteCapability(p.id)
                    is Pending.Quote -> vm.deleteQuotation(p.id)
                    is Pending.Tender -> vm.deleteOpportunity(p.id)
                }
                pending = null
            },
            onDismiss = { pending = null }
        )
    }
}

@Composable
private fun RecordCard(title: String, lines: List<String>, onOpen: () -> Unit, onDelete: (() -> Unit)?) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}
