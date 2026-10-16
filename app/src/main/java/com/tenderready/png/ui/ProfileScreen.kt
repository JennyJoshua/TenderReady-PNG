package com.tenderready.png.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.tenderready.png.core.Validation
import com.tenderready.png.data.BusinessProfile
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private val registrationOptions = listOf(
    "" to "Not stated",
    "Registered" to "Registered",
    "Application in progress" to "Application in progress",
    "Not registered yet" to "Not registered yet"
)
private val tinOptions = listOf(
    "" to "Not stated",
    "Has a TIN" to "Has a TIN",
    "Applying for a TIN" to "Applying for a TIN",
    "No TIN yet" to "No TIN yet"
)

@Composable
fun ProfileScreen(vm: MainViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loaded by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var products by rememberSaveable { mutableStateOf("") }
    var areas by rememberSaveable { mutableStateOf("") }
    var years by rememberSaveable { mutableStateOf("") }
    var registration by rememberSaveable { mutableStateOf("") }
    var tin by rememberSaveable { mutableStateOf("") }
    var quals by rememberSaveable { mutableStateOf("") }
    var refs by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!loaded) {
            vm.repo.getProfile()?.let {
                name = it.businessName; type = it.businessType; location = it.location
                contact = it.contactPerson; phone = it.phone; email = it.email
                products = it.productsServices; areas = it.areasServed; years = it.yearsExperience
                registration = it.registrationStatus; tin = it.tinStatus
                quals = it.qualifications; refs = it.references
            }
            loaded = true
        }
    }

    ScreenScaffold("Business profile", onBack) {
        Hint("Fill in what you can. You can save an incomplete profile and finish later. Nothing is shared outside this phone.")
        FormField("Business name", name, { name = it })
        FormField("Business type", type, { type = it }, hint = "For example: carpentry, cleaning, catering, transport")
        FormField("Business location", location, { location = it })
        FormField("Contact person", contact, { contact = it })
        FormField("Phone number", phone, { phone = it }, error = errors["phone"], keyboard = KeyboardType.Phone)
        FormField("Email address (optional)", email, { email = it }, error = errors["email"], keyboard = KeyboardType.Email)
        FormField("Products and services", products, { products = it }, singleLine = false, minLines = 3)
        FormField("Areas served", areas, { areas = it })
        FormField("Years of experience", years, { years = it }, error = errors["years"], keyboard = KeyboardType.Number)
        Text("Registration status (optional)")
        RadioGroup(registrationOptions, registration) { registration = it }
        Text("TIN status (optional)")
        RadioGroup(tinOptions, tin) { tin = it }
        FormField("Relevant qualifications", quals, { quals = it }, singleLine = false, minLines = 3)
        FormField("References or previous projects", refs, { refs = it }, singleLine = false, minLines = 3)

        BigButton("Save profile", onClick = {
            val p = BusinessProfile(
                businessName = name.trim(), businessType = type.trim(), location = location.trim(),
                contactPerson = contact.trim(), phone = phone.trim(), email = email.trim(),
                productsServices = products.trim(), areasServed = areas.trim(), yearsExperience = years.trim(),
                registrationStatus = registration, tinStatus = tin,
                qualifications = quals.trim(), references = refs.trim()
            )
            val e = Validation.profileErrors(p)
            errors = e
            if (e.isEmpty()) {
                scope.launch {
                    vm.repo.saveProfile(p)
                    message = "Profile saved on this phone."
                }
            } else {
                message = null
            }
        })
        val m = message
        if (m != null) Text(m)
        Hint("Unsaved changes are lost if you leave this screen. Tap Save profile first.")
    }
}

