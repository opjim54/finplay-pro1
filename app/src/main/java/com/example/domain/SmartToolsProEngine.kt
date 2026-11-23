package com.example.domain

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import androidx.core.graphics.withTranslation
import java.io.OutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import kotlin.math.pow

data class CompoundInterestResult(
    val finalBalance: Double,
    val totalContributed: Double,
    val totalInterestEarned: Double
)

data class SplitBillResult(
    val tipAmount: Double,
    val taxAmount: Double,
    val grandTotal: Double,
    val perPerson: Double
)

data class DiscountVatResult(
    val discountSaved: Double,
    val priceAfterDiscount: Double,
    val vatAmount: Double,
    val finalPayable: Double
)

data class PasswordGenerationResult(
    val password: String,
    val entropyBits: Int,
    val strengthLabel: String
)

data class InvoiceLineItem(
    val description: String,
    val qty: Double,
    val unitPrice: Double
)

object SmartToolsProEngine {
    private val secureRandom = SecureRandom()

    fun calculateCompoundInterest(
        principal: Double,
        annualRatePercent: Double,
        years: Double,
        compoundsPerYear: Int = 12,
        monthlyContribution: Double = 0.0
    ): CompoundInterestResult {
        val p = principal.coerceAtLeast(0.0)
        val n = compoundsPerYear.coerceAtLeast(1)
        val t = years.coerceAtLeast(0.0)
        val r = (annualRatePercent / 100.0).coerceAtLeast(0.0)
        val m = monthlyContribution.coerceAtLeast(0.0)

        val principalFuture = if (r == 0.0) p else p * (1.0 + r / n).pow(n * t)
        val totalMonths = (t * 12.0).toInt().coerceAtLeast(0)
        val monthlyRate = r / 12.0
        val contribFuture = if (m == 0.0 || totalMonths == 0) {
            0.0
        } else if (monthlyRate == 0.0) {
            m * totalMonths
        } else {
            m * (((1.0 + monthlyRate).pow(totalMonths) - 1.0) / monthlyRate)
        }
        val finalTotal = principalFuture + contribFuture
        val totalContributed = p + (m * totalMonths)
        return CompoundInterestResult(
            finalBalance = finalTotal,
            totalContributed = totalContributed,
            totalInterestEarned = (finalTotal - totalContributed).coerceAtLeast(0.0)
        )
    }

    fun calculateSplitBill(
        billAmount: Double,
        tipPercent: Double,
        taxPercent: Double,
        peopleCount: Int
    ): SplitBillResult {
        val cleanBill = billAmount.coerceAtLeast(0.0)
        val tip = cleanBill * (tipPercent.coerceAtLeast(0.0) / 100.0)
        val tax = cleanBill * (taxPercent.coerceAtLeast(0.0) / 100.0)
        val grand = cleanBill + tip + tax
        val people = peopleCount.coerceAtLeast(1)
        return SplitBillResult(
            tipAmount = tip,
            taxAmount = tax,
            grandTotal = grand,
            perPerson = grand / people
        )
    }

    fun calculateDiscountVat(
        originalPrice: Double,
        discountPercent: Double,
        extraCouponPercent: Double,
        vatPercent: Double
    ): DiscountVatResult {
        val base = originalPrice.coerceAtLeast(0.0)
        val d1 = base * (discountPercent.coerceIn(0.0, 100.0) / 100.0)
        val afterFirst = base - d1
        val d2 = afterFirst * (extraCouponPercent.coerceIn(0.0, 100.0) / 100.0)
        val discounted = (afterFirst - d2).coerceAtLeast(0.0)
        val vat = discounted * (vatPercent.coerceAtLeast(0.0) / 100.0)
        return DiscountVatResult(
            discountSaved = d1 + d2,
            priceAfterDiscount = discounted,
            vatAmount = vat,
            finalPayable = discounted + vat
        )
    }

    fun generateStrongPassword(
        length: Int = 16,
        includeUpper: Boolean = true,
        includeLower: Boolean = true,
        includeDigits: Boolean = true,
        includeSymbols: Boolean = true
    ): PasswordGenerationResult {
        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lower = "abcdefghijklmnopqrstuvwxyz"
        val digits = "0123456789"
        val symbols = "!@#\$%^&*()-_=+[]{}?"
        val pools = mutableListOf<String>()
        if (includeUpper) pools.add(upper)
        if (includeLower) pools.add(lower)
        if (includeDigits) pools.add(digits)
        if (includeSymbols) pools.add(symbols)
        if (pools.isEmpty()) pools.add(lower + digits)

        val safeLen = length.coerceIn(4, 64)
        val allChars = pools.joinToString("")
        val chars = mutableListOf<Char>()
        pools.forEach { pool ->
            if (chars.size < safeLen) {
                chars.add(pool[secureRandom.nextInt(pool.length)])
            }
        }
        while (chars.size < safeLen) {
            chars.add(allChars[secureRandom.nextInt(allChars.length)])
        }
        for (i in chars.indices.reversed()) {
            val swapIdx = secureRandom.nextInt(i + 1)
            val tmp = chars[i]
            chars[i] = chars[swapIdx]
            chars[swapIdx] = tmp
        }
        val entropy = (safeLen * kotlin.math.log2(allChars.length.toDouble())).toInt()
        val strength = when {
            entropy >= 90 -> "Fortress"
            entropy >= 65 -> "Strong"
            entropy >= 45 -> "Moderate"
            else -> "Weak"
        }
        return PasswordGenerationResult(chars.joinToString(""), entropy, strength)
    }

    fun newUuid(): String = UUID.randomUUID().toString()

    fun computeHash(input: String, algorithm: String): String = runCatching {
        val digest = MessageDigest.getInstance(algorithm)
        digest.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }.getOrDefault("Error computing $algorithm")

    fun urlEncode(text: String): String = URLEncoder.encode(text, "UTF-8")

    fun urlDecode(text: String): Result<String> = runCatching {
        URLDecoder.decode(text, "UTF-8")
    }

    fun encodeTextBase64(text: String): String =
        Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    fun decodeTextBase64(b64: String): Result<String> = runCatching {
        String(Base64.decode(b64.trim(), Base64.DEFAULT), Charsets.UTF_8)
    }

    fun testRegexMatches(pattern: String, targetText: String): Result<List<String>> = runCatching {
        val regex = Regex(pattern)
        regex.findAll(targetText).map { it.value }.toList()
    }

    fun writeInvoicePdf(
        invoiceTitle: String,
        fromParty: String,
        billToParty: String,
        items: List<InvoiceLineItem>,
        taxPercent: Double,
        currencySymbol: String,
        out: OutputStream
    ): Result<Unit> = runCatching {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        val headerBg = Paint().apply { color = Color.rgb(4, 108, 78) }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, headerBg)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            isFakeBoldText = true
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(212, 175, 55)
            textSize = 11f
        }
        canvas.drawText(invoiceTitle.ifBlank { "INVOICE" }, margin, 52f, titlePaint)
        canvas.drawText("Smart Finance Life • Generated ${DateTimeUtil.formatDate(System.currentTimeMillis())}", margin, 78f, subPaint)

        val bodyText = buildString {
            appendLine("FROM: ${fromParty.ifBlank { "Business Profile" }}")
            appendLine("BILL TO: ${billToParty.ifBlank { "Client" }}")
            appendLine("------------------------------------------------------------")
            appendLine(String.format(Locale.US, "%-28s %6s %10s %10s", "ITEM", "QTY", "PRICE", "TOTAL"))
            appendLine("------------------------------------------------------------")
            var subtotal = 0.0
            items.forEach { item ->
                val lineTotal = item.qty * item.unitPrice
                subtotal += lineTotal
                appendLine(
                    String.format(
                        Locale.US,
                        "%-28s %6.1f %10.2f %10.2f",
                        item.description.take(26),
                        item.qty,
                        item.unitPrice,
                        lineTotal
                    )
                )
            }
            val taxAmt = subtotal * (taxPercent.coerceAtLeast(0.0) / 100.0)
            val total = subtotal + taxAmt
            appendLine("------------------------------------------------------------")
            appendLine(String.format(Locale.US, "Subtotal: %s%.2f", currencySymbol, subtotal))
            appendLine(String.format(Locale.US, "Tax/VAT (%.1f%%): %s%.2f", taxPercent, currencySymbol, taxAmt))
            appendLine(String.format(Locale.US, "GRAND TOTAL: %s%.2f", currencySymbol, total))
        }

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        val layout = StaticLayout.Builder
            .obtain(bodyText, 0, bodyText.length, bodyPaint, (pageWidth - 2 * margin).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f, 1.1f)
            .build()
        canvas.withTranslation(margin, 132f) {
            layout.draw(this)
        }
        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }
}
