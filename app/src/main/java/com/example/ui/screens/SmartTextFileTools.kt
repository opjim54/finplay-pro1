package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.SmartToolsEngine
import com.example.domain.TextCaseMode
import com.example.i18n.LocalStrings
import com.example.ui.components.LedgerCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartTextAndFileToolView(
    tool: SmartToolId,
    onSaveToNote: (title: String, content: String) -> Unit = { _, _ -> },
    onNotify: (String) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputText by remember(tool) {
        mutableStateOf(
            when (tool) {
                SmartToolId.CSV_TO_JSON -> "name,amount,category\nRice,1200,Food\nFuel,500,Transport"
                SmartToolId.JSON_FORMATTER -> "{\"app\":\"Smart Finance Life\",\"profiles\":[\"Farm\",\"Family\"]}"
                else -> ""
            }
        )
    }
    var outputText by remember(tool) { mutableStateOf("") }
    var busy by remember(tool) { mutableStateOf(false) }
    var selectedImageUris by remember(tool) { mutableStateOf<List<Uri>>(emptyList()) }
    var loremParagraphs by remember(tool) { mutableFloatStateOf(3f) }

    val saveTxtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val content = outputText.ifBlank { inputText }
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            }.onSuccess { onNotify(strings.exportSuccess) }
                .onFailure { onNotify(it.localizedMessage ?: "Failed") }
        }
    }

    val saveJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val content = outputText.ifBlank { inputText }
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            }.onSuccess { onNotify(strings.exportSuccess) }
                .onFailure { onNotify(it.localizedMessage ?: "Failed") }
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val res = withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        if (tool == SmartToolId.IMAGE_TO_PDF) {
                            SmartToolsEngine.writeImagesToPdf(context, selectedImageUris, out)
                        } else {
                            SmartToolsEngine.writeTextToPdf(inputText, out)
                        }
                    } ?: Result.failure(IllegalStateException("Cannot open destination"))
                }
                busy = false
                res.onSuccess { onNotify(strings.exportSuccess) }
                    .onFailure { onNotify(it.localizedMessage ?: "PDF failed") }
            }
        }
    }

    val openPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val res = withContext(Dispatchers.IO) { SmartToolsEngine.extractTextFromPdf(context, uri) }
                busy = false
                res.onSuccess { outputText = it.ifBlank { "(No selectable text found in PDF)" } }
                    .onFailure { outputText = "Error: ${it.localizedMessage}" }
            }
        }
    }

    val openCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val loaded = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                    }
                }
                busy = false
                loaded.onSuccess { inputText = it }
            }
        }
    }

    val multiImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) selectedImageUris = uris
    }

    fun copyToClipboard(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(tool.title, text))
        onNotify(strings.copiedMsg)
    }

    fun saveTextToKeepNote(content: String) {
        if (content.isNotBlank()) {
            onSaveToNote(tool.title, content)
            onNotify(strings.savedToNoteMsg)
        }
    }

    fun pasteFromClipboard(): String {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (busy) {
            CircularProgressIndicator()
        }

        when (tool) {
            SmartToolId.STYLISH_TEXT_STUDIO -> {
                StylishTextStudioView(
                    onCopy = { copyToClipboard(it) },
                    onSaveToNote = { saveTextToKeepNote(it) },
                    onNotify = onNotify
                )
            }

            SmartToolId.WORD_COUNTER, SmartToolId.CHAR_COUNTER -> {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Paste or type your text") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                val stats = remember(inputText) { SmartToolsEngine.analyzeText(inputText) }
                val readMins = ceil(stats.words / 200.0).toInt().coerceAtLeast(if (stats.words > 0) 1 else 0)
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBadge("${strings.wordCountLabel}", stats.words.toString())
                        StatBadge("${strings.charCountLabel} (incl. spaces)", stats.charsWithSpaces.toString())
                        StatBadge("${strings.charCountLabel} (no spaces)", stats.charsWithoutSpaces.toString())
                        StatBadge("Sentences", stats.sentences.toString())
                        StatBadge("Lines", stats.lines.toString())
                        StatBadge("Paragraphs", stats.paragraphs.toString())
                        StatBadge("Read Time", "$readMins min")
                    }
                }
                if (inputText.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val summary = "${inputText.trim()}\n\n[Words: ${stats.words} | Chars: ${stats.charsWithSpaces} | Read: ${readMins}m]"
                            saveTextToKeepNote(summary)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.saveToNote)
                    }
                }
            }

            SmartToolId.TEXT_TO_TXT, SmartToolId.WORD_TO_TXT -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { inputText = pasteFromClipboard() }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.pasteText)
                    }
                    if (tool == SmartToolId.WORD_TO_TXT) {
                        Button(onClick = { outputText = SmartToolsEngine.cleanWordPasteToTxt(inputText) }) {
                            Text("Clean Word Text")
                        }
                    }
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text(if (tool == SmartToolId.WORD_TO_TXT) "Paste formatted Word text" else "Enter text to save as .txt") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                if (outputText.isNotBlank()) {
                    OutlinedTextField(
                        value = outputText,
                        onValueChange = { outputText = it },
                        label = { Text("Cleaned .TXT output") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val clean = if (tool == SmartToolId.WORD_TO_TXT && outputText.isBlank()) {
                                SmartToolsEngine.cleanWordPasteToTxt(inputText).also { outputText = it }
                            } else outputText.ifBlank { inputText }
                            if (clean.isNotBlank()) {
                                saveTxtLauncher.launch("smart_document_${System.currentTimeMillis()}.txt")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.exportTxt)
                    }
                    OutlinedButton(onClick = { copyToClipboard(outputText.ifBlank { inputText }) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                    }
                }
            }

            SmartToolId.CASE_CONVERTER -> {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Enter text") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { outputText = SmartToolsEngine.convertCase(inputText, TextCaseMode.UPPER) }) { Text("UPPERCASE") }
                    Button(onClick = { outputText = SmartToolsEngine.convertCase(inputText, TextCaseMode.LOWER) }) { Text("lowercase") }
                    Button(onClick = { outputText = SmartToolsEngine.convertCase(inputText, TextCaseMode.TITLE) }) { Text("Title Case") }
                    Button(onClick = { outputText = SmartToolsEngine.convertCase(inputText, TextCaseMode.SENTENCE) }) { Text("Sentence case") }
                    OutlinedButton(onClick = { outputText = SmartToolsEngine.convertCase(inputText, TextCaseMode.TOGGLE) }) { Text("tOGGLE cASE") }
                }
                OutlinedTextField(
                    value = outputText,
                    onValueChange = { outputText = it },
                    label = { Text("Converted Result") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { copyToClipboard(outputText) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.copyText)
                    }
                    OutlinedButton(
                        onClick = { saveTextToKeepNote(outputText) },
                        enabled = outputText.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.saveToNote)
                    }
                }
            }

            SmartToolId.REMOVE_EXTRA_SPACES -> {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Text with extra spaces / blank lines") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { outputText = SmartToolsEngine.removeExtraSpaces(inputText) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Clean Extra Spaces") }
                OutlinedTextField(
                    value = outputText,
                    onValueChange = { outputText = it },
                    label = { Text("Cleaned Output") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { copyToClipboard(outputText) }, modifier = Modifier.weight(1f)) {
                        Text(strings.copyText)
                    }
                    OutlinedButton(
                        onClick = { saveTextToKeepNote(outputText) },
                        enabled = outputText.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.saveToNote)
                    }
                }
            }

            SmartToolId.TEXT_REVERSER -> {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Input text") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { outputText = SmartToolsEngine.reverseText(inputText, byWordsOnly = false) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Reverse Letters") }
                    OutlinedButton(
                        onClick = { outputText = SmartToolsEngine.reverseText(inputText, byWordsOnly = true) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Reverse Words") }
                }
                OutlinedTextField(
                    value = outputText,
                    onValueChange = { outputText = it },
                    label = { Text("Reversed Output") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { copyToClipboard(outputText) }, modifier = Modifier.weight(1f)) { Text(strings.copyText) }
                    OutlinedButton(
                        onClick = { saveTextToKeepNote(outputText) },
                        enabled = outputText.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.saveToNote)
                    }
                }
            }

            SmartToolId.LOREM_IPSUM -> {
                Text("Paragraphs: ${loremParagraphs.roundToInt()}", fontWeight = FontWeight.Bold)
                Slider(
                    value = loremParagraphs,
                    onValueChange = { loremParagraphs = it },
                    valueRange = 1f..10f,
                    steps = 8
                )
                Button(
                    onClick = { outputText = SmartToolsEngine.generateLoremIpsum(loremParagraphs.roundToInt()) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(strings.generateBtn) }
                OutlinedTextField(
                    value = outputText,
                    onValueChange = { outputText = it },
                    label = { Text("Generated Lorem Ipsum") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { copyToClipboard(outputText) }, modifier = Modifier.weight(1f)) { Text(strings.copyText) }
                    OutlinedButton(
                        onClick = { saveTextToKeepNote(outputText) },
                        enabled = outputText.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.saveToNote)
                    }
                }
            }

            SmartToolId.TEXT_TO_PDF -> {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Enter or paste document content") },
                    minLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            savePdfLauncher.launch("smart_doc_${System.currentTimeMillis()}.pdf")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.exportPdfBtn)
                }
            }

            SmartToolId.IMAGE_TO_PDF -> {
                Button(
                    onClick = {
                        multiImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${strings.pickImageBtn} (${selectedImageUris.size})")
                }
                if (selectedImageUris.isNotEmpty()) {
                    Text("${selectedImageUris.size} images ready for PDF conversion.", style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { savePdfLauncher.launch("images_bundle_${System.currentTimeMillis()}.pdf") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.exportPdfBtn)
                    }
                }
            }

            SmartToolId.PDF_TO_TEXT -> {
                Button(
                    onClick = { openPdfLauncher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick PDF File")
                }
                OutlinedTextField(
                    value = outputText,
                    onValueChange = { outputText = it },
                    label = { Text("Extracted Text") },
                    minLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { copyToClipboard(outputText) }, modifier = Modifier.weight(1f)) { Text(strings.copyText) }
                    OutlinedButton(
                        onClick = { if (outputText.isNotBlank()) saveTxtLauncher.launch("extracted_${System.currentTimeMillis()}.txt") },
                        modifier = Modifier.weight(1f)
                    ) { Text(strings.exportTxt) }
                    OutlinedButton(
                        onClick = { saveTextToKeepNote(outputText) },
                        enabled = outputText.isNotBlank()
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = strings.saveToNote)
                    }
                }
            }

            SmartToolId.CSV_TO_JSON -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { openCsvLauncher.launch(arrayOf("text/*", "text/csv")) }) {
                        Text(strings.pickFileBtn)
                    }
                    Button(onClick = {
                        SmartToolsEngine.csvToJson(inputText)
                            .onSuccess { outputText = it }
                            .onFailure { outputText = "Error: ${it.localizedMessage}" }
                    }) { Text("Convert to JSON") }
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("CSV Input") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = outputText,
                    onValueChange = { outputText = it },
                    label = { Text("JSON Output") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { copyToClipboard(outputText) }, modifier = Modifier.weight(1f)) { Text(strings.copyText) }
                    OutlinedButton(
                        onClick = { if (outputText.isNotBlank()) saveJsonLauncher.launch("data_${System.currentTimeMillis()}.json") },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save .JSON") }
                    OutlinedButton(
                        onClick = { saveTextToKeepNote(outputText) },
                        enabled = outputText.isNotBlank()
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = strings.saveToNote)
                    }
                }
            }

            SmartToolId.JSON_FORMATTER -> {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Raw JSON") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            SmartToolsEngine.formatJson(inputText, minify = false)
                                .onSuccess { outputText = it }
                                .onFailure { outputText = "Invalid JSON: ${it.localizedMessage}" }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Beautify") }
                    OutlinedButton(
                        onClick = {
                            SmartToolsEngine.formatJson(inputText, minify = true)
                                .onSuccess { outputText = it }
                                .onFailure { outputText = "Invalid JSON: ${it.localizedMessage}" }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Minify") }
                }
                OutlinedTextField(
                    value = outputText,
                    onValueChange = { outputText = it },
                    label = { Text("Formatted JSON") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { copyToClipboard(outputText) }, modifier = Modifier.weight(1f)) { Text(strings.copyText) }
                    OutlinedButton(
                        onClick = { saveTextToKeepNote(outputText) },
                        enabled = outputText.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = strings.saveToNote)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.saveToNote)
                    }
                }
            }

            else -> Unit
        }
        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun StatBadge(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
