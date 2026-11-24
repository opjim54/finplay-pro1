package com.example.ui.screens.games

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlayWalletEntity
import com.example.domain.games.ArmoryItem
import com.example.domain.games.ArmoryItemCategory
import com.example.domain.games.PlayArmoryCatalog
import com.example.ui.theme.LocalFinanceColors

@Composable
fun PlayArmoryTabContent(
    wallet: PlayWalletEntity,
    gStrings: GameUiStrings,
    onBuyItem: (String) -> Unit,
    onEquipItem: (String) -> Unit
) {
    val financeColors = LocalFinanceColors.current
    var selectedCategory by remember { mutableStateOf(ArmoryItemCategory.BOARD_SKIN) }
    val ownedSet = remember(wallet.ownedItemsCsv) { PlayArmoryCatalog.ownedItemSet(wallet) }
    val skillCharges = remember(wallet.skillChargesJson) { PlayArmoryCatalog.parseSkillCharges(wallet.skillChargesJson) }
    val filteredItems = remember(selectedCategory) {
        PlayArmoryCatalog.items.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "armory_banner") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF4C1D95))))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = gStrings.armoryShopTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = gStrings.armoryShopSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFDDD6FE)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = gStrings.gameWalletTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDE68A)
                            )
                            Text(
                                text = "৳${wallet.gameBalance.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        item(key = "armory_category_filter") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == ArmoryItemCategory.BOARD_SKIN,
                    onClick = { selectedCategory = ArmoryItemCategory.BOARD_SKIN },
                    label = { Text(gStrings.boardSkinsTab) },
                    modifier = Modifier.testTag("armory_cat_skins")
                )
                FilterChip(
                    selected = selectedCategory == ArmoryItemCategory.WAR_CHARACTER,
                    onClick = { selectedCategory = ArmoryItemCategory.WAR_CHARACTER },
                    label = { Text(if (gStrings.isBn) "ওয়ার ল্যান্ড ক্যারেক্টার" else "War Characters") }
                )
                FilterChip(
                    selected = selectedCategory == ArmoryItemCategory.WAR_WEAPON_SKIN,
                    onClick = { selectedCategory = ArmoryItemCategory.WAR_WEAPON_SKIN },
                    label = { Text(if (gStrings.isBn) "অস্ত্র স্কিন" else "Gun Skins") }
                )
                FilterChip(
                    selected = selectedCategory == ArmoryItemCategory.EMOTE,
                    onClick = { selectedCategory = ArmoryItemCategory.EMOTE },
                    label = { Text(if (gStrings.isBn) "ইমোটস" else "Emotes") }
                )
                FilterChip(
                    selected = selectedCategory == ArmoryItemCategory.CONSUMABLE_CARD,
                    onClick = { selectedCategory = ArmoryItemCategory.CONSUMABLE_CARD },
                    label = { Text(if (gStrings.isBn) "কার্ড ও স্পেশাল" else "Cards & Special") }
                )
                FilterChip(
                    selected = selectedCategory == ArmoryItemCategory.AVATAR,
                    onClick = { selectedCategory = ArmoryItemCategory.AVATAR },
                    label = { Text(gStrings.avatarsTab) },
                    modifier = Modifier.testTag("armory_cat_avatars")
                )
                FilterChip(
                    selected = selectedCategory == ArmoryItemCategory.SKILL,
                    onClick = { selectedCategory = ArmoryItemCategory.SKILL },
                    label = { Text(gStrings.skillsTab) },
                    modifier = Modifier.testTag("armory_cat_skills")
                )
            }
        }

        items(filteredItems, key = { it.id }) { item ->
            val remainingDays = remember(wallet.ownedItemsCsv, item.id) {
                PlayArmoryCatalog.getRemainingDays(wallet, item.id)
            }
            val isExpired = remember(wallet.ownedItemsCsv, item.id) {
                PlayArmoryCatalog.isItemExpired(wallet, item.id)
            }
            ArmoryItemCard(
                item = item,
                isOwned = !item.isConsumable && item.id in ownedSet && !isExpired,
                isExpired = isExpired,
                remainingDays = remainingDays,
                isEquipped = (item.category == ArmoryItemCategory.BOARD_SKIN && wallet.equippedBoardSkinId == item.id) ||
                    (item.category == ArmoryItemCategory.AVATAR && wallet.equippedAvatarId == item.id),
                chargeCount = skillCharges[item.id] ?: 0,
                canAfford = wallet.gameBalance >= item.priceGameCoins,
                gStrings = gStrings,
                cardBorder = financeColors.cardBorder,
                onBuy = { onBuyItem(item.id) },
                onEquip = { onEquipItem(item.id) }
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ArmoryItemCard(
    item: ArmoryItem,
    isOwned: Boolean,
    isExpired: Boolean = false,
    remainingDays: Int? = null,
    isEquipped: Boolean,
    chargeCount: Int,
    canAfford: Boolean,
    gStrings: GameUiStrings,
    cardBorder: Color,
    onBuy: () -> Unit,
    onEquip: () -> Unit
) {
    val accent = Color(item.previewAccentHex)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (isEquipped) 1.5.dp else 1.dp, if (isEquipped) accent else cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.iconGlyph, fontSize = 24.sp, color = accent)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (gStrings.isBn) item.titleBn else item.titleEn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (gStrings.isBn) item.descBn else item.descEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.isConsumable) {
                    Text(
                        text = "${gStrings.chargesCountLabel}: $chargeCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                } else if (isExpired) {
                    Text(
                        text = if (gStrings.isBn) "⚠️ মেয়াদ শেষ — কয়েন দিয়ে রিনিউ করুন" else "⚠️ Expired — Renew with Coins",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                } else if (remainingDays != null && remainingDays < 900) {
                    Text(
                        text = if (gStrings.isBn) "⏳ আর $remainingDays দিন মেয়াদ বাকি" else "⏳ $remainingDays days validity left",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                } else if (item.validityDays != null && !isOwned) {
                    Text(
                        text = if (gStrings.isBn) "📅 মেয়াদ: ${item.validityDays} দিন" else "📅 Validity: ${item.validityDays} Days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            when {
                isEquipped -> {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                            Text(gStrings.equippedLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accent)
                        }
                    }
                }
                isOwned -> {
                    OutlinedButton(
                        onClick = onEquip,
                        modifier = Modifier.testTag("armory_equip_${item.id}")
                    ) {
                        Text(gStrings.equipLabel)
                    }
                }
                else -> {
                    Button(
                        onClick = onBuy,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        modifier = Modifier.testTag("armory_buy_${item.id}")
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("৳${item.priceGameCoins.toInt()}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
