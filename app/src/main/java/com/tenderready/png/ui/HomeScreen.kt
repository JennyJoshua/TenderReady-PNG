package com.tenderready.png.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenderready.png.core.Validation
import com.tenderready.png.data.BusinessProfile
import com.tenderready.png.data.CapabilityStatement
import com.tenderready.png.data.QuotationWithItems

@Composable
fun HomeScreen(vm: MainViewModel, go: (String) -> Unit) {
    val profile by vm.profile.collectAsStateWithLifecycle(initialValue = null as BusinessProfile?)
    val quotes by vm.quotations.collectAsStateWithLifecycle(initialValue = emptyList<QuotationWithItems>())
    val caps by vm.capabilities.collectAsStateWithLifecycle(initialValue = emptyList<CapabilityStatement>())

    ScreenScaffold("TenderReady PNG", onBack = null) {
        Text("Get your PNG business ready to quote, bid, and win contracts.", style = MaterialTheme.typography.titleMedium)

        InfoCard {
            Text("Business profile", style = MaterialTheme.typography.titleMedium)
            val p = profile
            if (p == null || p.businessName.isBlank()) {
                Text("Not set up yet. Fill it in once and reuse it in your documents.")
            } else {
                Text(p.businessName)
                val missing = Validation.profileMissing(p)
                if (missing.isNotEmpty()) Text("Still missing: " + missing.joinToString(", "))
                else Text("Key details are filled in.")
            }
        }
        BigButton(if (profile == null) "Create business profile" else "Edit business profile", onClick = { go("profile") })
        BigButton("Create quotation", onClick = { go("quotation/0") })
        BigButton("Create capability statement", onClick = { go("capability/0") })
        BigButton("Tender checklist", onClick = { go("tenders") })
        BigButton("Business readiness checklist", onClick = { go("readiness") })
        BigButton("Saved documents", onClick = { go("saved") }, outlined = true)
        BigButton("Settings and privacy", onClick = { go("settings") }, outlined = true)

        if (quotes.isEmpty() && caps.isEmpty()) {
            Hint("No documents saved yet. Start with your business profile, then create a quotation or capability statement.")
        } else {
            Hint("Saved so far: ${quotes.size} quotation(s), ${caps.size} capability statement(s).")
        }
        Hint(
            "TenderReady PNG helps you organise and prepare documents. It does not confirm legal compliance or " +
                "tender eligibility, and it cannot promise you will win any contract. Your information stays on this phone."
        )
    }
}
