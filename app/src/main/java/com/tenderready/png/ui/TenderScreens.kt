package com.tenderready.png.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenderready.png.core.DateUtil
import com.tenderready.png.core.Validation
import com.tenderready.png.data.OpportunityWithRequirements
import com.tenderready.png.data.RequirementStatus
import com.tenderready.png.data.SaveResult
import com.tenderready.png.data.TenderOpportunity
import com.tenderready.png.data.TenderRequirement
import com.tenderready.png.data.requirementProgress
import kotlinx.coroutines.launch

@Composable
fun TendersListScreen(vm: MainViewModel, go: (String) -> Unit, onBack: () -> Unit) {
    val list by vm.opportunities.collectAsStateWithLifecycle(initialValue = emptyList<OpportunityWithRequirements>())
    ScreenScaffold("Tender checklist", onBack) {
        Hint("Keep track of what a tender asks for. Add the requirements yourself from the tender documents.")
        BigButton("New tender opportunity", onClick = { go("tender/0") })
        if (list.isEmpty()) {
            Hint("No opportunities yet. Tap the button above to add your first one.")
        }
        list.forEach { o ->
            val p = requirementProgress(o.requirements)
            Card(Modifier.fillMaxWidth().clickable { go("tender/${o.opportunity.id}") }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(o.opportunity.title, style = MaterialTheme.typography.titleMedium)
                    if (o.opportunity.issuer.isNotBlank()) Text(o.opportunity.issuer)
                    if (o.opportunity.deadline.isNotBlank()) Text("Deadline: ${DateUtil.display(o.opportunity.deadline)}")
                    Text("${p.complete} of ${p.applicable} requirements done (${p.percent}%)")
                }
            }
        }
        Hint(CHECKLIST_DISCLAIMER)
    }
}

@Composable
fun TenderDetailScreen(vm: MainViewModel, id: Long, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var currentId by rememberSaveable { mutableStateOf(id) }
    var createdAt by rememberSaveable { mutableStateOf(0L) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var issuer by rememberSaveable { mutableStateOf("") }
    var reference by rememberSaveable { mutableStateOf("") }
    var deadline by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    var dialogOpen by remember { mutableStateOf(false) }
    var dialogItem by remember { mutableStateOf<TenderRequirement?>(null) }
    var deleteRequirement by remember { mutableStateOf<TenderRequirement?>(null) }
    var deleteOpportunity by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!loaded) {
            if (id != 0L) {
                vm.repo.getOpportunity(id)?.opportunity?.let {
                    title = it.title; issuer = it.issuer; reference = it.referenceNumber
                    deadline = it.deadline; notes = it.notes; createdAt = it.createdAt
                }
            }
            loaded = true
        }
    }

    val flow = remember(currentId) { vm.repo.observeOpportunity(currentId) }
    val opp by flow.collectAsStateWithLifecycle(initialValue = null as OpportunityWithRequirements?)
    val requirements = (opp?.requirements ?: emptyList()).sortedBy { it.id }

    ScreenScaffold(if (currentId == 0L) "New opportunity" else "Tender opportunity", onBack) {
        if (!loaded) {
            Text("Loading...")
            return@ScreenScaffold
        }
        FormField("Opportunity title", title, { title = it })
        FormField("Issuing organisation", issuer, { issuer = it })
        FormField("Reference number (optional)", reference, { reference = it })
        FormField("Submission deadline (optional)", deadline, { deadline = it }, hint = "Format: YYYY-MM-DD")
        FormField("Notes", notes, { notes = it }, singleLine = false, minLines = 3)
        ErrorList(errors)
        BigButton("Save opportunity", onClick = {
            val errs = Validation.opportunityErrors(title, deadline)
            errors = errs
            if (errs.isEmpty()) {
                scope.launch {
                    val o = TenderOpportunity(
                        id = currentId, title = title.trim(), issuer = issuer.trim(), referenceNumber = reference.trim(),
                        deadline = deadline.trim(), notes = notes.trim(), createdAt = createdAt
                    )
                    when (val r = vm.repo.saveOpportunity(o)) {
                        is SaveResult.Saved -> {
                            currentId = r.id
                            message = "Opportunity saved."
                        }
                        is SaveResult.Failed -> errors = listOf(r.message)
                    }
                }
            }
        })
        val m = message
        if (m != null) Text(m)

        if (currentId == 0L) {
            Hint("Save the opportunity first, then you can add its requirements.")
        } else {
            SectionTitle("Requirements")
            ProgressBlock(requirementProgress(requirements))
            BigButton("Add requirement", onClick = {
                dialogItem = null
                dialogOpen = true
            })
            if (requirements.isEmpty()) Hint("No requirements yet. Add each item the tender asks you to provide.")
            requirements.forEach { r ->
                val status = RequirementStatus.from(r.status)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(r.title, style = MaterialTheme.typography.titleMedium)
                        Text("Status: ${status.label}")
                        if (r.deadline.isNotBlank()) Text("Deadline: ${DateUtil.display(r.deadline)}")
                        if (r.notes.isNotBlank()) Text(r.notes)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            if (status == RequirementStatus.COMPLETE) {
                                TextButton(onClick = { vm.setRequirementStatus(r, RequirementStatus.OUTSTANDING) }) { Text("Reopen") }
                            } else {
                                TextButton(onClick = { vm.setRequirementStatus(r, RequirementStatus.COMPLETE) }) { Text("Mark complete") }
                            }
                            TextButton(onClick = {
                                dialogItem = r
                                dialogOpen = true
                            }) { Text("Edit") }
                            TextButton(onClick = { deleteRequirement = r }) { Text("Delete") }
                        }
                    }
                }
            }
            BigButton("Delete this opportunity", onClick = { deleteOpportunity = true }, outlined = true)
        }
        Hint(CHECKLIST_DISCLAIMER)
    }

    if (dialogOpen) {
        RequirementDialog(
            initial = dialogItem,
            onDismiss = { dialogOpen = false },
            onSave = { req ->
                dialogOpen = false
                scope.launch { vm.repo.saveRequirement(req.copy(opportunityId = currentId)) }
            }
        )
    }
    deleteRequirement?.let { r ->
        ConfirmDialog(
            title = "Delete requirement?",
            text = "\"${r.title}\" will be removed from this checklist.",
            confirmLabel = "Delete",
            onConfirm = {
                vm.deleteRequirement(r.id)
                deleteRequirement = null
            },
            onDismiss = { deleteRequirement = null }
        )
    }
    if (deleteOpportunity) {
        ConfirmDialog(
            title = "Delete opportunity?",
            text = "This opportunity and all its requirements will be deleted from this phone.",
            confirmLabel = "Delete",
            onConfirm = {
                deleteOpportunity = false
                vm.deleteOpportunity(currentId)
                onBack()
            },
            onDismiss = { deleteOpportunity = false }
        )
    }
}

@Composable
private fun RequirementDialog(initial: TenderRequirement?, onDismiss: () -> Unit, onSave: (TenderRequirement) -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var deadline by remember { mutableStateOf(initial?.deadline ?: "") }
    var status by remember { mutableStateOf(RequirementStatus.from(initial?.status ?: RequirementStatus.OUTSTANDING.name)) }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add requirement" else "Edit requirement") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField("Title", title, { title = it })
                FormField("Notes (optional)", notes, { notes = it }, singleLine = false, minLines = 2)
                FormField("Deadline (optional)", deadline, { deadline = it }, hint = "Format: YYYY-MM-DD")
                RadioGroup(RequirementStatus.values().map { it to it.label }, status) { status = it }
                ErrorList(errors)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val errs = Validation.requirementErrors(title, deadline)
                errors = errs
                if (errs.isEmpty()) {
                    onSave(
                        TenderRequirement(
                            id = initial?.id ?: 0L,
                            opportunityId = initial?.opportunityId ?: 0L,
                            title = title.trim(),
                            notes = notes.trim(),
                            status = status.name,
                            deadline = deadline.trim(),
                            createdAt = initial?.createdAt ?: 0L
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
