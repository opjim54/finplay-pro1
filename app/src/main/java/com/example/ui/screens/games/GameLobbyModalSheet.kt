package com.example.ui.screens.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TournamentTierConfigEntity
import com.example.ui.theme.LocalFinanceColors

enum class GameLaunchMode {
    OFFLINE,
    BOT,
    FRIENDS_HOST,
    FRIENDS_JOIN,
    ONLINE_MULTIPLAYER,
    TOURNAMENT
}

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
    val financeColors = LocalFinanceColors.current
    var botAccuracy by remember { mutableIntStateOf(75) }
    var joinRoomCodeInput by remember { mutableStateOf("") }
    var showJoinRoomField by remember { mutableStateOf(false) }

    val bannerRes = gStrings.bannerResFor(game)
    val gameTitle = gStrings.gameTitle(game)
    val gameSubtitle = gStrings.gameSubtitle(game)
    val gameBadge = gStrings.gameBadge(game)

    // Filter tiers specifically for this game
    val gameTiers = remember(tournamentTiers, game) {
        tournamentTiers.filter { it.gameId.equals(game.name, ignoreCase = true) && it.isEnabled }
            .sortedBy { it.entryFee }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // 1. Cinematic 3D Banner Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Image(
                    painter = painterResource(id = bannerRes),
                    contentDescription = "$gameTitle Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )

                // Dark Bottom Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xFF0F172A).copy(alpha = 0.95f), Color(0xFF0F172A))
                            )
                        )
                )

                // Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                // Badge & Game Title on Banner
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFD97706)
                    ) {
                        Text(
                            text = gameBadge,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = gameTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = gameSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0),
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Mode Selection Header
            Text(
                text = "Select Game Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 18.dp)
            )

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ---- MODE 1: OFFLINE PASS & PLAY ----
                ModeCard(
                    icon = Icons.Default.PhoneAndroid,
                    title = if (gStrings.isBn) "অফলাইন পাস অ্যান্ড প্লে" else "Offline Pass & Play",
                    description = if (gStrings.isBn) "কোনো ইন্টারনেট ছাড়া একই ডিভাইসে ফ্রেন্ডদের সাথে খেলুন" else "Play locally on this device without requiring internet",
                    accentColor = Color(0xFF10B981),
                    buttonText = if (gStrings.isBn) "অফলাইন শুরু করুন" else "Play Offline",
                    onClick = {
                        onLaunchGame(GameLaunchMode.OFFLINE, botAccuracy, 0.0, 0.0, null)
                    }
                )

                // ---- MODE 2: PLAY WITH AI BOT ----
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, financeColors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0284C7).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color(0xFF0284C7))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (gStrings.isBn) "স্মার্ট এআই বট (AI Bot)" else "Play with AI Bot",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    if (gStrings.isBn) "কাস্টম ডিফিকাল্টি সিলেক্ট করে রোবটের সাথে প্র্যাকটিস করুন" else "Challenge responsive AI bots with adjustable difficulty",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Difficulty Level Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                50 to (if (gStrings.isBn) "সহজ (Easy)" else "Easy"),
                                75 to (if (gStrings.isBn) "মধ্যম (Medium)" else "Medium"),
                                95 to (if (gStrings.isBn) "কঠিন (Hard)" else "Hard")
                            ).forEach { (acc, label) ->
                                FilterChip(
                                    selected = botAccuracy == acc,
                                    onClick = { botAccuracy = acc },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0284C7),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = {
                                onLaunchGame(GameLaunchMode.BOT, botAccuracy, 0.0, 0.0, null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (gStrings.isBn) "বট ম্যাচ শুরু করুন" else "Start Bot Match", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ---- MODE 3: PLAY WITH FRIENDS (PRIVATE ROOM) ----
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, financeColors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF8B5CF6))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (gStrings.isBn) "বন্ধুদের সাথে খেলুন (Private Match)" else "Play with Friends",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    if (gStrings.isBn) "প্রাইভেট রুম কোড শেয়ার করে বন্ধুদের ইনভাইট করুন" else "Create a private room & invite your friends with a 6-digit code",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    val generatedCode = "${game.name.take(3)}-${(1000..9999).random()}"
                                    onLaunchGame(GameLaunchMode.FRIENDS_HOST, botAccuracy, 0.0, 0.0, generatedCode)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (gStrings.isBn) "রুম তৈরি করুন" else "Create Room", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showJoinRoomField = !showJoinRoomField },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (gStrings.isBn) "কোড দিয়ে জয়েন" else "Join Code")
                            }
                        }

                        AnimatedVisibility(visible = showJoinRoomField) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                                OutlinedTextField(
                                    value = joinRoomCodeInput,
                                    onValueChange = { joinRoomCodeInput = it.uppercase() },
                                    label = { Text("Enter 6-Digit Room Code") },
                                    placeholder = { Text("e.g. LUD-4821") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (joinRoomCodeInput.isNotBlank()) {
                                            onLaunchGame(GameLaunchMode.FRIENDS_JOIN, botAccuracy, 0.0, 0.0, joinRoomCodeInput)
                                        }
                                    },
                                    enabled = joinRoomCodeInput.length >= 4,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Enter Room Now", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ---- MODE 4: ONLINE MULTIPLAYER ----
                ModeCard(
                    icon = Icons.Default.Public,
                    title = if (gStrings.isBn) "অনলাইন মাল্টিপ্লেয়ার (Quick Match)" else "Online Multiplayer",
                    description = if (gStrings.isBn) "অনলাইনে উপস্থিত রিয়েল প্লেয়ারদের সাথে লাইভ ১v১ ম্যাচ" else "Instant 1v1 matchmaking with real online players",
                    accentColor = Color(0xFFF59E0B),
                    buttonText = if (gStrings.isBn) "অনলাইন ম্যাচ খুঁজুন" else "Find Online Match",
                    onClick = {
                        onLaunchGame(GameLaunchMode.ONLINE_MULTIPLAYER, botAccuracy, 0.0, 0.0, null)
                    }
                )

                // ---- MODE 5: TOURNAMENT & WIN PRIZES ----
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.5.dp, Color(0xFFD97706)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFD97706).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFD97706))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (gStrings.isBn) "অফিশিয়াল টুর্নামেন্ট (Win Real Prizes)" else "Official Tournaments",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFFD97706)
                                )
                                Text(
                                    if (gStrings.isBn) "এডমিন-কনফিগার করা টুর্নামেন্ট স্লটে জয়েন করে ক্যাশ প্রাইজ জিতুন" else "Join admin-configured tournament slots & win real wallet rewards",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (gameTiers.isEmpty()) {
                            Text(
                                "No active tournament tiers for this game yet. Check back soon!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            gameTiers.forEach { tier ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${tier.tierLabel} Tier",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Entry: ৳${tier.entryFee.toInt()} ➔ Win: ৳${tier.winPrize.toInt()}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            onLaunchGame(GameLaunchMode.TOURNAMENT, botAccuracy, tier.entryFee, tier.winPrize, null)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (tier.entryFee == 0.0) Color(0xFF10B981) else Color(0xFFD97706)
                                        ),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(
                                            text = if (tier.entryFee == 0.0) "Play Free" else "Join ৳${tier.entryFee.toInt()}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    buttonText: String,
    onClick: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, financeColors.cardBorder),
        modifier = Modifier.fillMaxWidth()
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
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.height(36.dp)
            ) {
                Text(buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
