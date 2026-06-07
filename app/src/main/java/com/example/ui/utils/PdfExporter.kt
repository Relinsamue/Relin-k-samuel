package com.example.ui.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    enum class LineType {
        LARGE_HEADER, // For main titles from ##
        SUB_HEADER,   // For subheadings from ###
        BODY,
        BULLET,       // Bullet main point
        EMPTY
    }

    class PdfLine(val text: String, val type: LineType)

    fun exportToPdf(context: Context, documentTitle: String, content: String) {
        val document = PdfDocument()

        // Page width 595, height 842 (A4 format)
        val pageWidth = 595
        val pageHeight = 842
        var pageNum = 1

        // Margins
        val marginX = 50f
        val startY = 90f
        val maxY = 760f
        var currentY = startY

        // Direct Paints for PDF styling based on Clean Minimalism
        val headerPaint = Paint().apply {
            color = Color.parseColor("#49454F") // Charcoal Slate variant
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val mainTitlePaint = Paint().apply {
            color = Color.parseColor("#001453") // Beautiful Royal Indigo
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val largeHeaderPaint = Paint().apply {
            color = Color.parseColor("#4355B9") // Primary theme cobalt blue
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val subHeaderPaint = Paint().apply {
            color = Color.parseColor("#7C50DE") // Warm purple brand color
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.parseColor("#1C1B1F") // Standard Obsidian charcoal body
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val lineDividerPaint = Paint().apply {
            color = Color.parseColor("#E7E0EC") // Soft lilac accent gray
            strokeWidth = 1f
            isAntiAlias = true
        }

        // Helper to wrap a single paragraph into multiple lines fitting the page printable area
        fun wrapParagraph(text: String, paint: Paint, maxWidth: Float): List<String> {
            val words = text.split("\\s+".toRegex())
            val wrapped = mutableListOf<String>()
            var currentLine = StringBuilder()

            for (word in words) {
                if (word.isEmpty()) continue
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine.append(if (currentLine.isEmpty()) word else " $word")
                } else {
                    wrapped.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                wrapped.add(currentLine.toString())
            }
            return wrapped
        }

        // Parse content to support headers, bullet points, spacing etc
        val rawParagraphs = content.split("\n")
        val parsedLines = mutableListOf<PdfLine>()
        val maxTextWidth = pageWidth - (2 * marginX)

        for (para in rawParagraphs) {
            val trimmed = para.trim()
            if (trimmed.isEmpty()) {
                parsedLines.add(PdfLine("", LineType.EMPTY))
                continue
            }

            when {
                trimmed.startsWith("# ") -> {
                    val cleanText = trimmed.removePrefix("# ").trim()
                    val lines = wrapParagraph(cleanText, largeHeaderPaint, maxTextWidth)
                    lines.forEach { parsedLines.add(PdfLine(it, LineType.LARGE_HEADER)) }
                    parsedLines.add(PdfLine("", LineType.EMPTY))
                }
                trimmed.startsWith("## ") -> {
                    val cleanText = trimmed.removePrefix("## ").trim()
                    val lines = wrapParagraph(cleanText, largeHeaderPaint, maxTextWidth)
                    lines.forEach { parsedLines.add(PdfLine(it, LineType.LARGE_HEADER)) }
                    parsedLines.add(PdfLine("", LineType.EMPTY))
                }
                trimmed.startsWith("### ") -> {
                    val cleanText = trimmed.removePrefix("### ").trim()
                    val lines = wrapParagraph(cleanText, subHeaderPaint, maxTextWidth)
                    lines.forEach { parsedLines.add(PdfLine(it, LineType.SUB_HEADER)) }
                    parsedLines.add(PdfLine("", LineType.EMPTY))
                }
                trimmed.startsWith("**") && trimmed.endsWith("**") -> {
                    val cleanText = trimmed.removeSurrounding("**").trim()
                    val lines = wrapParagraph(cleanText, subHeaderPaint, maxTextWidth)
                    lines.forEach { parsedLines.add(PdfLine(it, LineType.SUB_HEADER)) }
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val cleanText = trimmed.substring(2).trim()
                    // Indented somewhat for bullets
                    val lines = wrapParagraph(cleanText, bodyPaint, maxTextWidth - 15f)
                    if (lines.isNotEmpty()) {
                        parsedLines.add(PdfLine(lines[0], LineType.BULLET))
                        for (i in 1 until lines.size) {
                            parsedLines.add(PdfLine(lines[i], LineType.BODY)) // Drawn with indentation
                        }
                    }
                }
                else -> {
                    val lines = wrapParagraph(trimmed, bodyPaint, maxTextWidth)
                    lines.forEach { parsedLines.add(PdfLine(it, LineType.BODY)) }
                }
            }
        }

        // Setup Page 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // Header visual accent
        canvas.drawText("CREATORPRO | MINIMALIST EXPORT", marginX, 40f, headerPaint)
        canvas.drawLine(marginX, 48f, pageWidth - marginX, 48f, lineDividerPaint)

        // Main Document Title
        canvas.drawText(documentTitle.uppercase(Locale.US), marginX, currentY, mainTitlePaint)
        currentY += 24f

        // Metadata Subheader
        val dateStr = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Category: Document Format PDF  •  Generated: $dateStr", marginX, currentY, headerPaint)
        currentY += 16f
        canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, lineDividerPaint)
        currentY += 30f

        // Draw parsed document lines
        for (pdfLine in parsedLines) {
            // Check page boundaries
            if (currentY > maxY) {
                document.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                // Draw header on subsequent pages
                canvas.drawText("CREATORPRO | PAGE $pageNum", marginX, 40f, headerPaint)
                canvas.drawLine(marginX, 48f, pageWidth - marginX, 48f, lineDividerPaint)
                currentY = startY
            }

            when (pdfLine.type) {
                LineType.EMPTY -> {
                    currentY += 10f
                }
                LineType.LARGE_HEADER -> {
                    canvas.drawText(pdfLine.text, marginX, currentY, largeHeaderPaint)
                    currentY += 20f
                }
                LineType.SUB_HEADER -> {
                    canvas.drawText(pdfLine.text, marginX, currentY, subHeaderPaint)
                    currentY += 18f
                }
                LineType.BULLET -> {
                    // Draw a subtle primary color bullet point
                    val bulletPaint = Paint().apply {
                        color = Color.parseColor("#4355B9")
                        isAntiAlias = true
                    }
                    canvas.drawCircle(marginX + 5f, currentY - 4f, 2.5f, bulletPaint)
                    canvas.drawText(pdfLine.text, marginX + 15f, currentY, bodyPaint)
                    currentY += 18f
                }
                LineType.BODY -> {
                    // Checks if simple indenting is needed because it was a wrapped bullet line
                    val parsedIndex = parsedLines.indexOf(pdfLine)
                    val isIndent = parsedIndex > 0 && parsedLines[parsedIndex - 1].type == LineType.BULLET
                    val currentX = if (isIndent) marginX + 15f else marginX
                    canvas.drawText(pdfLine.text, currentX, currentY, bodyPaint)
                    currentY += 18f
                }
            }
        }

        // Finish the final page
        document.finishPage(page)

        // Save file
        val cleanTitle = documentTitle.replace("\\s+".toRegex(), "_")
            .replace("[^a-zA-Z0-9_]".toRegex(), "")
        val filename = "CreatorStudio_${cleanTitle}.pdf"

        savePdfToDownloads(context, document, filename)
    }

    private fun savePdfToDownloads(context: Context, document: PdfDocument, filename: String) {
        var outputStream: OutputStream? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = resolver.openOutputStream(uri)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = File(downloadsDir, filename)
                outputStream = FileOutputStream(file)
            }

            if (outputStream != null) {
                document.writeTo(outputStream)
                Toast.makeText(context, "Successfully downloaded PDF to Downloads!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Could not open download output stream.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to download PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
            document.close()
        }
    }
}
