package com.tenderready.png.pdf

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Save = user picks the location with the system file picker (no storage permission needed).
 * Share = temporary copy in the app cache, shared through FileProvider and the Android Sharesheet.
 * Nothing is ever sent to a server by this app.
 */
object PdfExporter {
    fun render(blocks: List<Block>): ByteArray {
        val out = ByteArrayOutputStream()
        PdfWriter.render(blocks, out)
        return out.toByteArray()
    }

    fun fileName(prefix: String, id: String): String {
        val cleaned = "$prefix-$id".replace(Regex("[^A-Za-z0-9_-]"), "-").lowercase()
        return "$cleaned.pdf"
    }

    fun saveTo(context: Context, uri: Uri, bytes: ByteArray): Boolean = try {
        context.contentResolver.openOutputStream(uri, "wt")?.use {
            it.write(bytes)
            true
        } ?: false
    } catch (e: Exception) {
        false
    }

    private fun cacheUri(context: Context, bytes: ByteArray, name: String): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, name)
        file.writeBytes(bytes)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun share(context: Context, bytes: ByteArray, name: String) {
        val uri = cacheUri(context, bytes, name)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share PDF"))
    }

    /** Opens the PDF in any installed PDF viewer. Returns false when none is installed. */
    fun view(context: Context, bytes: ByteArray, name: String): Boolean {
        val uri = cacheUri(context, bytes, name)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
