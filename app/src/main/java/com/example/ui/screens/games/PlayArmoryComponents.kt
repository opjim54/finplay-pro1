package com.example.ui.screens.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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

private val ArmoryObsidianBg = Color(0xFF090E1A)
private val ArmoryObsidianCard = Color(0xFF141F35)
private val ArmoryObsidianElevated = Color(0xFF1A2844)
private val ArmoryEmeraldDeep = Color(0xFF042F2E)
private val ArmoryGold = Color(0xFFF5C451)
private val ArmoryMint = Color(0xFF10B981)
private val ArmoryMintSoft = Color(0xFF6EE7B7)
private val ArmoryIvory = Color(0xFFF8FAFC)
private val ArmorySlate = Color(0xFF94A3B8)

@Composable
fun PlayArmoryTabContent(
    wallet: PlayWalletEntity,
    gStrings: GameUiStrings,
    onBuyItem: (String) -> Unit,
    onEquipItem: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ArmoryItemCategory.BOARD_SKIN) }
    val ownedSet = remember(wallet.ownedItemsCsv) { PlayArmoryCatalog.ownedItemSet(wallet) }
    val skillCharges = remember(wallet.skillChargesJson) { PlayArmoryCatalog.parseSkillCharges(wallet.skillChargesJson) }
    val filteredItems = remember(selectedCategory) {
        PlayArmoryCatalog.items.filter { it.category == selectedCategory }
    }

    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = ArmoryObsidianCard,
        labelColor = ArmorySlate,
        selectedContainerColor = ArmoryGold,
        selectedLabelColor = ArmoryObsidianBg
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ArmoryObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "armory_banner") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(
                    1.2.dp,
                    Brush.linearGradient(listOf(ArmoryGold.copy(alpha = 0.85f), ArmoryMint.copy(alpha = 0.65f)))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(Color(0xFF101829), ArmoryEmeraldDeep, ArmoryObsidianCard)))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "✦ ${gStrings.armoryShopTitle}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = ArmoryGold
                        )
                        Text(
                            text = gStrings.armoryShopSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = ArmoryMintSoft
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ArmoryObsidianBg.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, ArmoryGold.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = gStrings.gameWalletTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = ArmoryGold
                            )
                            Text(
                                text = "🪙 ${wallet.gameBalance.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = ArmoryIvory
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
                val tabs = listOf(
                    Triple(ArmoryItemCategory.BOARD_SKIN, gStrings.boardSkinsTab, "armory_cat_skins"),
                    Triple(ArmoryItemCategory.WAR_CHARACTER, if (gStrings.isBn) "ওয়ার ল্যান্ড ক্যারেক্টার" else "War Characters", "armory_cat_war_char"),
                    Triple(ArmoryItemCategory.WAR_WEAPON_SKIN, if (gStrings.isBn) "অস্ত্র স্কিন" else "Gun Skins", "armory_cat_war_gun"),
                    Triple(ArmoryItemCategory.EMOTE, if (gStrings.isBn) "ইমোটস" else "Emotes", "armory_cat_emotes"),
                    Triple(ArmoryItemCategory.CONSUMABLE_CARD, if (gStrings.isBn) "কার্ড ও স্পেশাল" else "Cards & Special", "armory_cat_cards"),
                    Triple(ArmoryItemCategory.AVATAR, gStrings.avatarsTab, "armory_cat_avatars"),
                    Triple(ArmoryItemCategory.SKILL, gStrings.skillsTab, "armory_cat_skills")
                )
                tabs.forEach { (cat, label, tag) ->
                    val selected = selectedCategory == cat
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = cat },
                        colors = chipColors,
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = ArmoryGold.copy(alpha = 0.30f),
                            selectedBorderColor = ArmoryGold
                        ),
                        label = {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                            )
                        },
                        modifier = Modifier.testTag(tag)
                    )
                }
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
    onBuy: () -> Unit,
    onEquip: () -> Unit
) {
    val accent = Color(item.previewAccentHex)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ArmoryObsidianCard,
        border = BorderStroke(
            width = if (isEquipped) 1.5.dp else 1.dp,
            color = if (isEquipped) ArmoryGold else ArmoryGold.copy(alpha = 0.24f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(ArmoryObsidianElevated)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.iconGlyph, fontSize = 24.sp, color = accent)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (gStrings.isBn) item.titleBn else item.titleEn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ArmoryIvory
                )
                Text(
                    text = if (gStrings.isBn) item.descBn else item.descEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = ArmorySlate
                )
                if (item.isConsumable) {
                    Text(
                        text = "${gStrings.chargesCountLabel}: $chargeCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ArmoryMintSoft
                    )
                } else if (isExpired) {
                    Text(
                        text = if (gStrings.isBn) "⚠️ মেয়াদ শেষ — কয়েন দিয়ে রিনিউ করুন" else "⚠️ Expired — Renew with Coins",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCA5A5)
                    )
                } else if (remainingDays != null && remainingDays < 900) {
                    Text(
                        text = if (gStrings.isBn) "⏳ আর $remainingDays দিন মেয়াদ বাকি" else "⏳ $remainingDays days validity left",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ArmoryGold
                    )
                } else if (item.validityDays != null && !isOwned) {
                    Text(
                        text = if (gStrings.isBn) "📅 মেয়াদ: ${item.validityDays} দিন" else "📅 Validity: ${item.validityDays} Days",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArmoryMintSoft
                    )
                }
            }
            when {
                isEquipped -> {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ArmoryMint.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, ArmoryMint.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ArmoryMintSoft, modifier = Modifier.size(16.dp))
                            Text(gStrings.equippedLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ArmoryMintSoft)
                        }
                    }
                }
                isOwned -> {
                    OutlinedButton(
                        onClick = onEquip,
                        border = BorderStroke(1.dp, ArmoryGold.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ArmoryGold),
                        modifier = Modifier.testTag("armory_equip_${item.id}")
                    ) {
                        Text(gStrings.equipLabel, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Button(
                        onClick = onBuy,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArmoryGold,
                            contentColor = ArmoryObsidianBg,
                            disabledContainerColor = ArmoryObsidianElevated,
                            disabledContentColor = ArmorySlate
                        ),
                        modifier = Modifier.testTag("armory_buy_${item.id}")
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("🪙${item.priceGameCoins.toInt()}", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
