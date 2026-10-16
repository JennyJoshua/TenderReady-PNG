package com.tenderready.png.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    var confirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    ScreenScaffold("Settings and privacy", onBack) {
        InfoCard {
            Text("TenderReady PNG")
            Text("Version $version")
        }

        SectionTitle("Privacy")
        InfoCard {
            Text("Your business details, quotations, capability statements and checklists are stored only on this phone, in the app's private storage.")
            Text("The app has no account, no analytics, no advertising and no server. It does not send your data anywhere.")
            Text("The app does not encrypt its stored data. Anyone who can unlock your phone and open the app can see it, so use a phone lock.")
            Text("PDFs you share or save become files outside the app. You control where they go, and deleting app data does not remove copies you already saved or sent.")
        }

        SectionTitle("Language")
        InfoCard {
            Text("English only for now.")
            Text("Tok Pisin is planned. All screen text is gathered in the screen files so it can be moved to string resources later (see docs/product-specification.md).")
        }

        SectionTitle("Your data")
        Hint("Documents are exported one at a time from the quotation and capability statement screens using Share PDF or Save PDF.")
        BigButton("Delete all my business data", onClick = { confirm = true }, outlined = true)
        val m = message
        if (m != null) Text(m)

        SectionTitle("About")
        Hint(
            "TenderReady PNG helps you organise and prepare documents. It does not give legal or tax advice, does not " +
                "confirm compliance or tender eligibility, and cannot promise you will win any contract."
        )
    }

    if (confirm) {
        ConfirmDialog(
            title = "Delete all data?",
            text = "This permanently deletes your profile, capability statements, quotations, tender opportunities and checklist from this phone. " +
                "It cannot be undone. PDFs you already saved or shared will not be deleted.",
            confirmLabel = "Delete everything",
            onConfirm = {
                confirm = false
                vm.deleteAllData { message = "All business data was deleted from this phone." }
            },
            onDismiss = { confirm = false }
        )
    }
}
