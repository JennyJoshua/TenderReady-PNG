package com.tenderready.png.pdf

enum class TextStyle(val size: Float, val bold: Boolean) {
    TITLE(22f, true), HEADING(13f, true), BODY(11f, false), BOLD(11f, true), SMALL(9f, false);

    val lineHeight: Float get() = size * 1.4f
}

sealed class Block {
    data class Text(val text: String, val style: TextStyle = TextStyle.BODY) : Block()
    data class Field(val label: String, val value: String) : Block()
    /** A table row. Five cells use the quotation column widths; other counts share the width equally. */
    data class Row(val cells: List<String>, val header: Boolean = false) : Block()
    data class Total(val label: String, val value: String, val bold: Boolean = false) : Block()
    data class Space(val height: Float) : Block()
    object Rule : Block()
}

data class DrawText(val text: String, val x: Float, val y: Float, val style: TextStyle, val rightEdge: Float? = null)
data class DrawLine(val x1: Float, val y1: Float, val x2: Float, val y2: Float)
class PageContent {
    val texts = mutableListOf<DrawText>()
    val lines = mutableListOf<DrawLine>()
}

interface TextMeasurer {
    fun width(text: String, style: TextStyle): Float
}

/**
 * Pure Kotlin page layout (no Android classes), so it can be unit tested on a normal JVM.
 * Long text wraps and continues onto new pages; nothing is clipped or dropped.
 */
class PdfLayout(
    private val measurer: TextMeasurer,
    val pageWidth: Float = 595f,
    val pageHeight: Float = 842f,
    val margin: Float = 48f,
    footerSpace: Float = 28f
) {
    private val contentWidth = pageWidth - 2 * margin
    private val bottom = pageHeight - margin - footerSpace
    private val pages = mutableListOf<PageContent>()
    private var page = PageContent()
    private var y = margin

    private val fiveColumns = floatArrayOf(0.38f, 0.10f, 0.12f, 0.20f, 0.20f)
    private val labelWidth = 130f

    fun layout(blocks: List<Block>): List<PageContent> {
        pages.clear()
        page = PageContent()
        pages.add(page)
        y = margin
        for (b in blocks) {
            when (b) {
                is Block.Text -> text(b)
                is Block.Field -> field(b)
                is Block.Row -> row(b)
                is Block.Total -> total(b)
                is Block.Space -> y += b.height
                Block.Rule -> {
                    ensure(10f)
                    page.lines.add(DrawLine(margin, y + 4f, margin + contentWidth, y + 4f))
                    y += 10f
                }
            }
        }
        return pages.toList()
    }

    private fun newPage() {
        page = PageContent()
        pages.add(page)
        y = margin
    }

    private fun ensure(h: Float) {
        if (y + h > bottom && y > margin) newPage()
    }

    private fun text(b: Block.Text) {
        val lines = wrap(b.text, b.style, contentWidth)
        if (b.style == TextStyle.HEADING || b.style == TextStyle.TITLE) ensure(b.style.lineHeight * 3f)
        for (line in lines) {
            ensure(b.style.lineHeight)
            if (line.isNotEmpty()) page.texts.add(DrawText(line, margin, y + b.style.size, b.style))
            y += b.style.lineHeight
        }
    }

    private fun field(b: Block.Field) {
        val lines = wrap(b.value, TextStyle.BODY, contentWidth - labelWidth)
        val lh = TextStyle.BODY.lineHeight
        lines.forEachIndexed { i, line ->
            ensure(lh)
            if (i == 0) page.texts.add(DrawText(b.label, margin, y + TextStyle.BOLD.size, TextStyle.BOLD))
            if (line.isNotEmpty()) page.texts.add(DrawText(line, margin + labelWidth, y + TextStyle.BODY.size, TextStyle.BODY))
            y += lh
        }
    }

    private fun row(b: Block.Row) {
        val n = b.cells.size
        if (n == 0) return
        val style = if (b.header) TextStyle.BOLD else TextStyle.BODY
        val fractions = if (n == 5) fiveColumns else FloatArray(n) { 1f / n }
        val pad = 4f
        val wrapped = b.cells.mapIndexed { i, c -> wrap(c, style, fractions[i] * contentWidth - 2 * pad) }
        val rowLines = wrapped.maxOf { it.size }
        val height = rowLines * style.lineHeight + 6f
        ensure(height)
        var x = margin
        for (i in 0 until n) {
            val colW = fractions[i] * contentWidth
            val rightAlign = n == 5 && (i == 1 || i == 3 || i == 4)
            wrapped[i].forEachIndexed { li, line ->
                if (line.isNotEmpty()) {
                    val baseline = y + 2f + li * style.lineHeight + style.size
                    page.texts.add(
                        if (rightAlign) DrawText(line, x, baseline, style, rightEdge = x + colW - pad)
                        else DrawText(line, x + pad, baseline, style)
                    )
                }
            }
            x += colW
        }
        page.lines.add(DrawLine(margin, y + height, margin + contentWidth, y + height))
        y += height
    }

    private fun total(b: Block.Total) {
        val style = if (b.bold) TextStyle.BOLD else TextStyle.BODY
        val labelX = margin + contentWidth * 0.40f
        val lines = wrap(b.label, style, contentWidth * 0.38f)
        lines.forEachIndexed { i, line ->
            ensure(style.lineHeight)
            if (line.isNotEmpty()) page.texts.add(DrawText(line, labelX, y + style.size, style))
            if (i == 0) page.texts.add(DrawText(b.value, labelX, y + style.size, style, rightEdge = margin + contentWidth))
            y += style.lineHeight
        }
    }

    internal fun wrap(text: String, style: TextStyle, maxWidth: Float): List<String> {
        val out = mutableListOf<String>()
        for (para in text.replace("\r", "").split("\n")) {
            if (para.isBlank()) {
                out.add("")
                continue
            }
            var line = StringBuilder()
            for (word in para.trim().split(Regex("\\s+"))) {
                var w = word
                while (measurer.width(w, style) > maxWidth) {
                    if (line.isNotEmpty()) {
                        out.add(line.toString())
                        line = StringBuilder()
                    }
                    var cut = w.length
                    while (cut > 1 && measurer.width(w.substring(0, cut), style) > maxWidth) cut--
                    out.add(w.substring(0, cut))
                    w = w.substring(cut)
                }
                if (w.isEmpty()) continue
                val candidate = if (line.isEmpty()) w else "$line $w"
                if (line.isNotEmpty() && measurer.width(candidate, style) > maxWidth) {
                    out.add(line.toString())
                    line = StringBuilder(w)
                } else {
                    line = StringBuilder(candidate)
                }
            }
            if (line.isNotEmpty()) out.add(line.toString())
        }
        return if (out.isEmpty()) listOf("") else out
    }
}
