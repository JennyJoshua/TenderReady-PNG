package com.tenderready.png.pdf

import com.tenderready.png.data.CapabilityStatement
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smoke test that the Android PdfDocument path runs end to end without error. Robolectric's PdfDocument is a
 * simplified stand-in, so real PDF quality (fonts, spacing) must still be checked on a phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfWriterTest {
    @Test fun creatingAMultiPagePdfSucceeds() {
        val longText = (1..3000).joinToString(" ") { "word$it" }
        val blocks = DocumentBuilders.capabilityStatement(CapabilityStatement(businessName = "Gulf Builders Ltd", overview = longText))
        val out = ByteArrayOutputStream()
        PdfWriter.render(blocks, out)
        assertTrue(out.size() > 0)
    }

    @Test fun exporterBuildsBytes() {
        val bytes = PdfExporter.render(DocumentBuilders.capabilityStatement(CapabilityStatement(businessName = "A")))
        assertTrue(bytes.isNotEmpty())
        assertTrue(PdfExporter.fileName("quotation", "QT 0001/x").endsWith(".pdf"))
        assertTrue(!PdfExporter.fileName("quotation", "QT 0001/x").contains("/"))
    }
}
