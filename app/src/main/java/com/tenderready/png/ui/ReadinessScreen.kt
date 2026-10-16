package com.tenderready.png.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenderready.png.core.Validation
import com.tenderready.png.data.ReadinessDefaults
import com.tenderready.png.data.ReadinessItem
import com.tenderready.png.data.ReadinessStatus
import com.tenderready.png.data.readinessProgress

@Composable
fun ReadinessScreen(vm: MainViewModel, onBack: () -> Unit) {
    val items by vm.readiness.collectAsStateWithLifecycle(initialValue = emptyList<ReadinessItem>())
    var dialogOpen by remember { mutableStateOf(false) }
    var dialogItem by remember { mutableStateOf<ReadinessItem?>(null) }
    var deleteItem by remember { mutableStateOf<ReadinessItem?>(null) }

    LaunchedEffect(Unit) { vm.ensureReadinessSeeded() }

    ScreenScaffold("Readiness checklist", onBack) {
        Hint(CHECKLIST_DISCLAIMER)
        ProgressBlock(readinessProgress(items))
        BigButton("Add your own item", onClick = {
            dialogItem = null
            dialogOpen = true
        })
        val categories = ReadinessDefaults.categories + items.map { it.category }.filter { it !in ReadinessDefaults.categories }.distinct()
        categories.forEach { cat ->
            val inCat = items.filter { it.category == cat }
            if (inCat.isNotEmpty()) {
                SectionTitle(cat)
                inCat.forEach { item ->
                    Card(Modifier.fillMaxWidth().clickable {
                        dialogItem = item
                        dialogOpen = true
                    }) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall)
                            Text("Status: ${ReadinessStatus.from(item.status).label}")
                            if (item.notes.isNotBlank()) Text(item.notes)
                            Hint("Tap to update")
                        }
                    }
                }
            }
        }
    }

    if (dialogOpen) {
        ReadinessDialog(
            initial = dialogItem,
            categories = ReadinessDefaults.categories,
            onDismiss = { dialogOpen = false },
            onSave = {
                dialogOpen = false
                vm.saveReadinessItem(it)
            },
            onDelete = {
                dialogOpen = false
                deleteItem = dialogItem
            }
        )
    }
    deleteItem?.let { item ->
        ConfirmDialog(
            title = "Remove item?",
            text = "\"${item.title}\" will be removed from your checklist.",
            confirmLabel = "Remove",
            onConfirm = {
                vm.deleteReadinessItem(item.id)
                deleteItem = null
            },
            onDismiss = { deleteItem = null }
        )
    }
}

@Composable
private fun ReadinessDialog(
    initial: ReadinessItem?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (ReadinessItem) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: categories.first()) }
    var status by remember { mutableStateOf(ReadinessStatus.from(initial?.status ?: ReadinessStatus.NOT_STARTED.name)) }
    var menuOpen by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add item" else "Update item") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField("Item", title, { title = it })
                Text("Category")
                OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text(category) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    (categories + listOf(category)).distinct().forEach { c ->
                        DropdownMenuItem(text = { Text(c) }, onClick = {
                            category = c
                            menuOpen = false
                        })
                    }
                }
                RadioGroup(ReadinessStatus.values().map { it to it.label }, status) { status = it }
                FormField("Notes (optional)", notes, { notes = it }, singleLine = false, minLines = 2)
                ErrorList(errors)
                if (initial != null) TextButton(onClick = onDelete) { Text("Remove this item") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val errs = Validation.readinessErrors(title)
                errors = errs
                if (errs.isEmpty()) {
                    onSave(
                        ReadinessItem(
                            id = initial?.id ?: 0L, category = category, title = title.trim(),
                            notes = notes.trim(), status = status.name, sortOrder = initial?.sortOrder ?: 0
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
