package com.tenderready.png.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tenderready.png.pdf.Block
import com.tenderready.png.pdf.PdfExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ExportAction { SHARE, VIEW, SAVE }

/**
 * Three export buttons. prepare() must save the record first and return the PDF blocks built from the
 * SAVED record (the database is the source of truth), or null when validation failed.
 */
@Composable
fun ExportBar(fileName: String, prepare: suspend () -> List<Block>?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val bytes = pendingBytes
        pendingBytes = null
        if (uri == null || bytes == null) {
            status = "Save cancelled."
        } else {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { PdfExporter.saveTo(context, uri, bytes) }
                status = if (ok) "PDF saved to the location you chose." else "Could not save the PDF. Try another location."
            }
        }
    }

    fun go(action: ExportAction) {
        if (busy) return
        busy = true
        status = "Creating PDF..."
        scope.launch {
            try {
                val blocks = prepare()
                if (blocks == null) {
                    status = "Please fix the problems listed above, then try again."
                } else {
                    val bytes = withContext(Dispatchers.IO) { PdfExporter.render(blocks) }
                    when (action) {
                        ExportAction.SHARE -> {
                            PdfExporter.share(context, bytes, fileName)
                            status = "Record saved. PDF ready to share."
                        }
                        ExportAction.VIEW -> {
                            status = if (PdfExporter.view(context, bytes, fileName)) "Record saved."
                            else "No PDF viewer app found. Use Share or Save instead."
                        }
                        ExportAction.SAVE -> {
                            pendingBytes = bytes
                            status = null
                            saver.launch(fileName)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = "Something went wrong while creating the PDF."
            } finally {
                busy = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BigButton("Share PDF", onClick = { go(ExportAction.SHARE) }, enabled = !busy)
        BigButton("Preview PDF in a viewer app", onClick = { go(ExportAction.VIEW) }, enabled = !busy, outlined = true)
        BigButton("Save PDF to my phone", onClick = { go(ExportAction.SAVE) }, enabled = !busy, outlined = true)
        val s = status
        if (s != null) Text(s, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}
