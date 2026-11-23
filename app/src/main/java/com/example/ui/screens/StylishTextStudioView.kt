package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.StylishTextEngine
import com.example.ui.theme.LocalFinanceColors

private enum class StudioSubTab(val label: String) {
    FONTS("Fonts (28+)"),
    SYMBOLS("Symbols (10 Sets)"),
    FRAMES("Line Borders"),
    ART("Kaomoji & Art")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StylishTextStudioView(
    onCopy: (String) -> Unit,
    onSaveToNote: (String) -> Unit,
    onNotify: (String) -> Unit
) {
    val financeColors = LocalFinanceColors.current
    var text by remember { mutableStateOf("Smart Finance Life") }
    var composedBar by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(StudioSubTab.FONTS) }
    var selectedFontCat by remember { mutableStateOf("All") }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Enter text to transform or decorate") },
        minLines = 2,
        maxLines = 4,
        trailingIcon = {
            if (text.isNotEmpty()) {
                IconButton(onClick = { text = "" }) {
                    Icon(Icons.Default.Backspace, contentDescription = "Clear")
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stylish_text_input")
    )

    if (composedBar.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, financeColors.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Symbol & Text Tray",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        composedBar,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = { onCopy(composedBar) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy tray")
                }
                IconButton(onClick = {
                    onSaveToNote(composedBar)
                }) {
                    Icon(Icons.Default.NoteAdd, contentDescription = "Save tray to Note")
                }
                IconButton(onClick = { composedBar = "" }) {
                    Icon(Icons.Default.Backspace, contentDescription = "Clear tray")
                }
            }
        }
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(StudioSubTab.entries) { tab ->
            FilterChip(
                selected = activeTab == tab,
                onClick = { activeTab = tab },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("studio_tab_${tab.name.lowercase()}")
            )
        }
    }

    val previewInput = text.ifBlank { "Smart Finance Life" }

    when (activeTab) {
        StudioSubTab.FONTS -> {
            val cats = remember { listOf("All") + StylishTextEngine.fontPresets.map { it.category }.distinct() }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(cats) { cat ->
                    FilterChip(
                        selected = selectedFontCat == cat,
                        onClick = { selectedFontCat = cat },
                        label = { Text(cat) }
                    )
                }
            }
            val visibleFonts = remember(selectedFontCat) {
                if (selectedFontCat == "All") StylishTextEngine.fontPresets
                else StylishTextEngine.fontPresets.filter { it.category == selectedFontCat }
            }
            visibleFonts.forEach { preset ->
                val styled = remember(previewInput, preset.id) { preset.transform(previewInput) }
                StylishResultCard(
                    title = preset.name,
                    badge = preset.category,
                    styledOutput = styled,
                    onCopy = { onCopy(styled) },
                    onSaveToNote = { onSaveToNote(styled) },
                    onAppendTray = {
                        composedBar += styled
                        onNotify("Added to tray")
                    }
                )
            }
        }

        StudioSubTab.SYMBOLS -> {
            StylishTextEngine.symbolGroups.forEach { group ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, financeColors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            group.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            group.symbols.forEach { sym ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            composedBar += sym
                                            onCopy(sym)
                                        }
                                ) {
                                    Text(
                                        text = sym,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        StudioSubTab.FRAMES -> {
            StylishTextEngine.lineTemplates.forEach { tpl ->
                val wrapped = remember(previewInput, tpl.id) { tpl.wrap(previewInput) }
                StylishResultCard(
                    title = tpl.name,
                    badge = "Line Art",
                    styledOutput = wrapped,
                    onCopy = { onCopy(wrapped) },
                    onSaveToNote = { onSaveToNote(wrapped) },
                    onAppendTray = {
                        composedBar = if (composedBar.isEmpty()) wrapped else "$composedBar\n$wrapped"
                        onNotify("Added frame to tray")
                    }
                )
            }
        }

        StudioSubTab.ART -> {
            StylishTextEngine.kaomojiAndArt.forEach { (title, art) ->
                StylishResultCard(
                    title = title,
                    badge = "Art",
                    styledOutput = art,
                    onCopy = { onCopy(art) },
                    onSaveToNote = { onSaveToNote(art) },
                    onAppendTray = {
                        composedBar += art
                        onNotify("Added to tray")
                    }
                )
            }
        }
    }
}

@Composable
private fun StylishResultCard(
    title: String,
    badge: String,
    styledOutput: String,
    onCopy: () -> Unit,
    onSaveToNote: () -> Unit,
    onAppendTray: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, financeColors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = financeColors.champagneGold.copy(alpha = 0.16f)
                ) {
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = financeColors.champagneGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = styledOutput,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCopy)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy")
                }
                OutlinedButton(onClick = onAppendTray) {
                    Text("+ Tray")
                }
                IconButton(onClick = onSaveToNote) {
                    Icon(Icons.Default.NoteAdd, contentDescription = "Save to Note")
                }
            }
        }
    }
}
