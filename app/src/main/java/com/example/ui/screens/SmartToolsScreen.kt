package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.QuickTodoEntity
import com.example.i18n.LocalStrings
import com.example.ui.components.BouncySurface
import com.example.ui.theme.LocalFinanceColors

enum class SmartToolCategory {
    ALL, FINANCE_PRO, TEXT, FILE, CALCULATOR, IMAGE, DEV_SEC, PRODUCTIVITY, DAILY
}

enum class SmartToolId(
    val title: String,
    val subtitle: String,
    val category: SmartToolCategory,
    val icon: ImageVector,
    val accentHex: Long,
    val isProNew: Boolean = false
) {
    // 1. Text Tools (7)
    TEXT_TO_TXT("Text to TXT", "Save plain text directly as .txt file", SmartToolCategory.TEXT, Icons.Default.Description, 0xFF047857),
    WORD_COUNTER("Word Counter", "Count words, lines, sentences & reading time", SmartToolCategory.TEXT, Icons.Default.Numbers, 0xFF0284C7),
    CHAR_COUNTER("Character Counter", "Count characters with/without spaces & bytes", SmartToolCategory.TEXT, Icons.Default.FormatSize, 0xFF0D9488),
    CASE_CONVERTER("Case Converter", "UPPER, lower, Title, Sentence & Toggle case", SmartToolCategory.TEXT, Icons.Default.Abc, 0xFF7C3AED),
    REMOVE_EXTRA_SPACES("Remove Extra Spaces", "Clean messy spaces, tabs & blank lines", SmartToolCategory.TEXT, Icons.Default.SpaceBar, 0xFFD97706),
    TEXT_REVERSER("Text Reverser", "Reverse characters or reverse word order", SmartToolCategory.TEXT, Icons.Default.SwapHoriz, 0xFFDB2777),
    LOREM_IPSUM("Lorem Ipsum Generator", "Generate clean placeholder paragraphs", SmartToolCategory.TEXT, Icons.Default.TextFields, 0xFF4F46E5),
    STYLISH_TEXT_STUDIO("Stylish Text & Symbols", "28+ Unicode fonts, symbols, banners & frames", SmartToolCategory.TEXT, Icons.Default.Style, 0xFFEC4899, isProNew = true),

    // 2. File Converter Tools (6)
    TEXT_TO_PDF("Text to PDF", "Convert multi-page text into a real PDF file", SmartToolCategory.FILE, Icons.Default.PictureAsPdf, 0xFFDC2626),
    IMAGE_TO_PDF("Image to PDF", "Combine gallery photos into one PDF", SmartToolCategory.FILE, Icons.Default.Image, 0xFFEA580C),
    PDF_TO_TEXT("PDF to Text", "Extract readable text from any PDF file", SmartToolCategory.FILE, Icons.Default.Transform, 0xFF2563EB),
    WORD_TO_TXT("Word to TXT", "Clean copied Word text and export .txt", SmartToolCategory.FILE, Icons.Default.Description, 0xFF0891B2),
    CSV_TO_JSON("CSV to JSON", "Convert spreadsheet CSV data to JSON", SmartToolCategory.FILE, Icons.Default.DataObject, 0xFF059669),
    JSON_FORMATTER("JSON Formatter", "Beautify, validate & minify JSON payloads", SmartToolCategory.FILE, Icons.Default.Code, 0xFF7C3AED),

    // 3. Calculator Tools (5)
    AGE_CALC("Age Calculator", "Exact age & next birthday countdown", SmartToolCategory.CALCULATOR, Icons.Default.Cake, 0xFFDB2777),
    BMI_CALC("BMI Calculator", "Body Mass Index & healthy range check", SmartToolCategory.CALCULATOR, Icons.Default.MonitorWeight, 0xFF059669),
    EMI_CALC("EMI / Loan Calculator", "Monthly installment & total interest breakdown", SmartToolCategory.CALCULATOR, Icons.Default.RequestQuote, 0xFF0284C7),
    PERCENT_CALC("Percentage Calculator", "Percent of total, change & discount math", SmartToolCategory.CALCULATOR, Icons.Default.Percent, 0xFFD97706),
    UNIT_CONVERTER("Unit Converter", "KG↔Pound, Feet↔Meter, Temp, Land & more", SmartToolCategory.CALCULATOR, Icons.Default.Straighten, 0xFF4F46E5),

    // 4. Image Tools (6)
    IMAGE_COMPRESSOR("Image Compressor", "Reduce photo KB size with quality slider", SmartToolCategory.IMAGE, Icons.Default.Compress, 0xFF047857),
    IMAGE_RESIZER("Image Resizer", "Resize width & height with aspect ratio", SmartToolCategory.IMAGE, Icons.Default.PhotoSizeSelectLarge, 0xFF0284C7),
    IMAGE_TO_BASE64("Image to Base64", "Encode any image to Base64 string", SmartToolCategory.IMAGE, Icons.Default.Code, 0xFF7C3AED),
    COLOR_PICKER("Color Picker", "RGB/HEX mixer & image eyedropper", SmartToolCategory.IMAGE, Icons.Default.ColorLens, 0xFFEC4899),
    QR_GENERATOR("QR Code Generator", "Generate crisp QR code & export PNG", SmartToolCategory.IMAGE, Icons.Default.QrCode2, 0xFF0F766E),
    BARCODE_GENERATOR("Barcode Generator", "Generate CODE 128 / EAN 13 / CODE 39", SmartToolCategory.IMAGE, Icons.Default.QrCode, 0xFF1D4ED8),

    // 5. Daily Use Tools (3)
    TODO_LIST("To-Do List App", "Synced daily checklist backed up in Cloud", SmartToolCategory.DAILY, Icons.Default.Checklist, 0xFF059669),
    STOPWATCH_TIMER("Stopwatch & Timer", "Lap stopwatch & countdown timer", SmartToolCategory.DAILY, Icons.Default.Timer, 0xFFD97706),
    RANDOM_NAME_PICKER("Random Name Picker", "Pick random winners cleanly", SmartToolCategory.DAILY, Icons.Default.Casino, 0xFF7C3AED),

    // 6. Finance Pro Tools (3 New)
    COMPOUND_INTEREST("Compound Interest", "Project savings growth & interest earned", SmartToolCategory.FINANCE_PRO, Icons.Default.Savings, 0xFF046C4E, isProNew = true),
    SPLIT_BILL("Split Bill & Tip", "Split restaurant bills with tip & VAT", SmartToolCategory.FINANCE_PRO, Icons.Default.CallSplit, 0xFF0F766E, isProNew = true),
    DISCOUNT_VAT("Discount & VAT", "Calculate stacked discounts + net tax", SmartToolCategory.FINANCE_PRO, Icons.Default.LocalOffer, 0xFFD4AF37, isProNew = true),

    // 7. Security & Dev Tools (4 New)
    PASSWORD_GEN("Password Generator", "Cryptographic random passwords + entropy", SmartToolCategory.DEV_SEC, Icons.Default.Password, 0xFF059669, isProNew = true),
    UUID_HASH_GEN("UUID & Hash Toolkit", "Generate UUID v4 + SHA-256 / SHA-512 / MD5", SmartToolCategory.DEV_SEC, Icons.Default.Fingerprint, 0xFF2563EB, isProNew = true),
    URL_ENCODER("URL Encoder / Decoder", "Encode & decode query strings safely", SmartToolCategory.DEV_SEC, Icons.Default.Link, 0xFF7C3AED, isProNew = true),
    BASE64_REGEX("Base64 & Regex", "Text Base64 codec & live Regex matcher", SmartToolCategory.DEV_SEC, Icons.Default.Code, 0xFF0891B2, isProNew = true),

    // 8. Productivity Tools (3 New)
    MARKDOWN_PREVIEWER("Markdown Previewer", "Write & preview styled Markdown live", SmartToolCategory.PRODUCTIVITY, Icons.Default.Visibility, 0xFF047857, isProNew = true),
    INVOICE_MAKER("Mini Invoice Maker", "Create itemized invoices & export PDF", SmartToolCategory.PRODUCTIVITY, Icons.Default.Receipt, 0xFFB45309, isProNew = true),
    POMODORO_TIMER("Pomodoro Focus Timer", "25/5 & 50/10 deep focus sessions", SmartToolCategory.PRODUCTIVITY, Icons.Default.HourglassTop, 0xFFDC2626, isProNew = true)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartToolsScreen(
    userId: Long,
    quickTodos: List<QuickTodoEntity>,
    onSaveQuickTodo: (QuickTodoEntity) -> Unit,
    onDeleteQuickTodo: (Long) -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenGamesArena: () -> Unit = {},
    onSaveToNote: (title: String, content: String) -> Unit = { _, _ -> },
    onNotify: (String) -> Unit
) {
    val strings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    var selectedCategory by remember { mutableStateOf(SmartToolCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var activeTool by remember { mutableStateOf<SmartToolId?>(null) }

    if (activeTool != null) {
        BackHandler { activeTool = null }
        val tool = activeTool!!
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { activeTool = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.cancel)
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(tool.accentHex).copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tool.icon, contentDescription = null, tint = Color(tool.accentHex), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(tool.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(tool.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                AnimatedContent(targetState = tool, label = "tool_runner") { currentTool ->
                    when (currentTool.category) {
                        SmartToolCategory.TEXT, SmartToolCategory.FILE -> SmartTextAndFileToolView(
                            tool = currentTool,
                            onSaveToNote = onSaveToNote,
                            onNotify = onNotify
                        )
                        SmartToolCategory.FINANCE_PRO, SmartToolCategory.DEV_SEC, SmartToolCategory.PRODUCTIVITY ->
                            SmartProToolView(
                                tool = currentTool,
                                onSaveToNote = onSaveToNote,
                                onNotify = onNotify
                            )
                        else -> SmartCalcImageDailyToolView(
                            tool = currentTool,
                            userId = userId,
                            quickTodos = quickTodos,
                            onSaveQuickTodo = onSaveQuickTodo,
                            onDeleteQuickTodo = onDeleteQuickTodo,
                            onSaveToNote = onSaveToNote,
                            onNotify = onNotify
                        )
                    }
                }
            }
        }
        return
    }

    val categories = listOf(
        SmartToolCategory.ALL to "All (${SmartToolId.entries.size})",
        SmartToolCategory.FINANCE_PRO to "Finance Pro",
        SmartToolCategory.PRODUCTIVITY to "Productivity",
        SmartToolCategory.DEV_SEC to "Security & Dev",
        SmartToolCategory.TEXT to strings.catTextTools,
        SmartToolCategory.FILE to strings.catFileTools,
        SmartToolCategory.CALCULATOR to strings.catCalcTools,
        SmartToolCategory.IMAGE to strings.catImageTools,
        SmartToolCategory.DAILY to strings.catDailyTools
    )

    val visibleTools = remember(selectedCategory, searchQuery) {
        val byCat = if (selectedCategory == SmartToolCategory.ALL) SmartToolId.entries
        else SmartToolId.entries.filter { it.category == selectedCategory }
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) byCat else byCat.filter {
            it.title.lowercase().contains(q) ||
                it.subtitle.lowercase().contains(q) ||
                it.category.name.lowercase().contains(q)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.smartToolsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = financeColors.champagneGold.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${SmartToolId.entries.size} Tools",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = financeColors.champagneGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onOpenAiAssistant)
                        .testTag("tools_open_ai_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF06231C), Color(0xFF046C4E), Color(0xFF0F766E))
                                )
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFDE68A))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.chatWithAi,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Auto-Failover • Gemini • xKiro • Custom Provider",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD1FAE5)
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onOpenGamesArena)
                        .testTag("tools_open_games_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E1B4B), Color(0xFF4338CA), Color(0xFF0F766E))
                                )
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = Color(0xFFFDE68A))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.gamesArenaTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = strings.gamesArenaSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE0E7FF)
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchToolsHint) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tools_search_input")
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { (cat, label) ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(label) }
                        )
                    }
                }
            }

            val chunked = visibleTools.chunked(2)
            items(chunked) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { tool ->
                        ToolGridCard(
                            tool = tool,
                            onClick = { activeTool = tool },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun ToolGridCard(
    tool: SmartToolId,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(tool.accentHex)
    val financeColors = LocalFinanceColors.current
    BouncySurface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, financeColors.cardBorder),
        modifier = modifier.testTag("tool_${tool.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(tool.icon, contentDescription = tool.title, tint = accent, modifier = Modifier.size(20.dp))
                }
                if (tool.isProNew) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = financeColors.champagneGold.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = financeColors.champagneGold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = tool.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tool.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
