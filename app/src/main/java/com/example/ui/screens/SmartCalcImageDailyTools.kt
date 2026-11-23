package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.get
import com.example.data.local.QuickTodoEntity
import com.example.domain.DateTimeUtil
import com.example.domain.SmartToolsEngine
import com.example.i18n.LocalStrings
import com.example.ui.components.LedgerCard
import com.google.zxing.BarcodeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartCalcImageDailyToolView(
    tool: SmartToolId,
    userId: Long,
    quickTodos: List<QuickTodoEntity>,
    onSaveQuickTodo: (QuickTodoEntity) -> Unit,
    onDeleteQuickTodo: (Long) -> Unit,
    onSaveToNote: (title: String, content: String) -> Unit = { _, _ -> },
    onNotify: (String) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current

    fun copyText(value: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(tool.title, value))
        onNotify(strings.copiedMsg)
    }

    fun saveCalcToNote(summary: String) {
        onSaveToNote(tool.title, summary)
        onNotify(strings.savedToNoteMsg)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (tool) {
            // --- 3. CALCULATOR TOOLS (5) ---
            SmartToolId.AGE_CALC -> {
                var birthMillis by remember {
                    mutableLongStateOf(
                        Calendar.getInstance().apply { set(2000, Calendar.JANUARY, 1) }.timeInMillis
                    )
                }
                val age = remember(birthMillis) { SmartToolsEngine.calculateAge(birthMillis) }
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = birthMillis }
                        DatePickerDialog(context, { _, y, m, d ->
                            cal.set(y, m, d)
                            birthMillis = cal.timeInMillis
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Event, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Date of Birth: ${DateTimeUtil.formatDate(birthMillis)}")
                }
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${age.years} Years, ${age.months} Months, ${age.days} Days",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Total Days Lived: ${age.totalDays} days")
                    Text("Next Birthday In: ${age.daysUntilNextBirthday} days")
                    Spacer(modifier = Modifier.height(8.dp))
                    CalcActionRow(
                        resultSummary = "DOB: ${DateTimeUtil.formatDate(birthMillis)}\nAge: ${age.years}y ${age.months}m ${age.days}d (${age.totalDays} days)",
                        onCopy = ::copyText,
                        onSaveNote = ::saveCalcToNote
                    )
                }
            }

            SmartToolId.BMI_CALC -> {
                var weight by remember { mutableStateOf("65") }
                var height by remember { mutableStateOf("170") }
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Weight (KG)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val w = weight.toDoubleOrNull() ?: 0.0
                val h = height.toDoubleOrNull() ?: 0.0
                if (w > 0 && h > 0) {
                    val bmiRes = remember(w, h) { SmartToolsEngine.calculateBmi(w, h) }
                    LedgerCard(modifier = Modifier.fillMaxWidth()) {
                        Text("BMI: ${bmiRes.bmi}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Category: ${bmiRes.category}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        CalcActionRow(
                            resultSummary = "Weight: ${w}kg | Height: ${h}cm\nBMI: ${bmiRes.bmi} (${bmiRes.category})",
                            onCopy = ::copyText,
                            onSaveNote = ::saveCalcToNote
                        )
                    }
                }
            }

            SmartToolId.EMI_CALC -> {
                var principal by remember { mutableStateOf("500000") }
                var rate by remember { mutableStateOf("9.5") }
                var months by remember { mutableStateOf("24") }
                OutlinedTextField(
                    value = principal,
                    onValueChange = { principal = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(strings.principalAmount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Annual Interest Rate (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = months,
                    onValueChange = { months = it.filter { c -> c.isDigit() } },
                    label = { Text("Tenure (Months)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val p = principal.toDoubleOrNull() ?: 0.0
                val r = rate.toDoubleOrNull() ?: 0.0
                val n = months.toIntOrNull() ?: 1
                if (p > 0 && n > 0) {
                    val res = remember(p, r, n) { SmartToolsEngine.calculateEmi(p, r, n) }
                    val emiStr = String.format(Locale.US, "%.2f", res.monthlyEmi)
                    val intStr = String.format(Locale.US, "%.2f", res.totalInterest)
                    val totStr = String.format(Locale.US, "%.2f", res.totalPayable)
                    LedgerCard(modifier = Modifier.fillMaxWidth()) {
                        Text("${strings.monthlyEmi}: $emiStr", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Total Interest: $intStr")
                        Text("Total Payable: $totStr")
                        Spacer(modifier = Modifier.height(8.dp))
                        CalcActionRow(
                            resultSummary = "Loan: $p @ $r% for $n mo\nMonthly EMI: $emiStr\nTotal Interest: $intStr\nTotal Payable: $totStr",
                            onCopy = ::copyText,
                            onSaveNote = ::saveCalcToNote
                        )
                    }
                }
            }

            SmartToolId.PERCENT_CALC -> {
                var aStr by remember { mutableStateOf("15") }
                var bStr by remember { mutableStateOf("2000") }
                OutlinedTextField(
                    value = aStr,
                    onValueChange = { aStr = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                    label = { Text("Value A (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bStr,
                    onValueChange = { bStr = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                    label = { Text("Value B (Total)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val a = aStr.toDoubleOrNull() ?: 0.0
                val b = bStr.toDoubleOrNull() ?: 0.0
                val pLine1 = "$a% of $b = ${String.format(Locale.US, "%.2f", (a / 100.0) * b)}"
                val pLine2 = if (b != 0.0) "$a is ${String.format(Locale.US, "%.2f", (a / b) * 100.0)}% of $b" else ""
                val pLine3 = if (b != 0.0) "Change from $b to $a = ${String.format(Locale.US, "%.2f", ((a - b) / b) * 100.0)}%" else ""
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(pLine1, fontWeight = FontWeight.Bold)
                    if (pLine2.isNotEmpty()) Text(pLine2)
                    if (pLine3.isNotEmpty()) Text(pLine3)
                    Spacer(modifier = Modifier.height(8.dp))
                    CalcActionRow(
                        resultSummary = listOf(pLine1, pLine2, pLine3).filter { it.isNotEmpty() }.joinToString("\n"),
                        onCopy = ::copyText,
                        onSaveNote = ::saveCalcToNote
                    )
                }
            }

            SmartToolId.UNIT_CONVERTER -> {
                val options = listOf(
                    "KG_TO_LB" to "KG → Pound (lb)",
                    "LB_TO_KG" to "Pound (lb) → KG",
                    "FT_TO_M" to "Feet → Meter",
                    "M_TO_FT" to "Meter → Feet",
                    "CM_TO_IN" to "cm → Inch",
                    "IN_TO_CM" to "Inch → cm",
                    "KM_TO_MI" to "KM → Mile",
                    "C_TO_F" to "°C → °F",
                    "ACRE_TO_DECIMAL" to "Acre → Decimal (শতক)"
                )
                var selectedKey by remember { mutableStateOf("KG_TO_LB") }
                var valStr by remember { mutableStateOf("10") }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { (k, label) ->
                        FilterChip(selected = selectedKey == k, onClick = { selectedKey = k }, label = { Text(label) })
                    }
                }
                OutlinedTextField(
                    value = valStr,
                    onValueChange = { valStr = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                    label = { Text("Input Value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val num = valStr.toDoubleOrNull() ?: 0.0
                val result = SmartToolsEngine.convertUnit(num, selectedKey)
                val resFormatted = String.format(Locale.US, "%.4f", result)
                val optLabel = options.firstOrNull { it.first == selectedKey }?.second ?: selectedKey
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Result: $resFormatted",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CalcActionRow(
                        resultSummary = "$optLabel: $num → $resFormatted",
                        onCopy = ::copyText,
                        onSaveNote = ::saveCalcToNote
                    )
                }
            }

            // --- 4. IMAGE TOOLS (6) ---
            SmartToolId.IMAGE_COMPRESSOR -> ImageCompressorView(onNotify)
            SmartToolId.IMAGE_RESIZER -> ImageResizerView(onNotify)
            SmartToolId.IMAGE_TO_BASE64 -> ImageBase64View(::copyText)
            SmartToolId.COLOR_PICKER -> ColorPickerView(::copyText)
            SmartToolId.QR_GENERATOR -> CodeGeneratorView(isQr = true, onNotify = onNotify)
            SmartToolId.BARCODE_GENERATOR -> CodeGeneratorView(isQr = false, onNotify = onNotify)

            // --- 5. DAILY USE TOOLS (3) ---
            SmartToolId.TODO_LIST -> QuickTodoToolView(userId, quickTodos, onSaveQuickTodo, onDeleteQuickTodo)
            SmartToolId.STOPWATCH_TIMER -> StopwatchTimerToolView()
            SmartToolId.RANDOM_NAME_PICKER -> RandomNamePickerView()

            else -> Unit
        }
        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun CalcActionRow(
    resultSummary: String,
    onCopy: (String) -> Unit,
    onSaveNote: (String) -> Unit
) {
    val strings = LocalStrings.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onCopy(resultSummary) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.copyText)
        }
        OutlinedButton(onClick = { onSaveNote(resultSummary) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.saveToNote)
        }
    }
}

@Composable
private fun ImageCompressorView(onNotify: (String) -> Unit) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var origBytes by remember { mutableIntStateOf(0) }
    var quality by remember { mutableFloatStateOf(65f) }
    var compressedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val decoded = withContext(Dispatchers.IO) {
                    val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (raw != null) raw.size to BitmapFactory.decodeByteArray(raw, 0, raw.size) else null
                }
                if (decoded != null) {
                    origBytes = decoded.first
                    bitmap = decoded.second
                }
                busy = false
            }
        }
    }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        val bytes = compressedBytes
        if (uri != null && bytes != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    }
                }
                res.onSuccess { onNotify(strings.exportSuccess) }
            }
        }
    }

    LaunchedEffect(bitmap, quality.toInt()) {
        val bmp = bitmap ?: return@LaunchedEffect
        val q = quality.toInt()
        busy = true
        val pair = withContext(Dispatchers.Default) {
            val bytes = SmartToolsEngine.compressBitmap(bmp, q, Bitmap.CompressFormat.JPEG)
            val prev = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bytes to prev
        }
        compressedBytes = pair.first
        previewBitmap = pair.second
        busy = false
    }

    Button(
        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text(strings.pickImageBtn)
    }

    if (busy) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
    }

    bitmap?.let {
        val bytesSize = (compressedBytes?.size ?: 0) / 1024
        Text("Quality: ${quality.toInt()}% • Original: ${origBytes / 1024} KB → Compressed: $bytesSize KB", fontWeight = FontWeight.Bold)
        Slider(value = quality, onValueChange = { quality = it }, valueRange = 10f..95f)
        previewBitmap?.let { prev ->
            Image(
                bitmap = prev.asImageBitmap(),
                contentDescription = "Compressed preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp))
            )
        }
        Button(
            onClick = { saver.launch("compressed_${System.currentTimeMillis()}.jpg") },
            enabled = compressedBytes != null && !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Compressed JPEG")
        }
    }
}

@Composable
private fun ImageResizerView(onNotify: (String) -> Unit) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var targetW by remember { mutableStateOf("800") }
    var targetH by remember { mutableStateOf("600") }
    var keepAspect by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val bmp = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }
                if (bmp != null) {
                    bitmap = bmp
                    targetW = bmp.width.toString()
                    targetH = bmp.height.toString()
                }
                busy = false
            }
        }
    }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val bmp = bitmap
        val w = targetW.toIntOrNull()
        val h = targetH.toIntOrNull()
        if (uri != null && bmp != null && w != null && h != null) {
            scope.launch {
                busy = true
                val res = withContext(Dispatchers.IO) {
                    val resized = SmartToolsEngine.resizeBitmap(bmp, w, h)
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { resized.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                }
                busy = false
                res.onSuccess { onNotify(strings.exportSuccess) }
            }
        }
    }

    Button(
        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        modifier = Modifier.fillMaxWidth()
    ) { Text(strings.pickImageBtn) }

    if (busy) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)

    bitmap?.let { bmp ->
        Text("Original: ${bmp.width} × ${bmp.height} px")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = keepAspect, onCheckedChange = { keepAspect = it })
            Text("Maintain Aspect Ratio")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = targetW,
                onValueChange = { raw ->
                    val clean = raw.filter { c -> c.isDigit() }
                    targetW = clean
                    val w = clean.toIntOrNull()
                    if (keepAspect && w != null && w > 0 && bmp.width > 0) {
                        targetH = ((w.toFloat() / bmp.width) * bmp.height).toInt().coerceAtLeast(1).toString()
                    }
                },
                label = { Text("Width (px)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = targetH,
                onValueChange = { targetH = it.filter { c -> c.isDigit() } },
                label = { Text("Height (px)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Button(
            onClick = { saver.launch("resized_${System.currentTimeMillis()}.png") },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Resized PNG")
        }
    }
}

@Composable
private fun ImageBase64View(onCopy: (String) -> Unit) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var base64Text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val encoded = withContext(Dispatchers.Default) {
                    val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    if (bmp != null) "data:image/jpeg;base64," + SmartToolsEngine.bitmapToBase64(bmp, 80) else ""
                }
                if (encoded.isNotBlank()) base64Text = encoded
                busy = false
            }
        }
    }

    Button(
        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        modifier = Modifier.fillMaxWidth()
    ) { Text(strings.pickImageBtn) }

    if (busy) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)

    if (base64Text.isNotBlank()) {
        OutlinedTextField(
            value = base64Text.take(1500) + if (base64Text.length > 1500) "... (${base64Text.length} chars)" else "",
            onValueChange = {},
            readOnly = true,
            minLines = 5,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { onCopy(base64Text) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy Full Base64 (${base64Text.length} chars)")
        }
    }
}

@Composable
private fun ColorPickerView(onCopy: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var r by remember { mutableFloatStateOf(4f) }
    var g by remember { mutableFloatStateOf(120f) }
    var b by remember { mutableFloatStateOf(87f) }
    var pickedImage by remember { mutableStateOf<Bitmap?>(null) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }
                if (bmp != null) pickedImage = bmp
            }
        }
    }

    val color = Color(r.toInt(), g.toInt(), b.toInt())
    val hex = String.format(Locale.US, "#%02X%02X%02X", r.toInt(), g.toInt(), b.toInt())

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = color,
        modifier = Modifier.fillMaxWidth().height(90.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = hex,
                color = if ((r * 0.299 + g * 0.587 + b * 0.114) > 160) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        }
    }
    Text("Red: ${r.toInt()}"); Slider(value = r, onValueChange = { r = it }, valueRange = 0f..255f)
    Text("Green: ${g.toInt()}"); Slider(value = g, onValueChange = { g = it }, valueRange = 0f..255f)
    Text("Blue: ${b.toInt()}"); Slider(value = b, onValueChange = { b = it }, valueRange = 0f..255f)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onCopy(hex) }, modifier = Modifier.weight(1f)) { Text("Copy $hex") }
        OutlinedButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.weight(1f)
        ) { Text("Pick from Image") }
    }

    pickedImage?.let { bmp ->
        Text("Tap anywhere on the image to pick color:", style = MaterialTheme.typography.labelMedium)
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Tap to pick pixel color",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(14.dp))
                .onSizeChanged { boxSize = it }
                .pointerInput(bmp) {
                    detectTapGestures { offset ->
                        if (boxSize.width > 0 && boxSize.height > 0) {
                            val px = ((offset.x / boxSize.width) * bmp.width).toInt().coerceIn(0, bmp.width - 1)
                            val py = ((offset.y / boxSize.height) * bmp.height).toInt().coerceIn(0, bmp.height - 1)
                            val pixel = Color(bmp[px, py]).toArgb()
                            r = ((pixel shr 16) and 0xFF).toFloat()
                            g = ((pixel shr 8) and 0xFF).toFloat()
                            b = (pixel and 0xFF).toFloat()
                        }
                    }
                }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CodeGeneratorView(isQr: Boolean, onNotify: (String) -> Unit) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var content by remember(isQr) { mutableStateOf(if (isQr) "https://smartfinancelife.app" else "123456789012") }
    var barcodeFmt by remember(isQr) {
        mutableStateOf(if (isQr) BarcodeFormat.QR_CODE else BarcodeFormat.CODE_128)
    }
    var generatedResult by remember(isQr) { mutableStateOf<Result<Bitmap>?>(null) }

    LaunchedEffect(content, barcodeFmt) {
        generatedResult = withContext(Dispatchers.Default) {
            SmartToolsEngine.generateCodeBitmap(content, barcodeFmt)
        }
    }

    if (!isQr) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(BarcodeFormat.CODE_128, BarcodeFormat.CODE_39, BarcodeFormat.EAN_13).forEach { fmt ->
                FilterChip(selected = barcodeFmt == fmt, onClick = { barcodeFmt = fmt }, label = { Text(fmt.name) })
            }
        }
    }

    OutlinedTextField(
        value = content,
        onValueChange = { content = it },
        label = { Text(if (isQr) "Text or URL for QR Code" else "Value for Barcode") },
        modifier = Modifier.fillMaxWidth()
    )

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val bmp = generatedResult?.getOrNull()
        if (uri != null && bmp != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                }
                res.onSuccess { onNotify(strings.exportSuccess) }
            }
        }
    }

    generatedResult?.onSuccess { bmp ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = if (isQr) "Generated QR Code" else "Generated Barcode",
                modifier = Modifier.size(if (isQr) 220.dp else 280.dp, if (isQr) 220.dp else 110.dp)
            )
        }
        Button(
            onClick = { saver.launch("${if (isQr) "qr" else "barcode"}_${System.currentTimeMillis()}.png") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save PNG")
        }
    }?.onFailure { err ->
        Text(err.localizedMessage ?: "Invalid input for format", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun QuickTodoToolView(
    userId: Long,
    todos: List<QuickTodoEntity>,
    onSave: (QuickTodoEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    val strings = LocalStrings.current
    var newTodo by remember { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = newTodo,
            onValueChange = { newTodo = it },
            placeholder = { Text("Add quick to-do item...") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Button(onClick = {
            if (newTodo.isNotBlank()) {
                onSave(QuickTodoEntity(ownerUserId = userId, title = newTodo.trim()))
                newTodo = ""
            }
        }) {
            Icon(Icons.Default.Add, contentDescription = strings.save)
        }
    }
    todos.forEach { item ->
        LedgerCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.isDone, onCheckedChange = { onSave(item.copy(isDone = it)) })
                Text(
                    text = item.title,
                    modifier = Modifier.weight(1f),
                    textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                )
                IconButton(onClick = { onDelete(item.id) }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun StopwatchTimerToolView() {
    var mode by remember { mutableIntStateOf(0) } // 0=Stopwatch, 1=Timer
    var swRunning by remember { mutableStateOf(false) }
    var swMillis by remember { mutableLongStateOf(0L) }
    val laps = remember { mutableStateListOf<String>() }

    var timerSecondsInput by remember { mutableStateOf("60") }
    var timerRemainingMillis by remember { mutableLongStateOf(60_000L) }
    var timerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(swRunning) {
        var last = System.currentTimeMillis()
        while (swRunning) {
            delay(50)
            val now = System.currentTimeMillis()
            swMillis += (now - last)
            last = now
        }
    }

    LaunchedEffect(timerRunning) {
        var last = System.currentTimeMillis()
        while (timerRunning && timerRemainingMillis > 0L) {
            delay(100)
            val now = System.currentTimeMillis()
            timerRemainingMillis = (timerRemainingMillis - (now - last)).coerceAtLeast(0L)
            last = now
            if (timerRemainingMillis == 0L) timerRunning = false
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Stopwatch") })
        FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Countdown Timer") })
    }

    if (mode == 0) {
        val totalSec = swMillis / 1000
        val mins = totalSec / 60
        val secs = totalSec % 60
        val centis = (swMillis % 1000) / 10
        val display = String.format(Locale.US, "%02d:%02d.%02d", mins, secs, centis)
        LedgerCard(modifier = Modifier.fillMaxWidth()) {
            Text(display, fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { swRunning = !swRunning }, modifier = Modifier.weight(1f)) {
                Icon(if (swRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (swRunning) "Pause" else "Start")
            }
            OutlinedButton(onClick = { laps.add(0, display) }, enabled = swMillis > 0) {
                Icon(Icons.Default.Flag, contentDescription = "Lap")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Lap")
            }
            OutlinedButton(onClick = { swRunning = false; swMillis = 0L; laps.clear() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset")
            }
        }
        laps.forEachIndexed { idx, l ->
            Text("Lap ${laps.size - idx}: $l", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
        }
    } else {
        val secsLeft = (timerRemainingMillis + 999) / 1000
        val m = secsLeft / 60
        val s = secsLeft % 60
        LedgerCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = String.format(Locale.US, "%02d:%02d", m, s),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (timerRemainingMillis == 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
        OutlinedTextField(
            value = timerSecondsInput,
            onValueChange = { raw ->
                val clean = raw.filter { c -> c.isDigit() }
                timerSecondsInput = clean
                if (!timerRunning) timerRemainingMillis = (clean.toLongOrNull() ?: 0L) * 1000L
            },
            label = { Text("Seconds") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (timerRemainingMillis == 0L) timerRemainingMillis = (timerSecondsInput.toLongOrNull() ?: 60L) * 1000L
                timerRunning = !timerRunning
            }, modifier = Modifier.weight(1f)) {
                Text(if (timerRunning) "Pause" else "Start Timer")
            }
            OutlinedButton(onClick = {
                timerRunning = false
                timerRemainingMillis = (timerSecondsInput.toLongOrNull() ?: 60L) * 1000L
            }) { Text("Reset") }
        }
    }
}

@Composable
private fun RandomNamePickerView() {
    val scope = rememberCoroutineScope()
    var namesInput by remember { mutableStateOf("Rahim\nKarim\nSalma\nNadia\nTariq\nAyesha") }
    var pickedWinners by remember { mutableStateOf<List<String>>(emptyList()) }
    var spinningCandidate by remember { mutableStateOf<String?>(null) }
    var isSpinning by remember { mutableStateOf(false) }
    var removeWinner by remember { mutableStateOf(false) }
    var excludePrevious by remember { mutableStateOf(true) }
    var winnerCount by remember { mutableStateOf(1) }
    var previousWinner by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf<List<String>>(emptyList()) }

    val parsedPool = remember(namesInput) {
        namesInput.split('\n', ',', ';').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    OutlinedTextField(
        value = namesInput,
        onValueChange = { namesInput = it },
        label = { Text("Enter names (one per line or comma separated) • ${parsedPool.size} entries") },
        minLines = 5,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("random_picker_input")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Winners:", style = MaterialTheme.typography.labelLarge)
        (1..4).forEach { count ->
            FilterChip(
                selected = winnerCount == count,
                onClick = { if (!isSpinning) winnerCount = count },
                label = { Text("$count") }
            )
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = excludePrevious,
            onCheckedChange = { if (!isSpinning) excludePrevious = it }
        )
        Text("Avoid repeating last winner (when > 1 name)")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = removeWinner,
            onCheckedChange = { if (!isSpinning) removeWinner = it }
        )
        Text("Remove picked winner(s) from list")
    }

    Button(
        enabled = parsedPool.isNotEmpty() && !isSpinning,
        onClick = {
            val basePool = if (excludePrevious && previousWinner != null && parsedPool.size > winnerCount) {
                parsedPool.filterNot { it == previousWinner }
            } else {
                parsedPool
            }
            if (basePool.isEmpty()) return@Button

            isSpinning = true
            pickedWinners = emptyList()
            scope.launch {
                val ticks = 22
                for (step in 0 until ticks) {
                    val delayMs = when {
                        step < 10 -> 45L
                        step < 16 -> 85L
                        step < 20 -> 140L
                        else -> 210L
                    }
                    spinningCandidate = basePool[step % basePool.size]
                    kotlinx.coroutines.delay(delayMs)
                }
                val finalWinners = basePool.shuffled().take(winnerCount.coerceAtMost(basePool.size))
                spinningCandidate = null
                pickedWinners = finalWinners
                previousWinner = finalWinners.firstOrNull()
                history = (finalWinners + history).take(12)
                if (removeWinner) {
                    namesInput = parsedPool.filterNot { it in finalWinners }.joinToString("\n")
                }
                isSpinning = false
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("random_picker_spin_btn")
    ) {
        Icon(Icons.Default.Casino, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text(if (isSpinning) "Spinning Wheel..." else "Spin & Pick Winner")
    }

    val displayBannerText = spinningCandidate ?: pickedWinners.joinToString(" • ")
    if (displayBannerText.isNotEmpty()) {
        val boxColor = if (isSpinning) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = boxColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isSpinning) "DRAWING..." else if (pickedWinners.size > 1) "SELECTED WINNERS" else "SELECTED WINNER",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = displayBannerText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.testTag("random_picker_result")
                )
            }
        }
    }

    if (history.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Picks", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { history = emptyList() }) { Text("Clear") }
            }
            Text(
                history.joinToString("  •  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

