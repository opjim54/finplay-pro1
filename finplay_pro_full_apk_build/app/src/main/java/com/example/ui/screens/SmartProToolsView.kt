package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.InvoiceLineItem
import com.example.domain.SmartToolsProEngine
import com.example.i18n.LocalStrings
import com.example.ui.components.LedgerCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartProToolView(
    tool: SmartToolId,
    onSaveToNote: (title: String, content: String) -> Unit,
    onNotify: (String) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun copyText(value: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(tool.title, value))
        onNotify(strings.copiedMsg)
    }

    fun saveNote(content: String) {
        if (content.isNotBlank()) {
            onSaveToNote(tool.title, content)
            onNotify(strings.savedToNoteMsg)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (tool) {
            SmartToolId.COMPOUND_INTEREST -> {
                var principal by remember { mutableStateOf("100000") }
                var rate by remember { mutableStateOf("10") }
                var years by remember { mutableStateOf("5") }
                var monthlyAdd by remember { mutableStateOf("2000") }
                var compounds by remember { mutableIntStateOf(12) }

                OutlinedTextField(
                    value = principal,
                    onValueChange = { principal = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Initial Principal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Annual Rate (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = years,
                        onValueChange = { years = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Years") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = monthlyAdd,
                    onValueChange = { monthlyAdd = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Monthly Contribution") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(12 to "Monthly", 4 to "Quarterly", 1 to "Yearly").forEach { (freq, lbl) ->
                        FilterChip(selected = compounds == freq, onClick = { compounds = freq }, label = { Text(lbl) })
                    }
                }
                val res = remember(principal, rate, years, monthlyAdd, compounds) {
                    SmartToolsProEngine.calculateCompoundInterest(
                        principal = principal.toDoubleOrNull() ?: 0.0,
                        annualRatePercent = rate.toDoubleOrNull() ?: 0.0,
                        years = years.toDoubleOrNull() ?: 0.0,
                        compoundsPerYear = compounds,
                        monthlyContribution = monthlyAdd.toDoubleOrNull() ?: 0.0
                    )
                }
                val summary = String.format(
                    Locale.US,
                    "Future Value: %.2f\nTotal Invested: %.2f\nInterest Earned: %.2f",
                    res.finalBalance, res.totalContributed, res.totalInterestEarned
                )
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        String.format(Locale.US, "Final Value: %.2f", res.finalBalance),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(String.format(Locale.US, "Total Contributed: %.2f", res.totalContributed))
                    Text(String.format(Locale.US, "Compound Interest Earned: %.2f", res.totalInterestEarned))
                    Spacer(modifier = Modifier.height(8.dp))
                    ProActionButtons(summary, ::copyText, ::saveNote)
                }
            }

            SmartToolId.SPLIT_BILL -> {
                var bill by remember { mutableStateOf("2400") }
                var tip by remember { mutableStateOf("10") }
                var tax by remember { mutableStateOf("5") }
                var people by remember { mutableStateOf("4") }

                OutlinedTextField(
                    value = bill,
                    onValueChange = { bill = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Total Bill Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tip,
                        onValueChange = { tip = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Tip (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tax,
                        onValueChange = { tax = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("VAT/Tax (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = people,
                        onValueChange = { people = it.filter { c -> c.isDigit() } },
                        label = { Text("People") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                val res = remember(bill, tip, tax, people) {
                    SmartToolsProEngine.calculateSplitBill(
                        bill.toDoubleOrNull() ?: 0.0,
                        tip.toDoubleOrNull() ?: 0.0,
                        tax.toDoubleOrNull() ?: 0.0,
                        (people.toIntOrNull() ?: 1).coerceAtLeast(1)
                    )
                }
                val summary = String.format(
                    Locale.US,
                    "Per Person: %.2f\nGrand Total: %.2f (Tip: %.2f, Tax: %.2f)",
                    res.perPerson, res.grandTotal, res.tipAmount, res.taxAmount
                )
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        String.format(Locale.US, "Each Person Pays: %.2f", res.perPerson),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(String.format(Locale.US, "Grand Total: %.2f", res.grandTotal))
                    Text(String.format(Locale.US, "Tip: %.2f • Tax: %.2f", res.tipAmount, res.taxAmount))
                    Spacer(modifier = Modifier.height(8.dp))
                    ProActionButtons(summary, ::copyText, ::saveNote)
                }
            }

            SmartToolId.DISCOUNT_VAT -> {
                var price by remember { mutableStateOf("1500") }
                var disc by remember { mutableStateOf("20") }
                var coupon by remember { mutableStateOf("5") }
                var vat by remember { mutableStateOf("7.5") }

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Original Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = disc,
                        onValueChange = { disc = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Discount %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = coupon,
                        onValueChange = { coupon = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Extra %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = vat,
                        onValueChange = { vat = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("VAT %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                val res = remember(price, disc, coupon, vat) {
                    SmartToolsProEngine.calculateDiscountVat(
                        price.toDoubleOrNull() ?: 0.0,
                        disc.toDoubleOrNull() ?: 0.0,
                        coupon.toDoubleOrNull() ?: 0.0,
                        vat.toDoubleOrNull() ?: 0.0
                    )
                }
                val summary = String.format(
                    Locale.US,
                    "Final Payable: %.2f\nYou Saved: %.2f\nVAT Added: %.2f",
                    res.finalPayable, res.discountSaved, res.vatAmount
                )
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        String.format(Locale.US, "Final Payable: %.2f", res.finalPayable),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(String.format(Locale.US, "Discount Saved: %.2f", res.discountSaved))
                    Text(String.format(Locale.US, "VAT Amount: %.2f", res.vatAmount))
                    Spacer(modifier = Modifier.height(8.dp))
                    ProActionButtons(summary, ::copyText, ::saveNote)
                }
            }

            SmartToolId.PASSWORD_GEN -> {
                var length by remember { mutableFloatStateOf(16f) }
                var upper by remember { mutableStateOf(true) }
                var lower by remember { mutableStateOf(true) }
                var digits by remember { mutableStateOf(true) }
                var symbols by remember { mutableStateOf(true) }
                var generated by remember { mutableStateOf(SmartToolsProEngine.generateStrongPassword()) }

                Text("Length: ${length.roundToInt()} characters", fontWeight = FontWeight.Bold)
                Slider(value = length, onValueChange = { length = it }, valueRange = 6f..40f)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = upper, onClick = { upper = !upper }, label = { Text("A-Z") })
                    FilterChip(selected = lower, onClick = { lower = !lower }, label = { Text("a-z") })
                    FilterChip(selected = digits, onClick = { digits = !digits }, label = { Text("0-9") })
                    FilterChip(selected = symbols, onClick = { symbols = !symbols }, label = { Text("!@#") })
                }
                Button(
                    onClick = {
                        generated = SmartToolsProEngine.generateStrongPassword(
                            length.roundToInt(), upper, lower, digits, symbols
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Secure Password")
                }
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = generated.password,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Strength: ${generated.strengthLabel} (${generated.entropyBits} bits)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ProActionButtons(generated.password, ::copyText, ::saveNote)
                }
            }

            SmartToolId.UUID_HASH_GEN -> {
                var uuid by remember { mutableStateOf(SmartToolsProEngine.newUuid()) }
                var textToHash by remember { mutableStateOf("Smart Finance Life") }
                var selectedAlgo by remember { mutableStateOf("SHA-256") }

                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text("UUID v4", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(uuid, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { uuid = SmartToolsProEngine.newUuid() }, modifier = Modifier.weight(1f)) {
                            Text("New UUID")
                        }
                        Button(onClick = { copyText(uuid) }, modifier = Modifier.weight(1f)) {
                            Text("Copy UUID")
                        }
                    }
                }
                OutlinedTextField(
                    value = textToHash,
                    onValueChange = { textToHash = it },
                    label = { Text("Text to Hash") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SHA-256", "SHA-512", "SHA-1", "MD5").forEach { algo ->
                        FilterChip(selected = selectedAlgo == algo, onClick = { selectedAlgo = algo }, label = { Text(algo) })
                    }
                }
                val hash = remember(textToHash, selectedAlgo) {
                    SmartToolsProEngine.computeHash(textToHash, selectedAlgo)
                }
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text("$selectedAlgo Digest", fontWeight = FontWeight.Bold)
                    Text(hash, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    ProActionButtons(hash, ::copyText, ::saveNote)
                }
            }

            SmartToolId.URL_ENCODER -> {
                var input by remember { mutableStateOf("https://smartfinancelife.app/search?q=farm budget & notes") }
                var output by remember { mutableStateOf("") }
                OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Input URL / String") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { output = SmartToolsProEngine.urlEncode(input) }, modifier = Modifier.weight(1f)) {
                        Text("URL Encode")
                    }
                    OutlinedButton(onClick = {
                        output = SmartToolsProEngine.urlDecode(input).getOrElse { "Invalid URL encoding" }
                    }, modifier = Modifier.weight(1f)) {
                        Text("URL Decode")
                    }
                }
                OutlinedTextField(value = output, onValueChange = { output = it }, label = { Text("Result") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                ProActionButtons(output, ::copyText, ::saveNote)
            }

            SmartToolId.BASE64_REGEX -> {
                var mode by remember { mutableIntStateOf(0) } // 0=Base64, 1=Regex
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Text Base64") })
                    FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Regex Tester") })
                }
                if (mode == 0) {
                    var input by remember { mutableStateOf("Hello Smart Finance!") }
                    var output by remember { mutableStateOf("") }
                    OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Input Text / Base64") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { output = SmartToolsProEngine.encodeTextBase64(input) }, modifier = Modifier.weight(1f)) { Text("Encode") }
                        OutlinedButton(onClick = {
                            output = SmartToolsProEngine.decodeTextBase64(input).getOrElse { "Invalid Base64" }
                        }, modifier = Modifier.weight(1f)) { Text("Decode") }
                    }
                    OutlinedTextField(value = output, onValueChange = { output = it }, label = { Text("Output") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                    ProActionButtons(output, ::copyText, ::saveNote)
                } else {
                    var pattern by remember { mutableStateOf("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b") }
                    var sample by remember { mutableStateOf("Contact team@smartfinance.app or admin@farm.org for ledger sync.") }
                    OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Regex Pattern") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sample, onValueChange = { sample = it }, label = { Text("Test Text") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                    val matchesRes = remember(pattern, sample) { SmartToolsProEngine.testRegexMatches(pattern, sample) }
                    LedgerCard(modifier = Modifier.fillMaxWidth()) {
                        matchesRes.onSuccess { hits ->
                            Text("Matches Found: ${hits.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            hits.forEach { h -> Text("• $h", fontFamily = FontFamily.Monospace) }
                            if (hits.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                ProActionButtons(hits.joinToString("\n"), ::copyText, ::saveNote)
                            }
                        }.onFailure { err ->
                            Text("Invalid Regex: ${err.localizedMessage}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            SmartToolId.MARKDOWN_PREVIEWER -> {
                var md by remember {
                    mutableStateOf("# Q3 Budget Plan\n\n## Highlights\n- Reduce dining spend by **15%**\n- Increase emergency reserve\n- > Consistency compounds wealth")
                }
                OutlinedTextField(
                    value = md,
                    onValueChange = { md = it },
                    label = { Text("Markdown Source") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Live Preview", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    MarkdownLinesRenderer(md)
                }
                ProActionButtons(md, ::copyText, ::saveNote)
            }

            SmartToolId.INVOICE_MAKER -> {
                var invoiceTitle by remember { mutableStateOf("INVOICE #1042") }
                var fromParty by remember { mutableStateOf("Green Valley Business") }
                var billTo by remember { mutableStateOf("Rahim Enterprise") }
                var taxPct by remember { mutableStateOf("5") }
                var itemName by remember { mutableStateOf("") }
                var itemQty by remember { mutableStateOf("1") }
                var itemPrice by remember { mutableStateOf("") }
                val itemsList = remember {
                    mutableStateListOf(
                        InvoiceLineItem("Monthly Retainer Service", 1.0, 5000.0),
                        InvoiceLineItem("Setup & Consultation", 2.0, 750.0)
                    )
                }

                val saveInvoicePdf = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/pdf")
                ) { uri ->
                    if (uri != null) {
                        scope.launch {
                            val res = withContext(Dispatchers.IO) {
                                context.contentResolver.openOutputStream(uri)?.use { out ->
                                    SmartToolsProEngine.writeInvoicePdf(
                                        invoiceTitle, fromParty, billTo, itemsList.toList(),
                                        taxPct.toDoubleOrNull() ?: 0.0, "", out
                                    )
                                } ?: Result.failure(IllegalStateException("Stream error"))
                            }
                            res.onSuccess { onNotify(strings.exportSuccess) }
                                .onFailure { onNotify(it.localizedMessage ?: "PDF Error") }
                        }
                    }
                }

                OutlinedTextField(value = invoiceTitle, onValueChange = { invoiceTitle = it }, label = { Text("Invoice Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fromParty, onValueChange = { fromParty = it }, label = { Text("From") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = billTo, onValueChange = { billTo = it }, label = { Text("Bill To") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = taxPct,
                    onValueChange = { taxPct = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Tax / VAT (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Item") }, singleLine = true, modifier = Modifier.weight(1.6f))
                    OutlinedTextField(value = itemQty, onValueChange = { itemQty = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Qty") }, singleLine = true, modifier = Modifier.weight(0.7f))
                    OutlinedTextField(value = itemPrice, onValueChange = { itemPrice = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Price") }, singleLine = true, modifier = Modifier.weight(1f))
                    Button(onClick = {
                        val q = itemQty.toDoubleOrNull() ?: 1.0
                        val p = itemPrice.toDoubleOrNull() ?: 0.0
                        if (itemName.isNotBlank() && p > 0) {
                            itemsList.add(InvoiceLineItem(itemName.trim(), q, p))
                            itemName = ""; itemQty = "1"; itemPrice = ""
                        }
                    }) { Icon(Icons.Default.Add, contentDescription = "Add item") }
                }

                var subtotal = 0.0
                itemsList.forEachIndexed { idx, line ->
                    val lt = line.qty * line.unitPrice
                    subtotal += lt
                    LedgerCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(line.description, fontWeight = FontWeight.SemiBold)
                                Text("${line.qty} × ${line.unitPrice} = $lt", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { itemsList.removeAt(idx) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                val taxAmt = subtotal * ((taxPct.toDoubleOrNull() ?: 0.0) / 100.0)
                val grandTotal = subtotal + taxAmt
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(String.format(Locale.US, "Subtotal: %.2f | Tax: %.2f", subtotal, taxAmt))
                    Text(
                        String.format(Locale.US, "Total Payable: %.2f", grandTotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { saveInvoicePdf.launch("invoice_${System.currentTimeMillis()}.pdf") },
                        enabled = itemsList.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export PDF")
                    }
                    OutlinedButton(
                        onClick = {
                            val txt = "$invoiceTitle\nFrom: $fromParty\nTo: $billTo\nTotal: ${String.format(Locale.US, "%.2f", grandTotal)}"
                            saveNote(txt)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.saveToNote)
                    }
                }
            }

            SmartToolId.POMODORO_TIMER -> {
                var focusMins by remember { mutableIntStateOf(25) }
                var breakMins by remember { mutableIntStateOf(5) }
                var isFocusPhase by remember { mutableStateOf(true) }
                var running by remember { mutableStateOf(false) }
                var remainingMs by remember { mutableLongStateOf(25 * 60_000L) }
                var completedSessions by remember { mutableIntStateOf(0) }

                LaunchedEffect(running) {
                    var last = System.currentTimeMillis()
                    while (running) {
                        delay(250)
                        val now = System.currentTimeMillis()
                        val next = (remainingMs - (now - last)).coerceAtLeast(0L)
                        last = now
                        remainingMs = next
                        if (next == 0L) {
                            if (isFocusPhase) {
                                completedSessions++
                                isFocusPhase = false
                                remainingMs = breakMins * 60_000L
                            } else {
                                isFocusPhase = true
                                remainingMs = focusMins * 60_000L
                            }
                            running = false
                        }
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(25 to 5, 50 to 10, 15 to 3).forEach { (f, b) ->
                        FilterChip(
                            selected = focusMins == f,
                            onClick = {
                                if (!running) {
                                    focusMins = f; breakMins = b
                                    isFocusPhase = true
                                    remainingMs = f * 60_000L
                                }
                            },
                            label = { Text("${f}m / ${b}m") }
                        )
                    }
                }
                val secLeft = (remainingMs + 999) / 1000
                val m = secLeft / 60
                val s = secLeft % 60
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (isFocusPhase) "FOCUS SESSION" else "BREAK TIME",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        String.format(Locale.US, "%02d:%02d", m, s),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("Completed Sessions Today: $completedSessions", style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { running = !running }, modifier = Modifier.weight(1f)) {
                        Icon(if (running) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (running) "Pause" else "Start")
                    }
                    OutlinedButton(onClick = {
                        running = false
                        isFocusPhase = true
                        remainingMs = focusMins * 60_000L
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }
            }

            else -> Unit
        }
        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
fun MarkdownLinesRenderer(rawMarkdown: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rawMarkdown.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("# ") -> Text(
                    trimmed.removePrefix("# "),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                trimmed.startsWith("## ") -> Text(
                    trimmed.removePrefix("## "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                trimmed.startsWith("### ") -> Text(
                    trimmed.removePrefix("### "),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                trimmed.startsWith("> ") -> Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        trimmed.removePrefix("> "),
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> Text("• ${trimmed.drop(2)}")
                else -> Text(trimmed.replace("**", ""), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ProActionButtons(
    resultText: String,
    onCopy: (String) -> Unit,
    onSaveNote: (String) -> Unit
) {
    val strings = LocalStrings.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onCopy(resultText) },
            enabled = resultText.isNotBlank(),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.copyText)
        }
        OutlinedButton(
            onClick = { onSaveNote(resultText) },
            enabled = resultText.isNotBlank(),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.saveToNote)
        }
    }
}
