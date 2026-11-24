package com.example.ui.screens.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TournamentTierConfigEntity
import com.example.domain.games.GameSoundEffect
import com.example.domain.games.GameSoundEngine

enum class GameLaunchMode {
    OFFLINE,
    BOT,
    FRIENDS_HOST,
    FRIENDS_JOIN,
    ONLINE_MULTIPLAYER,
    TOURNAMENT
}

// Cohesive Luxury 2-Palette System:
// Palette 1: Obsidian Midnight + Royal Champagne Gold (VIP Play & Earn / Arena Header)
// Palette 2: Deep Royal Emerald + Luminous Mint (All Games / Active Action Accents)
private val LobbyObsidianBg = Color(0xFF0A101D)
private val LobbyObsidianCard = Color(0xFF111B2E)
private val LobbyObsidianElevated = Color(0xFF17243D)
private val LobbyGoldPrimary = Color(0xFFF5C451)
private val LobbyGoldDeep = Color(0xFFD49B27)
private val LobbyEmeraldDeep = Color(0xFF042F2E)
private val LobbyEmeraldRich = Color(0xFF065F46)
private val LobbyMintBright = Color(0xFF10B981)
private val LobbyMintSoft = Color(0xFF6EE7B7)
private val LobbyTextPrimary = Color(0xFFF8FAFC)
private val LobbyTextSecondary = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GameLobbyModalSheet(
    game: ArenaGameId,
    gStrings: GameUiStrings,
    tournamentTiers: List<TournamentTierConfigEntity>,
    userWalletBalance: Double,
    onDismiss: () -> Unit,
    onLaunchGame: (mode: GameLaunchMode, botAccuracy: Int, tierEntryFee: Double, tierWinPrize: Double, roomCode: String?) -> Unit
) {
    var showEntryFeeSlots by remember(game) { mutableStateOf(false) }
    var botAccuracy by remember { mutableIntStateOf(75) }
    var joinRoomCodeInput by remember { mutableStateOf("") }
    var showJoinRoomField by remember { mutableStateOf(false) }

    val bannerRes = gStrings.bannerResFor(game)
    val iconRes = gStrings.iconResFor(game)
    val gameTitle = gStrings.gameTitle(game)
    val gameSubtitle = gStrings.gameSubtitle(game)
    val gameBadge = gStrings.gameBadge(game)

    // Filter configured tiers for this game, falling back to canonical Play & Earn slots if not yet seeded
    val gameTiers = remember(tournamentTiers, game) {
        val configured = tournamentTiers
            .filter { it.gameId.equals(game.name, ignoreCase = true) && it.isEnabled }
            .sortedBy { it.entryFee }
        if (configured.isNotEmpty()) {
            configured
        } else {
            listOf(
                TournamentTierConfigEntity(gameId = game.name, tierLabel = "Practice", entryFee = 0.0, winPrize = 0.0, isEnabled = true, sortOrder = 0),
                TournamentTierConfigEntity(gameId = game.name, tierLabel = "Bronze", entryFee = 20.0, winPrize = 36.0, isEnabled = true, sortOrder = 1),
                TournamentTierConfigEntity(gameId = game.name, tierLabel = "Silver", entryFee = 50.0, winPrize = 90.0, isEnabled = true, sortOrder = 2),
                TournamentTierConfigEntity(gameId = game.name, tierLabel = "Gold", entryFee = 100.0, winPrize = 180.0, isEnabled = true, sortOrder = 3),
                TournamentTierConfigEntity(gameId = game.name, tierLabel = "Diamond", entryFee = 250.0, winPrize = 450.0, isEnabled = true, sortOrder = 4)
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LobbyObsidianBg,
        contentColor = LobbyTextPrimary,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // 1. Cinematic 3D Banner Header + Realistic 3D Game Icon Medallion
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Image(
                    painter = painterResource(id = bannerRes),
                    contentDescription = "$gameTitle Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Deep Obsidian Cinematic Vignette
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to LobbyObsidianBg.copy(alpha = 0.20f),
                                    0.48f to LobbyObsidianBg.copy(alpha = 0.58f),
                                    0.85f to LobbyObsidianBg.copy(alpha = 0.96f),
                                    1.0f to LobbyObsidianBg
                                )
                            )
                        )
                )

                // Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(40.dp)
                        .background(LobbyObsidianBg.copy(alpha = 0.72f), CircleShape)
                        .border(1.dp, LobbyGoldPrimary.copy(alpha = 0.45f), CircleShape)
                        .testTag("lobby_close_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = LobbyTextPrimary)
                }

                // Wallet Balance Pill (Top-Left)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = LobbyObsidianBg.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, LobbyGoldPrimary.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = LobbyGoldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "৳${userWalletBalance.toInt()}",
                            color = LobbyGoldPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Realistic 3D Icon + Title & Badge at Bottom
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Photorealistic 3D Game Icon Squircle
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .shadow(12.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(listOf(LobbyGoldPrimary, LobbyGoldDeep, LobbyMintBright)),
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = "$gameTitle 3D Icon",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LobbyGoldPrimary.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, LobbyGoldPrimary.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = gameBadge,
                                color = LobbyGoldPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = gameTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = LobbyTextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = gameSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = LobbyTextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // =================================================================
                // MODE 1 (FIRST & PRIMARY): PLAY & EARN -> OPENS ENTRY FEE SLOTS
                // =================================================================
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent,
                    border = BorderStroke(
                        width = if (showEntryFeeSlots) 2.dp else 1.5.dp,
                        brush = Brush.linearGradient(listOf(LobbyGoldPrimary, LobbyMintBright, LobbyGoldDeep))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF19150A), LobbyObsidianElevated, LobbyEmeraldDeep)
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Clickable Play & Earn Hero Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    GameSoundEngine.play(GameSoundEffect.TAP)
                                    showEntryFeeSlots = !showEntryFeeSlots
                                }
                                .testTag("lobby_play_and_earn_header"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(listOf(LobbyGoldPrimary, LobbyGoldDeep))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = "Play & Earn",
                                    tint = LobbyObsidianBg,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (gStrings.isBn) "প্লে অ্যান্ড আর্ন (Play & Earn)" else "Play & Earn",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = LobbyGoldPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = LobbyMintBright.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, LobbyMintBright.copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = "VIP",
                                            color = LobbyMintSoft,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (gStrings.isBn) "এন্ট্রি ফি স্লট বেছে নিন • জিতলে ইনস্ট্যান্ট উইনিং প্রাইজ ও ১০০% ক্যাশব্যাক"
                                    else "Tap to open Entry Fee Slots • Instant Cash Prizes & 100% Cashback",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                            Icon(
                                imageVector = if (showEntryFeeSlots) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showEntryFeeSlots) "Hide slots" else "Show entry fee slots",
                                tint = LobbyGoldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Primary CTA Button when Entry Fee Slots are collapsed
                        if (!showEntryFeeSlots) {
                            Button(
                                onClick = {
                                    GameSoundEngine.play(GameSoundEffect.TAP)
                                    showEntryFeeSlots = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LobbyGoldPrimary,
                                    contentColor = LobbyObsidianBg
                                ),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("lobby_play_and_earn_btn")
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (gStrings.isBn) "Play & Earn • এন্ট্রি ফি স্লট দেখুন" else "Play & Earn • Choose Entry Fee Slot",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Expandable Entry Fee Slots Panel (Revealed on Play & Earn click)
                        AnimatedVisibility(
                            visible = showEntryFeeSlots,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Cashback & Wallet Guarantee Strip
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = LobbyEmeraldRich.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, LobbyMintBright.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = LobbyMintSoft,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (gStrings.isBn) "হারলেও ১০০% এন্ট্রি ফি গেম ওয়ালেটে ক্যাশব্যাক! স্লট সিলেক্ট করুন:"
                                            else "100% Entry Fee Cashback on Loss! Select your Entry Fee Slot:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LobbyMintSoft
                                        )
                                    }
                                }

                                gameTiers.forEach { tier ->
                                    val isFree = tier.entryFee <= 0.0
                                    val canAfford = isFree || userWalletBalance >= tier.entryFee
                                    val tierAccent = when {
                                        isFree -> LobbyMintBright
                                        tier.entryFee >= 200.0 -> Color(0xFF38BDF8)
                                        tier.entryFee >= 100.0 -> LobbyGoldPrimary
                                        tier.entryFee >= 50.0 -> Color(0xFFE2E8F0)
                                        else -> Color(0xFFF59E0B)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = LobbyObsidianBg.copy(alpha = 0.78f),
                                        border = BorderStroke(
                                            1.dp,
                                            tierAccent.copy(alpha = if (canAfford) 0.55f else 0.25f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("play_earn_slot_${tier.entryFee.toInt()}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "${tier.tierLabel} Slot",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 14.sp,
                                                        color = tierAccent
                                                    )
                                                    if (!isFree) {
                                                        Surface(
                                                            shape = RoundedCornerShape(50),
                                                            color = LobbyGoldPrimary.copy(alpha = 0.16f)
                                                        ) {
                                                            Text(
                                                                text = "WIN ৳${tier.winPrize.toInt()}",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = LobbyGoldPrimary,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = if (isFree) {
                                                        if (gStrings.isBn) "ফ্রি প্র্যাকটিস • কোন এন্ট্রি ফি নেই" else "Free Practice • Zero Entry Fee"
                                                    } else {
                                                        if (gStrings.isBn) "এন্ট্রি: ৳${tier.entryFee.toInt()}  ➔  জিতলে: ৳${tier.winPrize.toInt()}"
                                                        else "Entry: ৳${tier.entryFee.toInt()}  ➔  Win Prize: ৳${tier.winPrize.toInt()}"
                                                    },
                                                    fontSize = 12.sp,
                                                    color = if (isFree) LobbyMintSoft else Color(0xFFCBD5E1),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Spacer(Modifier.width(8.dp))

                                            Button(
                                                onClick = {
                                                    GameSoundEngine.play(GameSoundEffect.TAP)
                                                    onLaunchGame(
                                                        GameLaunchMode.TOURNAMENT,
                                                        botAccuracy,
                                                        tier.entryFee,
                                                        tier.winPrize,
                                                        null
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isFree) LobbyMintBright else LobbyGoldPrimary,
                                                    contentColor = LobbyObsidianBg
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                modifier = Modifier
                                                    .height(38.dp)
                                                    .testTag("play_earn_enter_${tier.entryFee.toInt()}")
                                            ) {
                                                Text(
                                                    text = if (isFree) {
                                                        if (gStrings.isBn) "ফ্রি খেলুন" else "Play Free"
                                                    } else {
                                                        if (gStrings.isBn) "খেলুন ৳${tier.entryFee.toInt()}" else "Enter ৳${tier.entryFee.toInt()}"
                                                    },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section Divider for Other Modes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (gStrings.isBn) "অন্যান্য গেম মোড" else "MORE GAME MODES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = LobbyMintSoft,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(LobbyMintBright.copy(alpha = 0.25f))
                    )
                }

                // ---- MODE 2: ONLINE MULTIPLAYER ----
                PremiumModeCard(
                    icon = Icons.Default.Public,
                    title = if (gStrings.isBn) "অনলাইন মাল্টিপ্লেয়ার (৩x কয়েন!)" else "Online Multiplayer (3x Coins!)",
                    description = if (gStrings.isBn) "অনলাইনে রিয়েল প্লেয়ারদের সাথে খেলে ৩ গুণ বেশি (+৭৫/+২০) কয়েন পান" else "Play real 1v1 online matches for 3x Game Coins (+75 Win / +20 Play)",
                    accentColor = LobbyGoldPrimary,
                    buttonText = if (gStrings.isBn) "অনলাইন খুঁজুন" else "Quick Match",
                    onClick = {
                        GameSoundEngine.play(GameSoundEffect.TAP)
                        onLaunchGame(GameLaunchMode.ONLINE_MULTIPLAYER, botAccuracy, 0.0, 0.0, null)
                    }
                )

                // ---- MODE 3: PLAY WITH AI BOT ----
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = LobbyObsidianCard,
                    border = BorderStroke(1.dp, LobbyMintBright.copy(alpha = 0.32f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LobbyMintBright.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = LobbyMintSoft)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (gStrings.isBn) "স্মার্ট এআই বট (AI Bot • +২৫ জয় কয়েন)" else "Play with AI Bot (+25 Win Coins)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = LobbyTextPrimary
                                )
                                Text(
                                    if (gStrings.isBn) "অফলাইনেও কয়েন পাবেন! কাস্টম ডিফিকাল্টি বেছে নিয়ে খেলুন" else "Earn Game Coins even offline! Challenge adaptive AI bots",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LobbyTextSecondary
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                50 to (if (gStrings.isBn) "সহজ" else "Easy"),
                                75 to (if (gStrings.isBn) "মধ্যম" else "Medium"),
                                95 to (if (gStrings.isBn) "গ্র্যান্ডমাস্টার" else "Hard")
                            ).forEach { (acc, label) ->
                                FilterChip(
                                    selected = botAccuracy == acc,
                                    onClick = {
                                        GameSoundEngine.play(GameSoundEffect.TAP)
                                        botAccuracy = acc
                                    },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = LobbyObsidianElevated,
                                        labelColor = LobbyTextSecondary,
                                        selectedContainerColor = LobbyMintBright,
                                        selectedLabelColor = LobbyObsidianBg
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = botAccuracy == acc,
                                        borderColor = LobbyMintBright.copy(alpha = 0.25f),
                                        selectedBorderColor = LobbyMintBright
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = {
                                GameSoundEngine.play(GameSoundEffect.TAP)
                                onLaunchGame(GameLaunchMode.BOT, botAccuracy, 0.0, 0.0, null)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LobbyMintBright,
                                contentColor = LobbyObsidianBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lobby_start_bot_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (gStrings.isBn) "বট ম্যাচ শুরু করুন" else "Start Bot Match",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // ---- MODE 4: PLAY WITH FRIENDS (PRIVATE ROOM) ----
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = LobbyObsidianCard,
                    border = BorderStroke(1.dp, LobbyGoldPrimary.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LobbyGoldPrimary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = LobbyGoldPrimary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (gStrings.isBn) "বন্ধুদের ইনভাইট করে খেলুন (৩x কয়েন!)" else "Play with Friends (3x Coins Bonus!)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = LobbyTextPrimary
                                )
                                Text(
                                    if (gStrings.isBn) "রুম কোড শেয়ার করে বন্ধুদের সাথে খেললে ৩ গুণ বেশি (+৭৫/+২০) কয়েন পাবেন" else "Invite friends with a Room Code to earn 3x Game Coins (+75 Win / +20 Play)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LobbyTextSecondary
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    GameSoundEngine.play(GameSoundEffect.TAP)
                                    val generatedCode = "${game.name.take(3)}-${(1000..9999).random()}"
                                    onLaunchGame(GameLaunchMode.FRIENDS_HOST, botAccuracy, 0.0, 0.0, generatedCode)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LobbyGoldPrimary,
                                    contentColor = LobbyObsidianBg
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (gStrings.isBn) "রুম তৈরি" else "Create Room", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    GameSoundEngine.play(GameSoundEffect.TAP)
                                    showJoinRoomField = !showJoinRoomField
                                },
                                border = BorderStroke(1.dp, LobbyGoldPrimary.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LobbyGoldPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (gStrings.isBn) "কোড জয়েন" else "Join Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        AnimatedVisibility(visible = showJoinRoomField) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                                OutlinedTextField(
                                    value = joinRoomCodeInput,
                                    onValueChange = { joinRoomCodeInput = it.uppercase() },
                                    label = { Text(if (gStrings.isBn) "৬-সংখ্যার রুম কোড" else "Enter 6-Digit Room Code") },
                                    placeholder = { Text("e.g. LUD-4821") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = LobbyTextPrimary,
                                        unfocusedTextColor = LobbyTextPrimary,
                                        focusedBorderColor = LobbyGoldPrimary,
                                        unfocusedBorderColor = LobbyTextSecondary.copy(alpha = 0.4f),
                                        focusedLabelColor = LobbyGoldPrimary,
                                        unfocusedLabelColor = LobbyTextSecondary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (joinRoomCodeInput.isNotBlank()) {
                                            GameSoundEngine.play(GameSoundEffect.TAP)
                                            onLaunchGame(GameLaunchMode.FRIENDS_JOIN, botAccuracy, 0.0, 0.0, joinRoomCodeInput.trim())
                                        }
                                    },
                                    enabled = joinRoomCodeInput.trim().length >= 4,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LobbyMintBright,
                                        contentColor = LobbyObsidianBg
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (gStrings.isBn) "রুমে প্রবেশ করুন" else "Enter Room Now", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }

                // ---- MODE 5: OFFLINE PASS & PLAY ----
                PremiumModeCard(
                    icon = Icons.Default.PhoneAndroid,
                    title = if (gStrings.isBn) "অফলাইন পাস অ্যান্ড প্লে (+২৫ জয় কয়েন)" else "Offline Pass & Play (+25 Win Coins)",
                    description = if (gStrings.isBn) "ইন্টারনেট ছাড়াই একই ফোনে খেলুন এবং গেম কয়েন জিতুন" else "Play locally offline and still earn +25/+8 Game Coins per match",
                    accentColor = LobbyMintBright,
                    buttonText = if (gStrings.isBn) "অফলাইন" else "Play Local",
                    onClick = {
                        GameSoundEngine.play(GameSoundEffect.TAP)
                        onLaunchGame(GameLaunchMode.OFFLINE, botAccuracy, 0.0, 0.0, null)
                    }
                )
            }
        }
    }
}

@Composable
private fun PremiumModeCard(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    buttonText: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = LobbyObsidianCard,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LobbyTextPrimary)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = LobbyTextSecondary,
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = LobbyObsidianBg
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(buttonText, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
