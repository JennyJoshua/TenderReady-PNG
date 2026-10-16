package com.tenderready.png.pdf

import com.tenderready.png.data.CapabilityStatement
import com.tenderready.png.data.Quotation
import com.tenderready.png.data.QuotationItem
import com.tenderready.png.data.QuotationWithItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure JVM tests: layout and document content. No Android classes needed. */
class PdfLayoutTest {
    private val measurer = object : TextMeasurer {
        override fun width(text: String, style: TextStyle) = text.length * style.size * 0.5f
    }
    private val layout get() = PdfLayout(measurer)
    private val bottomLimit = 842f - 48f - 28f

    private fun allText(pages: List<PageContent>) = pages.flatMap { it.texts }.map { it.text }

    private fun words(pages: List<PageContent>) = allText(pages).joinToString(" ").split(" ").filter { it.isNotEmpty() }

    @Test fun shortDocumentIsOnePage() {
        val pages = layout.layout(listOf(Block.Text("Hello", TextStyle.TITLE), Block.Text("Short body.")))
        assertEquals(1, pages.size)
        assertTrue(allText(pages).contains("Hello"))
    }

    @Test fun longParagraphIsNotTruncatedAndSpansPages() {
        val original = (1..1500).map { "word$it" }
        val pages = layout.layout(listOf(Block.Text(original.joinToString(" "))))
        assertTrue("expected several pages", pages.size > 1)
        assertEquals(original, words(pages))
    }

    @Test fun nothingIsDrawnBelowThePageLimit() {
        val pages = layout.layout(listOf(Block.Text((1..2000).joinToString(" ") { "w$it" })))
        pages.forEach { p -> p.texts.forEach { assertTrue(it.y <= bottomLimit) } }
    }

    @Test fun veryLongUnbrokenWordIsBrokenNotClipped() {
        val long = "x".repeat(300)
        val pages = layout.layout(listOf(Block.Text(long)))
        assertEquals(long, allText(pages).joinToString(""))
    }

    @Test fun explicitNewlinesAreKept() {
        val pages = layout.layout(listOf(Block.Text("line one\nline two")))
        assertEquals(listOf("line one", "line two"), allText(pages))
    }

    @Test fun headingDoesNotSitAloneAtPageBottom() {
        val filler = Block.Text((1..560).joinToString(" ") { "filler$it" })
        val pages = layout.layout(listOf(filler, Block.Text("Section heading", TextStyle.HEADING), Block.Text("Body text after heading.")))
        val headingPage = pages.indexOfFirst { p -> p.texts.any { it.text == "Section heading" } }
        assertTrue(pages[headingPage].texts.any { it.text.startsWith("Body text") })
    }

    @Test fun businessNameAppearsInCapabilityStatement() {
        val c = CapabilityStatement(businessName = "Gulf Builders Ltd", overview = "We build houses.")
        val pages = layout.layout(DocumentBuilders.capabilityStatement(c))
        assertTrue(allText(pages).any { it.contains("Gulf Builders Ltd") })
        assertTrue(words(pages).joinToString(" ").contains("based on information provided by the business owner"))
    }

    @Test fun emptySectionsAreSkippedAndNothingIsInvented() {
        val blocks = DocumentBuilders.capabilityStatement(CapabilityStatement(businessName = "A", overview = "Only this."))
        val headings = blocks.filterIsInstance<Block.Text>().filter { it.style == TextStyle.HEADING }.map { it.text }
        assertEquals(listOf("Business overview"), headings)
    }

    @Test fun longCapabilityStatementKeepsAllContentOverManyPages() {
        val refs = (1..2000).joinToString(" ") { "ref$it" }
        val pages = layout.layout(DocumentBuilders.capabilityStatement(CapabilityStatement(businessName = "B", references = refs)))
        assertTrue(pages.size > 1)
        val out = words(pages)
        assertTrue(out.containsAll((1..2000).map { "ref$it" }))
    }

    private fun quote(items: List<QuotationItem>, tax: Boolean = false) = QuotationWithItems(
        Quotation(
            id = 1, quotationNumber = "QT-0001", businessName = "Gulf Builders Ltd", issueDate = "2026-10-17",
            clientName = "Client Ltd", taxEnabled = tax, taxLabel = "GST", taxRateBp = 1000, validityDays = 30,
            paymentTerms = "50% deposit"
        ),
        items
    )

    @Test fun quotationTotalsAreFormattedInKina() {
        val blocks = DocumentBuilders.quotation(quote(listOf(QuotationItem(position = 0, description = "Labour", quantityMilli = 1000, unit = "job", unitPriceMinor = 10000)), tax = true))
        val totals = blocks.filterIsInstance<Block.Total>()
        assertEquals("K100.00", totals.first { it.label == "Subtotal" }.value)
        assertEquals("K10.00", totals.first { it.label == "GST (10%)" }.value)
        assertEquals("K110.00", totals.first { it.label == "TOTAL (PGK)" }.value)
    }

    @Test fun noTaxLineWhenTaxIsOff() {
        val blocks = DocumentBuilders.quotation(quote(listOf(QuotationItem(position = 0, description = "Labour", quantityMilli = 1000, unitPriceMinor = 10000))))
        assertFalse(blocks.filterIsInstance<Block.Total>().any { it.label.startsWith("GST") || it.label.startsWith("Tax") })
    }

    @Test fun manyLineItemsRunOntoSeveralPagesWithoutLoss() {
        val items = (1..80).map { QuotationItem(position = it, description = "Item description number $it", quantityMilli = 2500, unit = "bag", unitPriceMinor = 1999) }
        val pages = layout.layout(DocumentBuilders.quotation(quote(items)))
        assertTrue(pages.size > 1)
        val text = allText(pages)
        (1..80).forEach { n -> assertTrue("missing item $n", text.any { it.contains("number $n") || it.endsWith("number $n") }) }
        assertTrue(text.any { it.contains("Gulf Builders Ltd") })
    }
}
