package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.AiChatMessageEntity
import com.example.data.local.AiProviderEntity
import com.example.data.local.AiProviderKind
import com.example.data.local.allModelsList
import com.example.domain.DateTimeUtil
import com.example.i18n.LocalStrings
import com.example.ui.components.LedgerCard
import com.example.ui.components.LivePulseDot
import com.example.ui.theme.LocalFinanceColors
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.mutableStateListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    userId: Long,
    providers: List<AiProviderEntity>,
    activeProvider: AiProviderEntity?,
    messages: List<AiChatMessageEntity>,
    busy: Boolean,
    errorText: String?,
    autoFailoverEnabled: Boolean = true,
    profileContextSummary: String = "",
    onBack: () -> Unit,
    onToggleAutoFailover: (Boolean) -> Unit = {},
    onSelectProvider: (Long) -> Unit,
    onSelectModel: (AiProviderEntity, String) -> Unit = { _, _ -> },
    onSaveProvider: (AiProviderEntity) -> Unit,
    onDeleteProvider: (Long) -> Unit,
    onSendMessage: (String, Uri?) -> Unit,
    onClearChat: () -> Unit,
    onClearError: () -> Unit,
    onSaveReplyToNote: (String) -> Unit,
    onNotify: (String) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val financeColors = LocalFinanceColors.current
    var prompt by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showProviderManager by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) attachedImageUri = uri
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(strings.aiAssistantTitle, fontWeight = FontWeight.Bold)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = financeColors.champagneGold.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "PRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = financeColors.champagneGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        activeProvider?.let {
                            Text(
                                text = "${it.name} • ${it.modelName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.cancel)
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        TextButton(onClick = onClearChat) { Text(strings.clearChat) }
                    }
                    IconButton(
                        onClick = { showProviderManager = true },
                        modifier = Modifier.testTag("manage_ai_providers_btn")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = strings.manageProviders)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
            ) {
                if (providers.isEmpty() || activeProvider == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, financeColors.champagneGold)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = strings.aiAssistantTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.configureProviderFirst,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showProviderManager = true },
                            modifier = Modifier.testTag("setup_first_ai_provider_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.addAiProvider)
                        }
                    }
                } else {
                    // Auto-Failover Banner & Switch
                    Surface(
                        color = if (autoFailoverEnabled) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (autoFailoverEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            else financeColors.cardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onToggleAutoFailover(!autoFailoverEnabled) }
                            .testTag("ai_auto_switch_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Auto-Switch Providers",
                                tint = if (autoFailoverEnabled) financeColors.champagneGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Switch on Limit (${providers.size} active)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (autoFailoverEnabled) "Silently fails over if quota/rate hits limit" else "Stops if active provider hits limit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoFailoverEnabled,
                                onCheckedChange = onToggleAutoFailover,
                                modifier = Modifier.testTag("ai_auto_switch_toggle")
                            )
                        }
                    }

                    // Quick provider selector chips
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(providers, key = { it.id }) { prov ->
                            FilterChip(
                                selected = prov.id == activeProvider.id,
                                onClick = { onSelectProvider(prov.id) },
                                label = { Text("${prov.name} (${prov.providerKind.name})") }
                            )
                        }
                        item {
                            OutlinedButton(onClick = { showProviderManager = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.manageProviders)
                            }
                        }
                    }

                    // Active Provider Models Selector Strip
                    val activeModels = remember(activeProvider) { activeProvider.allModelsList() }
                    if (activeModels.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("ai_chat_model_selector_row")
                        ) {
                            item {
                                Text(
                                    text = "Model:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = financeColors.champagneGold
                                )
                            }
                            items(activeModels, key = { it }) { mId ->
                                FilterChip(
                                    selected = mId == activeProvider.modelName,
                                    onClick = { onSelectModel(activeProvider, mId) },
                                    label = {
                                        Text(
                                            text = mId,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (mId == activeProvider.modelName) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag("chat_model_chip_$mId")
                                )
                            }
                        }
                    }

                    // Chat timeline
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isUser = msg.role == "USER"
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = if (isUser) 18.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 18.dp
                                    ),
                                    color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    border = if (!isUser) BorderStroke(1.dp, financeColors.cardBorder) else null,
                                    modifier = Modifier.widthIn(max = 520.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (!isUser && msg.servedByProviderName.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (msg.wasAutoSwitched) {
                                                    financeColors.champagneGold.copy(alpha = 0.18f)
                                                } else {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                }
                                            ) {
                                                Text(
                                                    text = if (msg.wasAutoSwitched) {
                                                        "Auto-switched → ${msg.servedByProviderName}"
                                                    } else {
                                                        "Via ${msg.servedByProviderName}"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (msg.wasAutoSwitched) financeColors.champagneGold else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (!msg.imageBase64Thumb.isNullOrBlank()) {
                                            val imgBitmap = remember(msg.imageBase64Thumb) {
                                                runCatching {
                                                    val bytes = Base64.decode(msg.imageBase64Thumb, Base64.DEFAULT)
                                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                                }.getOrNull()
                                            }
                                            imgBitmap?.let { bmp ->
                                                Image(
                                                    bitmap = bmp,
                                                    contentDescription = "Attached image",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(160.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                )
                                            }
                                        }
                                        Text(
                                            text = msg.textContent,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = DateTimeUtil.formatTime(msg.timestamp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                            if (!isUser) {
                                                Row {
                                                    IconButton(
                                                        onClick = {
                                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                            cm.setPrimaryClip(ClipData.newPlainText("AI", msg.textContent))
                                                            onNotify(strings.copiedMsg)
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = strings.copyText, modifier = Modifier.size(15.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            onSaveReplyToNote(msg.textContent)
                                                            onNotify(strings.savedToNoteMsg)
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.NoteAdd, contentDescription = strings.saveToNote, modifier = Modifier.size(15.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!errorText.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = errorText,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onClearError, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    if (attachedImageUri != null) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                AsyncImage(
                                    model = attachedImageUri,
                                    contentDescription = strings.attachImage,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))
                                )
                                IconButton(
                                    onClick = { attachedImageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(22.dp)
                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    val quickPrompts = remember(profileContextSummary) {
                        val ctx = if (profileContextSummary.isNotBlank()) "\n\nContext:\n$profileContextSummary" else ""
                        listOf(
                            "Analyze My Finances" to "Analyze my current financial health, spending leaks, and give 3 actionable savings steps.$ctx",
                            "Monthly Budget Plan" to "Create a practical monthly 50/30/20 budget and emergency fund plan based on my profile.$ctx",
                            "Scan Receipt / Image" to "Extract date, merchant, category, and total amount from this image cleanly in a table.",
                            "Prioritize Tasks" to "Help me organize today's top priorities and productivity schedule cleanly.$ctx"
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickPrompts) { (chipLabel, fullPrompt) ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    if (!busy) {
                                        if (chipLabel == "Scan Receipt / Image" && attachedImageUri == null && activeProvider.supportsVision) {
                                            prompt = fullPrompt
                                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        } else {
                                            onSendMessage(fullPrompt, attachedImageUri)
                                            attachedImageUri = null
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                label = { Text(chipLabel, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    if (busy) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LivePulseDot(color = MaterialTheme.colorScheme.primary, sizeDp = 7.dp)
                            Text(
                                text = if (autoFailoverEnabled && providers.size > 1) {
                                    "${activeProvider.name} (Auto-Switch Armed)..."
                                } else {
                                    "${activeProvider.name} is thinking..."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        tonalElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeProvider.supportsVision) {
                                IconButton(
                                    onClick = {
                                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    modifier = Modifier.testTag("ai_attach_image_btn")
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = strings.attachImage)
                                }
                            }
                            OutlinedTextField(
                                value = prompt,
                                onValueChange = { prompt = it },
                                placeholder = { Text(strings.askAiHint) },
                                maxLines = 4,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_prompt_input")
                            )
                            Button(
                                onClick = {
                                    if (!busy && (prompt.isNotBlank() || attachedImageUri != null)) {
                                        val sendText = prompt
                                        val sendUri = attachedImageUri
                                        prompt = ""
                                        attachedImageUri = null
                                        onSendMessage(sendText, sendUri)
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.testTag("ai_send_btn")
                            ) {
                                if (busy) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProviderManager) {
        AiProviderManagerDialog(
            userId = userId,
            providers = providers,
            onSaveProvider = onSaveProvider,
            onDeleteProvider = onDeleteProvider,
            onDismiss = { showProviderManager = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiProviderManagerDialog(
    userId: Long,
    providers: List<AiProviderEntity>,
    onSaveProvider: (AiProviderEntity) -> Unit,
    onDeleteProvider: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var editingId by remember { mutableStateOf<Long?>(null) }
    var kind by remember { mutableStateOf(AiProviderKind.GEMINI) }
    var name by remember { mutableStateOf("Google Gemini") }
    var baseUrl by remember { mutableStateOf("https://generativelanguage.googleapis.com/v1beta") }
    var apiKey by remember { mutableStateOf("") }
    var activeModel by remember { mutableStateOf("gemini-2.5-flash") }
    var modelInput by remember { mutableStateOf("") }
    val addedModels = remember { mutableStateListOf("gemini-2.5-flash", "gemini-2.5-pro") }
    var supportsVision by remember { mutableStateOf(true) }

    val presetSuggestions = remember(kind) {
        when (kind) {
            AiProviderKind.GEMINI -> listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash", "gemini-1.5-pro")
            AiProviderKind.XKIRO -> listOf("openai/gpt-5.6-sol", "anthropic/claude-sonnet-4", "google/gemini-2.5-pro", "deepseek/deepseek-r1")
            AiProviderKind.OPENAI_COMPATIBLE -> listOf("gpt-4o", "gpt-4o-mini", "o3-mini", "deepseek-chat", "deepseek-reasoner")
        }
    }

    fun addModelToList(raw: String) {
        val parts = raw.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }
        parts.forEach { m ->
            if (!addedModels.contains(m)) addedModels.add(m)
        }
        if (activeModel.isBlank() && addedModels.isNotEmpty()) {
            activeModel = addedModels.first()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.manageProviders) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (providers.isNotEmpty()) {
                    providers.forEach { p ->
                        val count = p.allModelsList().size
                        LedgerCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${p.providerKind.name} • ${p.modelName} ($count models)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editingId = p.id
                                        kind = p.providerKind
                                        name = p.name
                                        baseUrl = p.baseUrl
                                        apiKey = p.apiKey
                                        activeModel = p.modelName
                                        supportsVision = p.supportsVision
                                        addedModels.clear()
                                        addedModels.addAll(p.allModelsList())
                                    },
                                    modifier = Modifier.testTag("edit_provider_${p.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Provider")
                                }
                                IconButton(onClick = { onDeleteProvider(p.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                Text(
                    if (editingId != null) "Edit Provider & Models" else strings.addAiProvider,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = kind == AiProviderKind.GEMINI,
                        onClick = {
                            kind = AiProviderKind.GEMINI
                            if (editingId == null) {
                                name = "Gemini"
                                baseUrl = "https://generativelanguage.googleapis.com/v1beta"
                                addedModels.clear()
                                addedModels.addAll(listOf("gemini-2.5-flash", "gemini-2.5-pro"))
                                activeModel = "gemini-2.5-flash"
                                supportsVision = true
                            }
                        },
                        label = { Text("Gemini API") }
                    )
                    FilterChip(
                        selected = kind == AiProviderKind.XKIRO,
                        onClick = {
                            kind = AiProviderKind.XKIRO
                            if (editingId == null) {
                                name = "xKiro AI"
                                baseUrl = "https://api.xkiro.com/v1"
                                addedModels.clear()
                                addedModels.addAll(listOf("openai/gpt-5.6-sol", "anthropic/claude-sonnet-4"))
                                activeModel = "openai/gpt-5.6-sol"
                                supportsVision = true
                            }
                        },
                        label = { Text("xKiro Connect") }
                    )
                    FilterChip(
                        selected = kind == AiProviderKind.OPENAI_COMPATIBLE,
                        onClick = {
                            kind = AiProviderKind.OPENAI_COMPATIBLE
                            if (editingId == null) {
                                name = "Custom OpenAI"
                                baseUrl = "https://api.openai.com/v1"
                                addedModels.clear()
                                addedModels.addAll(listOf("gpt-4o-mini", "gpt-4o"))
                                activeModel = "gpt-4o-mini"
                                supportsVision = true
                            }
                        },
                        label = { Text("Custom URL") }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.providerNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ai_provider_name_input")
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text(strings.baseUrlLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ai_provider_url_input")
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(strings.apiKeyLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ai_provider_key_input")
                )

                Text("Models for this Provider (tap to set default)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    addedModels.forEach { mId ->
                        FilterChip(
                            selected = mId == activeModel,
                            onClick = { activeModel = mId },
                            label = { Text(mId, style = MaterialTheme.typography.labelSmall) },
                            trailingIcon = if (addedModels.size > 1) {
                                {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove model",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                addedModels.remove(mId)
                                                if (activeModel == mId) {
                                                    activeModel = addedModels.firstOrNull().orEmpty()
                                                }
                                            }
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("provider_model_chip_$mId")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        label = { Text("Add Model ID (or comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("ai_provider_model_input")
                    )
                    Button(
                        onClick = {
                            if (modelInput.isNotBlank()) {
                                addModelToList(modelInput)
                                activeModel = modelInput.split(',').last().trim().ifBlank { activeModel }
                                modelInput = ""
                            }
                        },
                        modifier = Modifier.testTag("add_provider_model_btn")
                    ) { Text("+ Add") }
                }

                Text("Quick Model Presets", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presetSuggestions.filterNot { addedModels.contains(it) }.forEach { preset ->
                        OutlinedButton(
                            onClick = {
                                addModelToList(preset)
                                if (activeModel.isBlank()) activeModel = preset
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+ $preset", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = supportsVision, onCheckedChange = { supportsVision = it })
                    Text(strings.supportsVisionLabel, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (modelInput.isNotBlank()) {
                        addModelToList(modelInput)
                    }
                    val finalModels = addedModels.distinct()
                    val finalPrimary = activeModel.ifBlank { finalModels.firstOrNull().orEmpty() }
                    if (name.isNotBlank() && apiKey.isNotBlank() && finalPrimary.isNotBlank()) {
                        onSaveProvider(
                            AiProviderEntity(
                                id = editingId ?: 0L,
                                ownerUserId = userId,
                                name = name.trim(),
                                providerKind = kind,
                                baseUrl = baseUrl.trim(),
                                apiKey = apiKey.trim(),
                                modelName = finalPrimary,
                                availableModelsCsv = (listOf(finalPrimary) + finalModels).distinct().joinToString(","),
                                supportsVision = supportsVision,
                                isDefault = true
                            )
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_ai_provider_btn")
            ) { Text(strings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
