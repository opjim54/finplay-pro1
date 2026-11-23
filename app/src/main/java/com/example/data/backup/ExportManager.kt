package com.example.data.backup

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.CurrencyUtil
import com.example.domain.DateTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream

object ExportManager {
    val jsonCodec = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun snapshotToJson(snapshot: FullBackupSnapshot): String =
        jsonCodec.encodeToString(FullBackupSnapshot.serializer(), snapshot)

    fun jsonToSnapshot(raw: String): Result<FullBackupSnapshot> = runCatching {
        jsonCodec.decodeFromString(FullBackupSnapshot.serializer(), raw)
    }

    fun generateCsv(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>
    ): String {
        val catMap = categories.associateBy { it.id }
        val accMap = accounts.associateBy { it.id }
        val sb = StringBuilder()
        // UTF-8 BOM for Excel Bangla support
        sb.append('\uFEFF')
        sb.appendLine("ID,Date,Time,Type,Category,Account,TargetAccount,Amount,PaymentMethod,QRRef,Note")
        transactions.filter { !it.isDeleted }.forEach { tx ->
            val date = DateTimeUtil.formatDate(tx.timestamp)
            val time = DateTimeUtil.formatTime(tx.timestamp)
            val cat = csvEscape(catMap[tx.categoryId]?.name ?: "Others")
            val acc = csvEscape(accMap[tx.accountId]?.name ?: "Account")
            val target = csvEscape(tx.targetAccountId?.let { accMap[it]?.name } ?: "")
            val method = csvEscape(tx.paymentMethod)
            val qr = csvEscape(tx.qrReference.orEmpty())
            val note = csvEscape(tx.note)
            sb.appendLine("${tx.id},\"$date\",\"$time\",${tx.type.name},$cat,$acc,$target,${tx.amount},$method,$qr,$note")
        }
        return sb.toString()
    }

    fun generateExcelXml(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>,
        currencyCode: String
    ): String {
        val catMap = categories.associateBy { it.id }
        val accMap = accounts.associateBy { it.id }
        val rows = StringBuilder()
        rows.append(
            """
            <Row>
              <Cell ss:StyleID="Header"><Data ss:Type="String">Date</Data></Cell>
              <Cell ss:StyleID="Header"><Data ss:Type="String">Type</Data></Cell>
              <Cell ss:StyleID="Header"><Data ss:Type="String">Category</Data></Cell>
              <Cell ss:StyleID="Header"><Data ss:Type="String">Account</Data></Cell>
              <Cell ss:StyleID="Header"><Data ss:Type="String">Amount ($currencyCode)</Data></Cell>
              <Cell ss:StyleID="Header"><Data ss:Type="String">Payment Method</Data></Cell>
              <Cell ss:StyleID="Header"><Data ss:Type="String">Note</Data></Cell>
            </Row>
            """.trimIndent()
        )
        transactions.filter { !it.isDeleted }.forEach { tx ->
            val date = xmlEscape(DateTimeUtil.formatDateTime(tx.timestamp))
            val cat = xmlEscape(catMap[tx.categoryId]?.name ?: "Others")
            val acc = xmlEscape(accMap[tx.accountId]?.name ?: "Account")
            val method = xmlEscape(tx.paymentMethod)
            val note = xmlEscape(tx.note)
            rows.append(
                """
                <Row>
                  <Cell><Data ss:Type="String">$date</Data></Cell>
                  <Cell><Data ss:Type="String">${tx.type.name}</Data></Cell>
                  <Cell><Data ss:Type="String">$cat</Data></Cell>
                  <Cell><Data ss:Type="String">$acc</Data></Cell>
                  <Cell><Data ss:Type="Number">${tx.amount}</Data></Cell>
                  <Cell><Data ss:Type="String">$method</Data></Cell>
                  <Cell><Data ss:Type="String">$note</Data></Cell>
                </Row>
                """.trimIndent()
            )
        }
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <?mso-application progid="Excel.Sheet"?>
            <Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
             xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
             <Styles>
              <Style ss:ID="Header">
               <Font ss:Bold="1" ss:Color="#FFFFFF"/>
               <Interior ss:Color="#047857" ss:Pattern="Solid"/>
              </Style>
             </Styles>
             <Worksheet ss:Name="FinanceLedger">
              <Table>
               $rows
              </Table>
             </Worksheet>
            </Workbook>
        """.trimIndent()
    }

    fun writePdfReport(
        outputStream: OutputStream,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>,
        currencyCode: String
    ) {
        val catMap = categories.associateBy { it.id }
        val accMap = accounts.associateBy { it.id }
        val active = transactions.filter { !it.isDeleted }
        val totalIncome = active.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = active.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalBalance = accounts.sumOf { it.balance }

        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas

        val headerPaint = Paint().apply {
            color = Color.rgb(4, 120, 87)
            style = Paint.Style.FILL
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(209, 250, 229)
            textSize = 11f
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
        }
        val boldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            isFakeBoldText = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
        }

        // Header Banner
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 82f, headerPaint)
        canvas.drawText("Smart Finance Life — Financial Statement", 28f, 36f, titlePaint)
        canvas.drawText(
            "Generated: ${DateTimeUtil.formatDateTime(System.currentTimeMillis())} | Balance: ${
                CurrencyUtil.format(totalBalance, currencyCode)
            }",
            28f,
            58f,
            subtitlePaint
        )

        var y = 108f
        canvas.drawText(
            "Income: ${CurrencyUtil.format(totalIncome, currencyCode)}   |   Expense: ${
                CurrencyUtil.format(totalExpense, currencyCode)
            }   |   Records: ${active.size}",
            28f,
            y,
            boldPaint
        )
        y += 18f
        canvas.drawLine(28f, y, pageWidth - 28f, y, linePaint)
        y += 20f

        canvas.drawText("Date", 28f, y, boldPaint)
        canvas.drawText("Type", 125f, y, boldPaint)
        canvas.drawText("Category", 185f, y, boldPaint)
        canvas.drawText("Account", 295f, y, boldPaint)
        canvas.drawText("Amount", 390f, y, boldPaint)
        canvas.drawText("Note", 470f, y, boldPaint)
        y += 10f
        canvas.drawLine(28f, y, pageWidth - 28f, y, linePaint)
        y += 16f

        active.forEach { tx ->
            if (y > pageHeight - 48) {
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = 44f
            }
            val date = DateTimeUtil.formatDate(tx.timestamp)
            val cat = (catMap[tx.categoryId]?.name ?: "Others").take(16)
            val acc = (accMap[tx.accountId]?.name ?: "Acct").take(14)
            val amt = CurrencyUtil.format(tx.amount, currencyCode)
            val note = tx.note.take(18)

            canvas.drawText(date, 28f, y, bodyPaint)
            canvas.drawText(tx.type.name, 125f, y, bodyPaint)
            canvas.drawText(cat, 185f, y, bodyPaint)
            canvas.drawText(acc, 295f, y, bodyPaint)
            canvas.drawText(amt, 390f, y, bodyPaint)
            canvas.drawText(note, 470f, y, bodyPaint)
            y += 18f
        }

        pdf.finishPage(page)
        pdf.writeTo(outputStream)
        pdf.close()
    }

    suspend fun writeToUri(context: Context, uri: Uri, writer: (OutputStream) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    writer(out)
                } ?: throw IllegalStateException("Cannot open destination URI")
            }
        }

    suspend fun readFromUri(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                } ?: throw IllegalStateException("Cannot read selected file")
            }
        }

    fun buildShareIntent(context: Context, fileName: String, mimeType: String, writer: (OutputStream) -> Unit): Intent {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { writer(it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun csvEscape(value: String): String =
        "\"${value.replace("\"", "\"\"").replace("\n", " ")}\""

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
