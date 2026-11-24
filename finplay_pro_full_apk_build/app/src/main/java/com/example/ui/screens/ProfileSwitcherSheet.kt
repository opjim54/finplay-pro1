package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.data.local.FinanceProfileEntity
import com.example.data.local.TransactionEntity
import com.example.domain.CurrencyUtil
import com.example.i18n.LocalStrings

fun profileIconForKey(key: String): ImageVector = when (key) {
    "farm" -> Icons.Default.Agriculture
    "family" -> Icons.Default.FamilyRestroom
    "business" -> Icons.Default.BusinessCenter
    "shop" -> Icons.Default.Storefront
    "work" -> Icons.Default.Work
    else -> Icons.Default.Person
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileSwitcherSheet(
    profiles: List<FinanceProfileEntity>,
    activeProfileId: Long,
    allUserAccounts: List<AccountEntity>,
    allUserTransactions: List<TransactionEntity>,
    currencyCode: String,
    onSelectProfile: (Long) -> Unit,
    onCreateProfile: (name: String, iconKey: String, colorHex: Long, desc: String) -> Unit,
    onUpdateProfile: (FinanceProfileEntity) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingProfile by remember { mutableStateOf<FinanceProfileEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val balanceByProfile = remember(allUserAccounts) {
        allUserAccounts.groupBy { it.profileId }.mapValues { (_, list) -> list.sumOf { it.balance } }
    }
    val txCountByProfile = remember(allUserTransactions) {
        allUserTransactions.groupBy { it.profileId }.mapValues { (_, list) -> list.size }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.switchProfile,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.separateFinanceHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        editingProfile = null
                        showCreateDialog = true
                    },
                    modifier = Modifier.testTag("create_profile_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.createProfile)
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(340.dp)
            ) {
                items(profiles, key = { it.id }) { profile ->
                    val isSelected = profile.id == activeProfileId
                    val netBal = balanceByProfile[profile.id] ?: 0.0
                    val txCount = txCountByProfile[profile.id] ?: 0
                    val accent = Color(profile.colorHex)

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                onSelectProfile(profile.id)
                                onDismiss()
                            }
                            .testTag("profile_item_${profile.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = profileIconForKey(profile.iconKey),
                                    contentDescription = profile.name,
                                    tint = accent
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = strings.activeProfileLabel,
                                            tint = accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${CurrencyUtil.format(netBal, currencyCode)} • $txCount ${strings.transactions}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (profile.description.isNotBlank()) {
                                    Text(
                                        text = profile.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = {
                                editingProfile = profile
                                showCreateDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = strings.editProfile)
                            }
                            if (profiles.size > 1) {
                                IconButton(onClick = { onDeleteProfile(profile.id) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = strings.deleteProfile,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showCreateDialog) {
        var name by remember(editingProfile) { mutableStateOf(editingProfile?.name ?: "") }
        var desc by remember(editingProfile) { mutableStateOf(editingProfile?.description ?: "") }
        var iconKey by remember(editingProfile) { mutableStateOf(editingProfile?.iconKey ?: "farm") }
        var colorHex by remember(editingProfile) { mutableLongStateOf(editingProfile?.colorHex ?: 0xFF047857L) }

        val presets = listOf(
            "farm" to "Farm",
            "family" to "Family",
            "business" to "Business",
            "shop" to "Shop",
            "work" to "Project",
            "person" to "Personal"
        )
        val palette = listOf(0xFF047857L, 0xFF0284C7L, 0xFFD97706L, 0xFF7C3AEDL, 0xFFDB2777L, 0xFF0F766EL)

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(if (editingProfile == null) strings.createProfile else strings.editProfile) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(strings.profileName) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(strings.profileDesc) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.forEach { (key, label) ->
                            FilterChip(
                                selected = iconKey == key,
                                onClick = {
                                    iconKey = key
                                    if (name.isBlank() && editingProfile == null) name = label
                                },
                                leadingIcon = { Icon(profileIconForKey(key), contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        palette.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(if (colorHex == hex) 34.dp else 28.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .clickable { colorHex = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val existing = editingProfile
                            if (existing == null) {
                                onCreateProfile(name.trim(), iconKey, colorHex, desc.trim())
                            } else {
                                onUpdateProfile(
                                    existing.copy(
                                        name = name.trim(),
                                        iconKey = iconKey,
                                        colorHex = colorHex,
                                        description = desc.trim()
                                    )
                                )
                            }
                            showCreateDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_profile_btn")
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}
