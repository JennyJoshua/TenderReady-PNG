package com.tenderready.png.pdf

import com.tenderready.png.data.CapabilityStatement
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfWriterTest {
    @Test fun fileNamesAreSafeAndEndWithPdf() {
        val name = PdfExporter.fileName("quotation", "QT 0001/x")
        assertTrue(name.endsWith(".pdf"))
        assertFalse(name.contains("/"))
        assertFalse(name.contains(" "))
        assertTrue(name == name.lowercase())
    }

    /**
     * Robolectric's stand-in for android.graphics.pdf.PdfDocument throws IllegalStateException on the first CI run,
     * so the Android PDF drawing step cannot be checked on a plain JVM. Page layout, wrapping and content are covered
     * by PdfLayoutTest. The real PDF output must be checked on a phone (see REVIEW.md, manual test 5 and 6).
     */
    @Ignore("PdfDocument is not reliable under Robolectric; verify real PDF output on a phone")
    @Test fun creatingAMultiPagePdfSucceeds() {
        val longText = (1..3000).joinToString(" ") { "word$it" }
        val blocks = DocumentBuilders.capabilityStatement(CapabilityStatement(businessName = "Gulf Builders Ltd", overview = longText))
        val out = ByteArrayOutputStream()
        PdfWriter.render(blocks, out)
        assertTrue(out.size() > 0)
    }
}
