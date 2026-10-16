package com.tenderready.png.pdf

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

/** Draws laid-out pages with Android's built-in PdfDocument. Free, local, no extra libraries. */
object PdfWriter {
    const val FOOTER = "Prepared with TenderReady PNG from information provided by the user."

    private fun paintFor(style: TextStyle): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = style.size
        typeface = if (style.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        color = Color.BLACK
    }

    private fun measurer(): TextMeasurer {
        val paints = TextStyle.values().associateWith { paintFor(it) }
        return object : TextMeasurer {
            override fun width(text: String, style: TextStyle): Float = paints.getValue(style).measureText(text)
        }
    }

    fun render(blocks: List<Block>, out: OutputStream) {
        val layout = PdfLayout(measurer())
        val pages = layout.layout(blocks)
        val paints = TextStyle.values().associateWith { paintFor(it) }
        val footerPaint = paintFor(TextStyle.SMALL).apply { color = Color.DKGRAY }
        val linePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 0.8f
        }
        val doc = PdfDocument()
        try {
            pages.forEachIndexed { i, p ->
                val info = PdfDocument.PageInfo.Builder(layout.pageWidth.toInt(), layout.pageHeight.toInt(), i + 1).create()
                val page = doc.startPage(info)
                val c = page.canvas
                for (t in p.texts) {
                    val paint = paints.getValue(t.style)
                    val right = t.rightEdge
                    if (right != null) c.drawText(t.text, right - paint.measureText(t.text), t.y, paint)
                    else c.drawText(t.text, t.x, t.y, paint)
                }
                for (l in p.lines) c.drawLine(l.x1, l.y1, l.x2, l.y2, linePaint)
                val footerY = layout.pageHeight - layout.margin + 6f
                c.drawText(FOOTER, layout.margin, footerY, footerPaint)
                val pageLabel = "Page ${i + 1} of ${pages.size}"
                c.drawText(pageLabel, layout.pageWidth - layout.margin - footerPaint.measureText(pageLabel), footerY + 12f, footerPaint)
                doc.finishPage(page)
            }
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }
}
