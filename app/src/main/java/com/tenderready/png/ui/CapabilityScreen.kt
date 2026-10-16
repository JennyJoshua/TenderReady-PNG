package com.tenderready.png.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tenderready.png.core.Validation
import com.tenderready.png.data.CapabilityStatement
import com.tenderready.png.data.SaveResult
import com.tenderready.png.data.businessContactLine
import kotlinx.coroutines.launch
import com.tenderready.png.pdf.DocumentBuilders
import com.tenderready.png.pdf.PdfExporter

@Composable
fun CapabilityScreen(vm: MainViewModel, id: Long, onBack: () -> Unit) {
    var loaded by rememberSaveable { mutableStateOf(false) }
    var currentId by rememberSaveable { mutableStateOf(id) }
    var createdAt by rememberSaveable { mutableStateOf(0L) }
    var name by rememberSaveable { mutableStateOf("") }
    var overview by rememberSaveable { mutableStateOf("") }
    var products by rememberSaveable { mutableStateOf("") }
    var experience by rememberSaveable { mutableStateOf("") }
    var quals by rememberSaveable { mutableStateOf("") }
    var refs by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!loaded) {
            if (id == 0L) {
                // Start from the saved profile. Only details the user entered are copied; nothing is invented.
                vm.repo.getProfile()?.let { p ->
                    name = p.businessName
                    overview = listOfNotNull(
                        p.businessType.takeIf { it.isNotBlank() }?.let { "Business type: $it" },
                        p.location.takeIf { it.isNotBlank() }?.let { "Location: $it" },
                        p.areasServed.takeIf { it.isNotBlank() }?.let { "Areas served: $it" }
                    ).joinToString("\n")
                    products = p.productsServices
                    experience = p.yearsExperience.takeIf { it.isNotBlank() }
                        ?.let { "$it years of experience (as stated by the business)." } ?: ""
                    quals = p.qualifications
                    refs = p.references
                    contact = businessContactLine(p) + (if (p.location.isNotBlank()) "\nLocation: ${p.location}" else "")
                }
            } else {
                vm.repo.getCapability(id)?.let { c ->
                    createdAt = c.createdAt
                    name = c.businessName; overview = c.overview; products = c.productsServices
                    experience = c.experience; quals = c.qualifications; refs = c.references
                    contact = c.contactDetails
                }
            }
            loaded = true
        }
    }

    /** Validates and saves. Returns the saved id, or null when something must be fixed. */
    suspend fun saveNow(): Long? {
        val errs = Validation.capabilityErrors(name)
        errors = errs
        if (errs.isNotEmpty()) return null
        val c = CapabilityStatement(
            id = currentId, businessName = name.trim(), overview = overview.trim(),
            productsServices = products.trim(), experience = experience.trim(),
            qualifications = quals.trim(), references = refs.trim(), contactDetails = contact.trim(),
            createdAt = createdAt
        )
        return when (val r = vm.repo.saveCapability(c)) {
            is SaveResult.Saved -> {
                currentId = r.id
                message = "Capability statement saved."
                r.id
            }
            is SaveResult.Failed -> {
                errors = listOf(r.message)
                null
            }
        }
    }

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    ScreenScaffold("Capability statement", onBack) {
        if (!loaded) {
            Text("Loading...")
            return@ScreenScaffold
        }
        Hint("Edit any text before exporting. Only write what is true. The PDF will say it is based on information you provided.")
        FormField("Business name", name, { name = it })
        FormField("Business overview", overview, { overview = it }, singleLine = false, minLines = 4)
        FormField("Products or services", products, { products = it }, singleLine = false, minLines = 4)
        FormField("Experience", experience, { experience = it }, singleLine = false, minLines = 3)
        FormField("Qualifications", quals, { quals = it }, singleLine = false, minLines = 3)
        FormField("References and previous projects", refs, { refs = it }, singleLine = false, minLines = 3)
        FormField("Contact details", contact, { contact = it }, singleLine = false, minLines = 3)
        ErrorList(errors)
        BigButton("Save capability statement", onClick = {
            scope.launch { saveNow() }
        })
        val m = message
        if (m != null) Text(m)
        SectionTitle("Create PDF")
        ExportBar(
            fileName = PdfExporter.fileName("capability-statement", name.ifBlank { "business" }),
            prepare = {
                val savedId = saveNow()
                if (savedId == null) null
                else vm.repo.getCapability(savedId)?.let { DocumentBuilders.capabilityStatement(it) }
            }
        )
        Hint("Unsaved changes are lost if you leave this screen. The PDF is made from the saved record.")
    }
}
