package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.set
import androidx.core.graphics.withTranslation
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.BreakIterator
import java.util.Calendar
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

enum class TextCaseMode {
    UPPER, LOWER, TITLE, SENTENCE, TOGGLE
}

data class TextStats(
    val words: Int,
    val charsWithSpaces: Int,
    val charsWithoutSpaces: Int,
    val lines: Int,
    val sentences: Int,
    val paragraphs: Int
)

data class AgeBreakdown(
    val years: Int,
    val months: Int,
    val days: Int,
    val totalDays: Long,
    val daysUntilNextBirthday: Int
)

data class BmiResult(
    val bmi: Double,
    val category: String
)

data class EmiCalculation(
    val monthlyEmi: Double,
    val totalPayable: Double,
    val totalInterest: Double
)

object SmartToolsEngine {
    private val prettyJson = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val compactJson = Json { prettyPrint = false; ignoreUnknownKeys = true }

    // --- 1. TEXT TOOLS ---
    fun analyzeText(text: String): TextStats {
        if (text.isEmpty()) {
            return TextStats(0, 0, 0, 0, 0, 0)
        }
        val words = countWords(text)
        val charsWithSpaces = text.length
        val charsWithoutSpaces = text.count { !it.isWhitespace() }
        val lines = text.split('\n').size
        val paragraphs = text.split(Regex("\\n\\s*\\n")).count { it.isNotBlank() }
            .let { if (it == 0 && text.isNotBlank()) 1 else it }
        val sentences = countSentences(text)
        return TextStats(words, charsWithSpaces, charsWithoutSpaces, lines, sentences, paragraphs)
    }

    fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    }

    private fun countSentences(text: String): Int {
        if (text.isBlank()) return 0
        val iterator = BreakIterator.getSentenceInstance(Locale.getDefault())
        iterator.setText(text)
        var count = 0
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            if (text.substring(start, end).any { it.isLetterOrDigit() }) {
                count++
            }
            start = end
            end = iterator.next()
        }
        return count
    }

    fun convertCase(text: String, mode: TextCaseMode): String {
        val locale = Locale.getDefault()
        return when (mode) {
            TextCaseMode.UPPER -> text.uppercase(locale)
            TextCaseMode.LOWER -> text.lowercase(locale)
            TextCaseMode.TITLE -> text.lowercase(locale).split(Regex("(?<=\\s)|(?=\\s)")).joinToString("") { token ->
                token.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            }
            TextCaseMode.SENTENCE -> {
                val sb = StringBuilder()
                var capitalizeNext = true
                for (ch in text.lowercase(locale)) {
                    if (capitalizeNext && ch.isLetter()) {
                        sb.append(ch.titlecaseChar())
                        capitalizeNext = false
                    } else {
                        sb.append(ch)
                        if (ch == '.' || ch == '!' || ch == '?' || ch == '।' || ch == '\n') {
                            capitalizeNext = true
                        }
                    }
                }
                sb.toString()
            }
            TextCaseMode.TOGGLE -> text.map { ch ->
                when {
                    ch.isUpperCase() -> ch.lowercaseChar()
                    ch.isLowerCase() -> ch.uppercaseChar()
                    else -> ch
                }
            }.joinToString("")
        }
    }

    fun removeExtraSpaces(text: String): String {
        return text
            .lines()
            .joinToString("\n") { line -> line.trim().replace(Regex("[ \\t\\u00A0]+"), " ") }
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    fun reverseText(text: String, byWordsOnly: Boolean = false): String {
        return if (byWordsOnly) {
            text.trim().split(Regex("\\s+")).reversed().joinToString(" ")
        } else {
            text.reversed()
        }
    }

    fun generateLoremIpsum(paragraphs: Int): String {
        val seeds = listOf(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
            "Curabitur pretium tincidunt lacus. Nulla gravida orci a odio. Nullam varius, turpis et commodo pharetra, est eros bibendum elit, nec luctus magna felis sollicitudin mauris.",
            "Integer in mauris eu nibh euismod gravida. Duis ac tellus et risus vulputate vehicula. Donec lobortis risus a elit. Etiam tempor. Ut ullamcorper, ligula eu tempor congue, eros est euismod turpis.",
            "Suspendisse potenti. Sed egestas, ante et vulputate volutpat, eros pede semper est, vitae luctus metus libero eu augue. Morbi purus libero, faucibus adipiscing, commodo quis, gravida id, est."
        )
        val count = paragraphs.coerceIn(1, 20)
        return (0 until count).joinToString("\n\n") { idx -> seeds[idx % seeds.size] }
    }

    // --- 2. FILE CONVERTER TOOLS ---
    fun cleanWordPasteToTxt(raw: String): String {
        val cleaned = raw
            .replace("\u2018", "'")
            .replace("\u2019", "'")
            .replace("\u201C", "\"")
            .replace("\u201D", "\"")
            .replace("\u2013", "-")
            .replace("\u2014", "--")
            .replace("\u00A0", " ")
            .replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
        return removeExtraSpaces(cleaned)
    }

    fun csvToJson(csvText: String): Result<String> = runCatching {
        val rows = parseCsvRows(csvText.trim())
        require(rows.isNotEmpty()) { "CSV input is empty." }
        val headers = rows.first().mapIndexed { i, h -> h.trim().ifEmpty { "column_${i + 1}" } }
        val objects = rows.drop(1).filter { row -> row.any { it.isNotBlank() } }.map { row ->
            val map = LinkedHashMap<String, JsonPrimitive>()
            headers.forEachIndexed { colIdx, key ->
                val rawVal = row.getOrElse(colIdx) { "" }.trim()
                val prim = when {
                    rawVal.equals("true", true) -> JsonPrimitive(true)
                    rawVal.equals("false", true) -> JsonPrimitive(false)
                    rawVal.toLongOrNull() != null -> JsonPrimitive(rawVal.toLong())
                    rawVal.toDoubleOrNull() != null -> JsonPrimitive(rawVal.toDouble())
                    else -> JsonPrimitive(rawVal)
                }
                map[key] = prim
            }
            JsonObject(map)
        }
        prettyJson.encodeToString(JsonArray.serializer(), JsonArray(objects))
    }

    private fun parseCsvRows(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < csv.length) {
            val ch = csv[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < csv.length && csv[i + 1] == '"') {
                        cell.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    currentRow.add(cell.toString())
                    cell.clear()
                }
                (ch == '\n' || ch == '\r') && !inQuotes -> {
                    if (ch == '\r' && i + 1 < csv.length && csv[i + 1] == '\n') i++
                    currentRow.add(cell.toString())
                    rows.add(currentRow.toList())
                    currentRow.clear()
                    cell.clear()
                }
                else -> cell.append(ch)
            }
            i++
        }
        if (cell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(cell.toString())
            rows.add(currentRow.toList())
        }
        return rows
    }

    fun formatJson(raw: String, minify: Boolean = false): Result<String> = runCatching {
        val elem = prettyJson.parseToJsonElement(raw.trim())
        if (minify) {
            compactJson.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), elem)
        } else {
            prettyJson.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), elem)
        }
    }

    fun writeTextToPdf(text: String, out: OutputStream): Result<Unit> = runCatching {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 42f
        val contentWidth = (pageWidth - 2 * margin).toInt()
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 12f
        }
        val safeText = text.ifBlank { " " }
        val layout = StaticLayout.Builder
            .obtain(safeText, 0, safeText.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f, 1.15f)
            .build()

        val usableHeight = (pageHeight - 2 * margin).toInt()
        var currentLine = 0
        var pageNum = 1
        while (currentLine < layout.lineCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum++).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            val startTop = layout.getLineTop(currentLine)
            var lastLine = currentLine
            while (lastLine < layout.lineCount && (layout.getLineBottom(lastLine) - startTop) <= usableHeight) {
                lastLine++
            }
            if (lastLine == currentLine) lastLine++

            canvas.withTranslation(margin, margin - startTop) {
                clipRect(0, startTop, contentWidth, layout.getLineBottom(lastLine - 1))
                layout.draw(this)
            }

            doc.finishPage(page)
            currentLine = lastLine
        }
        doc.writeTo(out)
        doc.close()
    }

    fun writeImagesToPdf(context: Context, uris: List<Uri>, out: OutputStream): Result<Unit> = runCatching {
        require(uris.isNotEmpty()) { "Select at least one image." }
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 24f
        uris.forEachIndexed { idx, uri ->
            val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: throw IllegalStateException("Unable to read image ${idx + 1}")
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, idx + 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            val availW = pageWidth - 2 * margin
            val availH = pageHeight - 2 * margin
            val scale = minOf(availW / bmp.width, availH / bmp.height)
            val drawW = bmp.width * scale
            val drawH = bmp.height * scale
            val left = (pageWidth - drawW) / 2f
            val top = (pageHeight - drawH) / 2f
            val scaled = bmp.scale(drawW.toInt().coerceAtLeast(1), drawH.toInt().coerceAtLeast(1))
            canvas.drawBitmap(scaled, left, top, null)
            doc.finishPage(page)
        }
        doc.writeTo(out)
        doc.close()
    }

    fun extractTextFromPdf(context: Context, uri: Uri): Result<String> = runCatching {
        PDFBoxResourceLoader.init(context.applicationContext)
        context.contentResolver.openInputStream(uri)?.use { stream ->
            PDDocument.load(stream).use { document ->
                val stripper = PDFTextStripper()
                stripper.getText(document).trim()
            }
        } ?: throw IllegalStateException("Cannot open PDF stream.")
    }

    // --- 3. CALCULATOR TOOLS ---
    fun calculateAge(birthMillis: Long, referenceMillis: Long = System.currentTimeMillis()): AgeBreakdown {
        val birth = Calendar.getInstance().apply {
            timeInMillis = birthMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val ref = Calendar.getInstance().apply {
            timeInMillis = maxOf(referenceMillis, birthMillis)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        var years = ref.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        var months = ref.get(Calendar.MONTH) - birth.get(Calendar.MONTH)
        var days = ref.get(Calendar.DAY_OF_MONTH) - birth.get(Calendar.DAY_OF_MONTH)

        if (days < 0) {
            months -= 1
            val prevMonth = ref.clone() as Calendar
            prevMonth.add(Calendar.MONTH, -1)
            days += prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
        if (months < 0) {
            years -= 1
            months += 12
        }
        val totalDays = ((ref.timeInMillis - birth.timeInMillis) / 86_400_000L).coerceAtLeast(0L)

        val nextBday = Calendar.getInstance().apply {
            timeInMillis = ref.timeInMillis
            set(Calendar.MONTH, birth.get(Calendar.MONTH))
            set(
                Calendar.DAY_OF_MONTH,
                birth.get(Calendar.DAY_OF_MONTH).coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH))
            )
            if (timeInMillis < ref.timeInMillis) {
                add(Calendar.YEAR, 1)
            }
        }
        val daysUntil = ((nextBday.timeInMillis - ref.timeInMillis) / 86_400_000L).toInt().coerceAtLeast(0)
        return AgeBreakdown(years.coerceAtLeast(0), months.coerceAtLeast(0), days.coerceAtLeast(0), totalDays, daysUntil)
    }

    fun calculateBmi(weightKg: Double, heightCm: Double): BmiResult {
        val heightM = (heightCm / 100.0).coerceAtLeast(0.3)
        val bmi = weightKg / (heightM * heightM)
        val category = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal Weight"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }
        return BmiResult((bmi * 10.0).roundToInt() / 10.0, category)
    }

    fun calculateEmi(principal: Double, annualRatePercent: Double, tenureMonths: Int): EmiCalculation {
        val n = tenureMonths.coerceAtLeast(1)
        val p = principal.coerceAtLeast(0.0)
        if (annualRatePercent <= 0.0) {
            return EmiCalculation(p / n, p, 0.0)
        }
        val r = annualRatePercent / 12.0 / 100.0
        val factor = (1.0 + r).pow(n.toDouble())
        val emi = (p * r * factor) / (factor - 1.0)
        val total = emi * n
        return EmiCalculation(emi, total, (total - p).coerceAtLeast(0.0))
    }

    fun convertUnit(value: Double, conversionKey: String): Double {
        return when (conversionKey) {
            "KG_TO_LB" -> value * 2.20462262
            "LB_TO_KG" -> value / 2.20462262
            "FT_TO_M" -> value * 0.3048
            "M_TO_FT" -> value / 0.3048
            "CM_TO_IN" -> value / 2.54
            "IN_TO_CM" -> value * 2.54
            "KM_TO_MI" -> value * 0.621371
            "MI_TO_KM" -> value / 0.621371
            "C_TO_F" -> (value * 9.0 / 5.0) + 32.0
            "F_TO_C" -> (value - 32.0) * 5.0 / 9.0
            "ACRE_TO_DECIMAL" -> value * 100.0
            "DECIMAL_TO_ACRE" -> value / 100.0
            else -> value
        }
    }

    // --- 4. IMAGE TOOLS ---
    fun compressBitmap(bitmap: Bitmap, quality: Int, format: Bitmap.CompressFormat): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(format, quality.coerceIn(5, 100), out)
        return out.toByteArray()
    }

    fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return bitmap.scale(width.coerceIn(1, 4096), height.coerceIn(1, 4096))
    }

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String {
        val bytes = compressBitmap(bitmap, quality, Bitmap.CompressFormat.JPEG)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun generateCodeBitmap(
        content: String,
        format: BarcodeFormat,
        width: Int = 640,
        height: Int = if (format == BarcodeFormat.QR_CODE) 640 else 240
    ): Result<Bitmap> = runCatching {
        require(content.isNotBlank()) { "Content cannot be empty." }
        val matrix = MultiFormatWriter().encode(content.trim(), format, width, height)
        val bmp = createBitmap(width, height)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp[x, y] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        bmp
    }
}
