package com.example.ui.screens.games

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.games.MultiplayerRoomDoc
import com.example.data.games.TournamentSettlementReceipt
import com.example.data.local.GameMatchResult
import com.example.domain.games.BoardSkinPalette
import com.example.domain.games.ChessEngine
import com.example.domain.games.ChessGameState
import com.example.domain.games.ChessOutcome
import com.example.domain.games.GameSoundEffect
import com.example.domain.games.GameSoundEngine
import com.example.domain.games.LudoColor
import com.example.domain.games.LudoEngine
import com.example.domain.games.LudoPhase
import com.example.domain.games.OnlineMatchSerializer
import com.example.domain.games.PieceColor
import com.example.domain.games.SholoGutiEngine
import com.example.domain.games.SholoGutiMove
import com.example.domain.games.SholoGutiOutcome
import com.example.domain.games.SholoGutiPlayer
import com.example.domain.games.SnakesLaddersEngine
import com.example.domain.games.SnakesPhase
import com.example.ui.viewmodel.OnlineAuthProfile
import kotlin.math.hypot
import kotlin.random.Random

private val OnlineObsidianCard = Color(0xFF141F35)
private val OnlineObsidianElevated = Color(0xFF1B2945)
private val OnlineEmeraldDeep = Color(0xFF042F2E)
private val OnlineChampagneGold = Color(0xFFF5C451)
private val OnlineMintGlow = Color(0xFF10B981)
private val OnlineMintSoft = Color(0xFF6EE7B7)
private val OnlineIvory = Color(0xFFF8FAFC)
private val OnlineSlate = Color(0xFF94A3B8)

@Composable
fun OnlineMultiplayerLobbyCard(
    onlineAuth: OnlineAuthProfile?,
    avatarGlyph: String,
    selectedEntryFee: Double,
    recentRooms: List<MultiplayerRoomDoc>,
    isBusy: Boolean,
    gStrings: GameUiStrings,
    onSignInGoogle: () -> Unit,
    onSignOutOnline: () -> Unit,
    onCreatePrivateRoom: (ArenaGameId) -> Unit,
    onQuickMatch: (ArenaGameId) -> Unit,
    onJoinByCode: (String) -> Unit,
    onReopenRoom: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGame by remember { mutableStateOf(ArenaGameId.LUDO) }
    var joinCode by remember { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = OnlineObsidianCard,
        border = BorderStroke(1.2.dp, OnlineChampagneGold.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = OnlineChampagneGold)
                    Column {
                        Text(
                            text = gStrings.onlineFriendsTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnlineIvory
                        )
                        Text(
                            text = if (gStrings.isBn) "৩x গেম কয়েন বোনাস • লাইভ ১v১ ও ফ্রেন্ড রুম" else "3x Game Coins Bonus • Live 1v1 & Friend Rooms",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnlineMintSoft
                        )
                    }
                }
                if (onlineAuth != null) {
                    TextButton(onClick = onSignOutOnline) {
                        Text(
                            if (gStrings.isBn) "সাইন আউট" else "Sign out",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnlineChampagneGold
                        )
                    }
                }
            }

            if (onlineAuth == null) {
                Text(
                    text = gStrings.onlineSignInPrompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnlineSlate
                )
                Button(
                    onClick = onSignInGoogle,
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OnlineChampagneGold,
                        contentColor = Color(0xFF090E1A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("online_google_signin_btn")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF090E1A))
                    } else {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(gStrings.signInWithGoogleLabel, fontWeight = FontWeight.ExtraBold)
                }
            } else {
                // Signed-In Dual Player Card Preview with Real Google Photo
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = OnlineEmeraldDeep.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, OnlineMintGlow.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(OnlineObsidianCard)
                                .border(1.5.dp, OnlineChampagneGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!onlineAuth.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = onlineAuth.photoUrl,
                                    contentDescription = onlineAuth.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(avatarGlyph, fontSize = 18.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                onlineAuth.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = OnlineIvory
                            )
                            Text(
                                onlineAuth.email,
                                style = MaterialTheme.typography.labelSmall,
                                color = OnlineSlate
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = OnlineMintGlow.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, OnlineMintGlow.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (gStrings.isBn) "● অনলাইন" else "● ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnlineMintSoft,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Game selector chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ArenaGameId.entries.forEach { game ->
                        val selected = selectedGame == game
                        FilterChip(
                            selected = selected,
                            onClick = { selectedGame = game },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = OnlineObsidianElevated,
                                labelColor = OnlineSlate,
                                selectedContainerColor = OnlineChampagneGold,
                                selectedLabelColor = Color(0xFF090E1A)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = OnlineChampagneGold.copy(alpha = 0.28f),
                                selectedBorderColor = OnlineChampagneGold
                            ),
                            label = {
                                Text(
                                    gStrings.gameTitle(game),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("online_game_chip_${game.name.lowercase()}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onCreatePrivateRoom(selectedGame) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OnlineChampagneGold,
                            contentColor = Color(0xFF090E1A)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("online_create_room_btn")
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(gStrings.createPrivateRoomLabel, maxLines = 1, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = { onQuickMatch(selectedGame) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OnlineMintGlow,
                            contentColor = Color(0xFF042F2E)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("online_quick_match_btn")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(gStrings.quickMatchLabel, maxLines = 1, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode = it.uppercase().take(6) },
                        label = { Text(gStrings.enterRoomCodeHint) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnlineIvory,
                            unfocusedTextColor = OnlineIvory,
                            focusedBorderColor = OnlineChampagneGold,
                            unfocusedBorderColor = OnlineSlate.copy(alpha = 0.45f),
                            focusedLabelColor = OnlineChampagneGold,
                            unfocusedLabelColor = OnlineSlate
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("online_join_code_input")
                    )
                    OutlinedButton(
                        onClick = {
                            if (joinCode.length == 6) onJoinByCode(joinCode)
                        },
                        enabled = !isBusy && joinCode.length == 6,
                        border = BorderStroke(1.dp, OnlineChampagneGold.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OnlineChampagneGold),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("online_join_room_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(gStrings.joinByCodeLabel, fontWeight = FontWeight.Bold)
                    }
                }

                if (selectedEntryFee > 0.0) {
                    Text(
                        text = if (gStrings.isBn) "টুর্নামেন্ট এন্ট্রি: ৳${selectedEntryFee.toInt()} • হারলে ১০০% গেম ওয়ালেটে ক্যাশব্যাক"
                        else "Tournament Stake: ৳${selectedEntryFee.toInt()} • 100% Loss Cashback to Game Wallet",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnlineChampagneGold,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val activeOrWaiting = recentRooms.filter { it.status == "WAITING" || it.status == "PLAYING" }
                if (activeOrWaiting.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (gStrings.isBn) "চলমান অনলাইন রুম" else "Active Online Rooms",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnlineIvory
                        )
                        activeOrWaiting.take(3).forEach { rm ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = OnlineObsidianElevated,
                                border = BorderStroke(1.dp, OnlineMintGlow.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onReopenRoom(rm.roomCode) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${rm.gameType} • #${rm.roomCode}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = OnlineIvory
                                    )
                                    Text(
                                        text = rm.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (rm.status == "PLAYING") OnlineMintSoft else OnlineChampagneGold,
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

@Composable
fun OnlineRoomLiveScreen(
    room: MultiplayerRoomDoc,
    localUid: String,
    skinPalette: BoardSkinPalette,
    settlementReceipt: TournamentSettlementReceipt?,
    gStrings: GameUiStrings,
    onSubmitMove: (
        roomCode: String,
        nextPayload: String,
        nextTurnUid: String,
        nextMoveCount: Int,
        summary: String,
        winnerUid: String,
        finished: Boolean
    ) -> Unit,
    onMatchFinished: (gameId: String, entryFee: Double, outcome: GameMatchResult) -> Unit,
    onLeaveOrForfeit: () -> Unit
) {
    val context = LocalContext.current
    val isHost = room.hostUid == localUid
    val isMyTurn = room.status == "PLAYING" && room.turnUid == localUid
    val opponentUid = room.memberUids.firstOrNull { it != localUid } ?: room.hostUid
    var settledOnce by remember(room.roomCode) { mutableStateOf(false) }

    LaunchedEffect(room.status, room.winnerUid) {
        if (room.status == "FINISHED" && !settledOnce) {
            settledOnce = true
            val outcome = when (room.winnerUid) {
                localUid -> GameMatchResult.WIN
                "DRAW", "" -> GameMatchResult.DRAW
                else -> GameMatchResult.LOSS
            }
            GameSoundEngine.play(if (outcome == GameMatchResult.WIN) GameSoundEffect.WIN else GameSoundEffect.MOVE)
            onMatchFinished(room.gameType, room.entryFee, outcome)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Match Header HUD
        Surface(
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.2.dp, skinPalette.accentBorder.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(skinPalette.frameStart, skinPalette.frameEnd)))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                        Text(
                            text = "${room.gameType} • #${room.roomCode}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onLeaveOrForfeit, modifier = Modifier.size(32.dp).testTag("online_leave_room_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "Leave room", tint = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("P1: ${room.hostName}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD1FAE5))
                    Text(
                        text = if (room.guestName.isNotBlank()) "P2: ${room.guestName}" else gStrings.waitingForFriendLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFDE68A)
                    )
                }

                if (room.status == "PLAYING") {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isMyTurn) Color(0xFF10B981) else Color.White.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = if (isMyTurn) "✦ ${gStrings.yourTurnBadge} ✦" else gStrings.opponentTurnBadge,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Settlement Cashback / Prize / Coin Reward Banner
        if (settlementReceipt != null && settlementReceipt.creditedAmount > 0.0) {
            TournamentSettlementBanner(receipt = settlementReceipt, gStrings = gStrings)
        }

        when (room.status) {
            "WAITING" -> {
                val pulseTransition = rememberInfiniteTransition(label = "waiting_pulse")
                val pulseScale by pulseTransition.animateFloat(
                    initialValue = 0.94f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                    label = "pulse_scale"
                )
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = OnlineObsidianCard,
                    border = BorderStroke(1.dp, OnlineChampagneGold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                                .clip(CircleShape)
                                .background(OnlineEmeraldDeep),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = OnlineChampagneGold, modifier = Modifier.size(32.dp))
                        }
                        Text(gStrings.waitingForFriendLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnlineIvory)
                        Text(
                            text = room.roomCode,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 4.sp
                            ),
                            color = OnlineChampagneGold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("Room Code", room.roomCode))
                                    Toast.makeText(context, "Copied: ${room.roomCode}", Toast.LENGTH_SHORT).show()
                                },
                                border = BorderStroke(1.dp, OnlineChampagneGold.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnlineChampagneGold)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copy")
                            }
                            Button(
                                onClick = {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Join my FinPlay Pro ${room.gameType} match for 3x Game Coins! Room Code: ${room.roomCode}"
                                        )
                                    }
                                    runCatching { context.startActivity(Intent.createChooser(share, gStrings.shareCodeLabel)) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OnlineMintGlow, contentColor = OnlineEmeraldDeep)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(gStrings.shareCodeLabel, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            "PLAYING", "FINISHED" -> {
                if (room.status == "FINISHED") {
                    val resultText = when (room.winnerUid) {
                        localUid -> if (gStrings.isBn) "🏆 অভিনন্দন! আপনি অনলাইন ম্যাচ জিতেছেন!" else "🏆 Victory! You won the Online Match!"
                        "DRAW", "" -> if (gStrings.isBn) "ম্যাচ ড্র হয়েছে!" else "Match ended in a Draw!"
                        else -> if (gStrings.isBn) "ম্যাচ শেষ — প্রতিপক্ষ জয়ী!" else "Match Finished — Opponent Won"
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = OnlineObsidianElevated,
                        border = BorderStroke(1.dp, OnlineChampagneGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(resultText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = OnlineChampagneGold)
                            Button(
                                onClick = onLeaveOrForfeit,
                                colors = ButtonDefaults.buttonColors(containerColor = OnlineChampagneGold, contentColor = Color(0xFF090E1A))
                            ) {
                                Text(if (gStrings.isBn) "এরিনায় ফিরে যান" else "Back to Arena", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Live Interactive Synced Board per Game Type (all 7 inbuilt games supported)
                when (room.gameType) {
                    "CHESS" -> OnlineChessBoard(
                        room = room,
                        isHost = isHost,
                        isMyTurn = isMyTurn,
                        localUid = localUid,
                        opponentUid = opponentUid,
                        skinPalette = skinPalette,
                        onSubmitMove = onSubmitMove
                    )
                    "LUDO" -> OnlineLudoControlBoard(
                        room = room,
                        isHost = isHost,
                        isMyTurn = isMyTurn,
                        localUid = localUid,
                        opponentUid = opponentUid,
                        skinPalette = skinPalette,
                        gStrings = gStrings,
                        onSubmitMove = onSubmitMove
                    )
                    "SHOLO_GUTI" -> OnlineSholoGutiBoard(
                        room = room,
                        isHost = isHost,
                        isMyTurn = isMyTurn,
                        localUid = localUid,
                        opponentUid = opponentUid,
                        skinPalette = skinPalette,
                        onSubmitMove = onSubmitMove
                    )
                    "SNAKES_LADDERS" -> OnlineSnakesLaddersBoard(
                        room = room,
                        isHost = isHost,
                        isMyTurn = isMyTurn,
                        localUid = localUid,
                        opponentUid = opponentUid,
                        skinPalette = skinPalette,
                        gStrings = gStrings,
                        onSubmitMove = onSubmitMove
                    )
                    else -> OnlineActionDuelBoard(
                        room = room,
                        localUid = localUid,
                        opponentUid = opponentUid,
                        gStrings = gStrings,
                        onSubmitMove = onSubmitMove
                    )
                }
            }

            else -> {
                Text(
                    text = if (gStrings.isBn) "রুম বাতিল করা হয়েছে।" else "Room was cancelled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnlineIvory
                )
                Button(onClick = onLeaveOrForfeit) {
                    Text(if (gStrings.isBn) "ফিরে যান" else "Return")
                }
            }
        }
    }
}

@Composable
fun TournamentSettlementBanner(
    receipt: TournamentSettlementReceipt,
    gStrings: GameUiStrings,
    modifier: Modifier = Modifier
) {
    val isFreeCoinBonus = receipt.entryFee <= 0.0
    val isCashback = !isFreeCoinBonus && receipt.outcome == GameMatchResult.LOSS
    val isWin = receipt.outcome == GameMatchResult.WIN
    val bg = when {
        isWin || isFreeCoinBonus -> listOf(Color(0xFF065F46), Color(0xFF059669))
        isCashback -> listOf(Color(0xFF141F35), Color(0xFF1E3A5F))
        else -> listOf(Color(0xFF334155), Color(0xFF475569))
    }
    val title = when {
        isFreeCoinBonus -> if (gStrings.isBn) "🪙 ম্যাচ রিওয়ার্ড কয়েন যোগ হয়েছে!" else "🪙 Match Game Coins Earned!"
        isWin -> gStrings.prizeWonTitle
        isCashback -> gStrings.cashbackReceivedTitle
        else -> gStrings.entryRefundedTitle
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
        modifier = modifier.fillMaxWidth().testTag("tournament_settlement_banner")
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(bg))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "+${receipt.creditedAmount.toInt()} → ${receipt.creditedWalletBucket} WALLET",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFDE68A)
            )
        }
    }
}

// ---- Synced Interactive Online Boards ----

@Composable
private fun OnlineSholoGutiBoard(
    room: MultiplayerRoomDoc,
    isHost: Boolean,
    isMyTurn: Boolean,
    localUid: String,
    opponentUid: String,
    skinPalette: BoardSkinPalette,
    onSubmitMove: (String, String, String, Int, String, String, Boolean) -> Unit
) {
    val state = remember(room.statePayload) {
        OnlineMatchSerializer.decodeSholoGuti(room.statePayload) ?: SholoGutiEngine.newGame()
    }
    val myColor = if (isHost) SholoGutiPlayer.RUBY else SholoGutiPlayer.GOLD
    var selectedNode by remember(room.moveCount) { mutableStateOf<Int?>(state.chainedNodeId) }
    val legalMoves = remember(state) { SholoGutiEngine.allLegalMoves(state) }
    val legalMovesFromSelected = remember(selectedNode, legalMoves) {
        if (selectedNode == null) emptyMap()
        else legalMoves.filter { it.from == selectedNode }.associateBy { it.to }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = skinPalette.feltSurface,
        border = BorderStroke(1.5.dp, skinPalette.accentBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ruby: ${state.rubyCount} (Cap ${state.rubyCaptured})", color = Color(0xFFFB7185), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Gold: ${state.goldCount} (Cap ${state.goldCaptured})", color = Color(0xFFF5C451), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text(state.statusLog, color = Color.White, style = MaterialTheme.typography.labelSmall)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .pointerInput(state, isMyTurn, selectedNode) {
                        detectTapGestures { offset ->
                            if (!isMyTurn || state.outcome != SholoGutiOutcome.ONGOING) return@detectTapGestures
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val tapped = SholoGutiEngine.NODES.minByOrNull { node ->
                                val nx = if (isHost) node.normX else (1f - node.normX)
                                val ny = if (isHost) node.normY else (1f - node.normY)
                                hypot(offset.x - nx * w, offset.y - ny * h)
                            } ?: return@detectTapGestures
                            val tx = (if (isHost) tapped.normX else 1f - tapped.normX) * w
                            val ty = (if (isHost) tapped.normY else 1f - tapped.normY) * h
                            if (hypot(offset.x - tx, offset.y - ty) > w * 0.12f) return@detectTapGestures

                            val move = legalMovesFromSelected[tapped.id]
                            if (move != null) {
                                GameSoundEngine.play(if (move.isCapture) GameSoundEffect.CAPTURE else GameSoundEffect.MOVE)
                                val next = SholoGutiEngine.applyMove(state, move)
                                val finished = next.outcome != SholoGutiOutcome.ONGOING
                                val nextTurnUid = when {
                                    finished -> localUid
                                    next.turn == SholoGutiPlayer.RUBY -> room.hostUid
                                    else -> room.guestUid
                                }
                                val winner = when (next.outcome) {
                                    SholoGutiOutcome.RUBY_WINS -> room.hostUid
                                    SholoGutiOutcome.GOLD_WINS -> room.guestUid
                                    SholoGutiOutcome.DRAW -> "DRAW"
                                    SholoGutiOutcome.ONGOING -> ""
                                }
                                selectedNode = next.chainedNodeId
                                onSubmitMove(
                                    room.roomCode,
                                    OnlineMatchSerializer.encodeSholoGuti(next),
                                    nextTurnUid,
                                    room.moveCount + 1,
                                    next.statusLog,
                                    winner,
                                    finished
                                )
                            } else if (state.chainedNodeId == null && state.board[tapped.id] == myColor && legalMoves.any { it.from == tapped.id }) {
                                selectedNode = tapped.id
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                fun pt(id: Int): Offset {
                    val n = SholoGutiEngine.NODES[id]
                    val x = (if (isHost) n.normX else 1f - n.normX) * w
                    val y = (if (isHost) n.normY else 1f - n.normY) * h
                    return Offset(x, y)
                }

                SholoGutiEngine.EDGES.forEach { (a, b) ->
                    drawLine(
                        color = skinPalette.accentBorder.copy(alpha = 0.5f),
                        start = pt(a),
                        end = pt(b),
                        strokeWidth = 3.5f,
                        cap = StrokeCap.Round
                    )
                }

                SholoGutiEngine.NODES.forEach { node ->
                    val center = pt(node.id)
                    val targetMove: SholoGutiMove? = legalMovesFromSelected[node.id]
                    drawCircle(color = skinPalette.boardDark, radius = 9f, center = center)
                    if (targetMove != null) {
                        drawCircle(
                            color = if (targetMove.isCapture) Color(0xFFEF4444) else Color(0xFF10B981),
                            radius = w * 0.042f,
                            center = center
                        )
                    }
                    val bead = state.board[node.id]
                    if (bead != null) {
                        val isSel = selectedNode == node.id
                        val fill = if (bead == SholoGutiPlayer.RUBY) Color(0xFFE11D48) else Color(0xFFF5C451)
                        if (isSel) {
                            drawCircle(color = Color.White, radius = w * 0.056f, center = center, style = Stroke(width = 4f))
                        }
                        drawCircle(color = fill, radius = w * 0.046f, center = center)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineSnakesLaddersBoard(
    room: MultiplayerRoomDoc,
    isHost: Boolean,
    isMyTurn: Boolean,
    localUid: String,
    opponentUid: String,
    skinPalette: BoardSkinPalette,
    gStrings: GameUiStrings,
    onSubmitMove: (String, String, String, Int, String, String, Boolean) -> Unit
) {
    val state = remember(room.statePayload) {
        OnlineMatchSerializer.decodeSnakes(
            room.statePayload,
            hostName = room.hostName,
            guestName = room.guestName.ifBlank { "Opponent" }
        ) ?: SnakesLaddersEngine.newGame(2, playVsBot = false)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = skinPalette.feltSurface,
        border = BorderStroke(1.5.dp, skinPalette.accentBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "🎲 Dice: ${state.lastDiceValue} • ${state.statusLog}",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            state.players.forEach { p ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.name, color = Color(p.color), fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = if (p.position == 0) "Start (0/100)" else "Tile ${p.position} / 100",
                            color = OnlineChampagneGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (isMyTurn && state.phase != SnakesPhase.GAME_OVER) {
                Button(
                    onClick = {
                        GameSoundEngine.play(GameSoundEffect.DICE_TICK)
                        val res = SnakesLaddersEngine.rollAndMove(state)
                        val next = res.nextState
                        val finished = next.phase == SnakesPhase.GAME_OVER
                        val nextTurnUid = when {
                            finished -> localUid
                            next.currentTurnIndex == 0 -> room.hostUid
                            else -> room.guestUid
                        }
                        val winnerUid = when (next.winner?.id) {
                            0 -> room.hostUid
                            1 -> room.guestUid
                            else -> ""
                        }
                        onSubmitMove(
                            room.roomCode,
                            OnlineMatchSerializer.encodeSnakes(next),
                            nextTurnUid,
                            room.moveCount + 1,
                            next.statusLog,
                            winnerUid,
                            finished
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OnlineChampagneGold, contentColor = Color(0xFF090E1A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🎲 ${gStrings.rollDice}", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun OnlineActionDuelBoard(
    room: MultiplayerRoomDoc,
    localUid: String,
    opponentUid: String,
    gStrings: GameUiStrings,
    onSubmitMove: (String, String, String, Int, String, String, Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = OnlineObsidianCard,
        border = BorderStroke(1.2.dp, OnlineChampagneGold.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚔️ ${room.gameType.replace("_", " ")} Online Challenge",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OnlineChampagneGold
            )
            Text(
                text = if (gStrings.isBn) "দুই প্লেয়ার লাইভ রুমে সংযুক্ত আছেন! ম্যাচ শেষে রেজাল্ট সাবমিট করলে ৩x গেম কয়েন ও পয়েন্ট যোগ হবে।"
                else "Both players are connected! Submit your duel outcome below once your match concludes for 3x Game Coins.",
                style = MaterialTheme.typography.bodySmall,
                color = OnlineSlate,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        onSubmitMove(room.roomCode, room.statePayload, localUid, room.moveCount + 1, "Victory Claimed", localUid, true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OnlineMintGlow, contentColor = OnlineEmeraldDeep),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (gStrings.isBn) "জয় সাবমিট" else "Claim Win", fontWeight = FontWeight.ExtraBold)
                }
                OutlinedButton(
                    onClick = {
                        onSubmitMove(room.roomCode, room.statePayload, opponentUid, room.moveCount + 1, "Draw", "DRAW", true)
                    },
                    border = BorderStroke(1.dp, OnlineChampagneGold.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OnlineChampagneGold),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (gStrings.isBn) "ড্র" else "Draw", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OnlineChessBoard(
    room: MultiplayerRoomDoc,
    isHost: Boolean,
    isMyTurn: Boolean,
    localUid: String,
    opponentUid: String,
    skinPalette: BoardSkinPalette,
    onSubmitMove: (String, String, String, Int, String, String, Boolean) -> Unit
) {
    val state = remember(room.statePayload) {
        OnlineMatchSerializer.decodeChess(room.statePayload) ?: ChessGameState()
    }
    var selectedSquare by remember(room.moveCount) { mutableStateOf<Int?>(null) }
    val myColor = if (isHost) PieceColor.WHITE else PieceColor.BLACK
    val legalMoves = remember(state) { ChessEngine.legalMoves(state) }
    val targetsForSelected = remember(selectedSquare, legalMoves) {
        if (selectedSquare == null) emptyMap()
        else legalMoves.filter { it.from == selectedSquare }.associateBy { it.to }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, skinPalette.accentBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val rows = if (isHost) (0..7) else (7 downTo 0)
            val cols = if (isHost) (0..7) else (7 downTo 0)
            for (r in rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (c in cols) {
                        val sq = r * 8 + c
                        val piece = state.board[sq]
                        val isLight = (r + c) % 2 == 0
                        val isSelected = selectedSquare == sq
                        val targetMove = targetsForSelected[sq]
                        val bg = when {
                            isSelected -> skinPalette.accentBorder.copy(alpha = 0.7f)
                            targetMove != null -> Color(0xFF10B981).copy(alpha = 0.55f)
                            isLight -> skinPalette.boardLight
                            else -> skinPalette.boardDark
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(bg)
                                .clickable(enabled = isMyTurn && state.outcome == ChessOutcome.ONGOING) {
                                    if (targetMove != null) {
                                        val finalMove = if (targetMove.promotion != null) {
                                            targetMove.copy(promotion = com.example.domain.games.PieceType.QUEEN)
                                        } else targetMove
                                        GameSoundEngine.play(GameSoundEffect.MOVE)
                                        val next = ChessEngine.applyMove(state, finalMove)
                                        val finished = next.outcome != ChessOutcome.ONGOING
                                        val winner = when (next.outcome) {
                                            ChessOutcome.WHITE_WINS -> room.hostUid
                                            ChessOutcome.BLACK_WINS -> room.guestUid
                                            ChessOutcome.STALEMATE,
                                            ChessOutcome.DRAW_FIFTY_MOVE,
                                            ChessOutcome.DRAW_INSUFFICIENT_MATERIAL -> "DRAW"
                                            ChessOutcome.ONGOING -> ""
                                        }
                                        selectedSquare = null
                                        onSubmitMove(
                                            room.roomCode,
                                            OnlineMatchSerializer.encodeChess(next),
                                            if (finished) localUid else opponentUid,
                                            room.moveCount + 1,
                                            "Moved ${piece?.symbol() ?: "piece"}",
                                            winner,
                                            finished
                                        )
                                    } else if (piece != null && piece.color == myColor) {
                                        selectedSquare = sq
                                    } else {
                                        selectedSquare = null
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (piece != null) {
                                Text(
                                    text = piece.symbol(),
                                    fontSize = 24.sp,
                                    color = if (piece.color == PieceColor.WHITE) Color(0xFF111827) else Color(0xFF000000),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineLudoControlBoard(
    room: MultiplayerRoomDoc,
    isHost: Boolean,
    isMyTurn: Boolean,
    localUid: String,
    opponentUid: String,
    skinPalette: BoardSkinPalette,
    gStrings: GameUiStrings,
    onSubmitMove: (String, String, String, Int, String, String, Boolean) -> Unit
) {
    val state = remember(room.statePayload) {
        OnlineMatchSerializer.decodeLudo(room.statePayload) ?: LudoEngine.newGame(2, playVsBot = false)
    }
    val myColor = if (isHost) LudoColor.RED else LudoColor.YELLOW
    val movable = remember(state) { LudoEngine.legalTokenIndices(state) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = skinPalette.feltSurface,
        border = BorderStroke(1.5.dp, skinPalette.accentBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "🎲 Dice: ${state.diceValue} • ${state.statusLog}",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            listOf(LudoColor.RED to room.hostName, LudoColor.YELLOW to room.guestName).forEach { (col, name) ->
                val list = state.tokens[col].orEmpty()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name.ifBlank { col.displayName }, color = Color(col.colorHex), fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            list.forEach { tok ->
                                val canMove = isMyTurn && state.phase == LudoPhase.WAIT_MOVE && col == myColor && tok.index in movable
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (canMove) skinPalette.accentBorder else Color(col.colorHex).copy(alpha = 0.35f),
                                    modifier = Modifier.clickable(enabled = canMove) {
                                        GameSoundEngine.play(GameSoundEffect.MOVE)
                                        val next = LudoEngine.applyMove(state, tok.index)
                                        val finished = next.phase == LudoPhase.GAME_OVER
                                        val nextTurnUid = when {
                                            finished -> localUid
                                            next.currentTurn == LudoColor.RED -> room.hostUid
                                            else -> room.guestUid
                                        }
                                        val winner = when (next.winner) {
                                            LudoColor.RED -> room.hostUid
                                            LudoColor.YELLOW -> room.guestUid
                                            else -> ""
                                        }
                                        onSubmitMove(
                                            room.roomCode,
                                            OnlineMatchSerializer.encodeLudo(next),
                                            nextTurnUid,
                                            room.moveCount + 1,
                                            next.statusLog,
                                            winner,
                                            finished
                                        )
                                    }
                                ) {
                                    Text(
                                        text = when {
                                            tok.progress < 0 -> "Y"
                                            tok.progress >= 56 -> "👑"
                                            else -> "${tok.progress}"
                                        },
                                        color = if (canMove) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isMyTurn && state.phase == LudoPhase.WAIT_ROLL) {
                Button(
                    onClick = {
                        GameSoundEngine.play(GameSoundEffect.DICE_TICK)
                        val rolled = Random.nextInt(1, 7)
                        val next = LudoEngine.applyRoll(state, rolled)
                        val nextTurnUid = if (next.currentTurn == LudoColor.RED) room.hostUid else room.guestUid
                        onSubmitMove(
                            room.roomCode,
                            OnlineMatchSerializer.encodeLudo(next),
                            nextTurnUid,
                            room.moveCount + 1,
                            "Rolled $rolled",
                            "",
                            false
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🎲 ${gStrings.rollDice}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
