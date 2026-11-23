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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.games.MultiplayerRoomDoc
import com.example.data.games.TournamentSettlementReceipt
import com.example.data.local.GameMatchResult
import com.example.domain.games.AetherCrownEngine
import com.example.domain.games.AetherPlayer
import com.example.domain.games.AetherTurnPhase
import com.example.domain.games.BoardSkinPalette
import com.example.domain.games.ChessEngine
import com.example.domain.games.ChessGameState
import com.example.domain.games.ChessOutcome
import com.example.domain.games.ConnectFourDisc
import com.example.domain.games.ConnectFourEngine
import com.example.domain.games.ConnectFourOutcome
import com.example.domain.games.ConnectFourState
import com.example.domain.games.GameSoundEffect
import com.example.domain.games.GameSoundEngine
import com.example.domain.games.LudoColor
import com.example.domain.games.LudoEngine
import com.example.domain.games.LudoPhase
import com.example.domain.games.OnlineMatchSerializer
import com.example.domain.games.PieceColor
import com.example.domain.games.ReversiDisc
import com.example.domain.games.ReversiEngine
import com.example.domain.games.ReversiOutcome
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.OnlineAuthProfile
import kotlin.random.Random

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
    val financeColors = LocalFinanceColors.current
    var selectedGame by remember { mutableStateOf(ArenaGameId.CHESS) }
    var joinCode by remember { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.5f)),
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
                    Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF0284C7))
                    Text(
                        text = gStrings.onlineFriendsTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (onlineAuth != null) {
                    TextButton(onClick = onSignOutOnline) {
                        Text(if (gStrings.isBn) "সাইন আউট" else "Sign out", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (onlineAuth == null) {
                Text(
                    text = gStrings.onlineSignInPrompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onSignInGoogle,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("online_google_signin_btn")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(gStrings.signInWithGoogleLabel, fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(avatarGlyph, fontSize = 18.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(onlineAuth.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(onlineAuth.email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(shape = RoundedCornerShape(50), color = Color(0xFF10B981).copy(alpha = 0.2f)) {
                            Text(
                                text = if (gStrings.isBn) "অনলাইন" else "ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
                        FilterChip(
                            selected = selectedGame == game,
                            onClick = { selectedGame = game },
                            label = { Text(gStrings.gameTitle(game), style = MaterialTheme.typography.labelSmall) },
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
                        modifier = Modifier
                            .weight(1f)
                            .testTag("online_create_room_btn")
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(gStrings.createPrivateRoomLabel, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = { onQuickMatch(selectedGame) },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("online_quick_match_btn")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(gStrings.quickMatchLabel, maxLines = 1, style = MaterialTheme.typography.labelMedium)
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
                        modifier = Modifier
                            .weight(1f)
                            .testTag("online_join_code_input")
                    )
                    OutlinedButton(
                        onClick = {
                            if (joinCode.length == 6) onJoinByCode(joinCode)
                        },
                        enabled = !isBusy && joinCode.length == 6,
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("online_join_room_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(gStrings.joinByCodeLabel)
                    }
                }

                if (selectedEntryFee > 0.0) {
                    Text(
                        text = if (gStrings.isBn) "টুর্নামেন্ট এন্ট্রি: ৳${selectedEntryFee.toInt()} • হারলে ১০০% গেম ওয়ালেটে ক্যাশব্যাক"
                        else "Tournament Stake: ৳${selectedEntryFee.toInt()} • 100% Loss Cashback to Game Wallet",
                        style = MaterialTheme.typography.labelSmall,
                        color = financeColors.champagneGold,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val activeOrWaiting = recentRooms.filter { it.status == "WAITING" || it.status == "PLAYING" }
                if (activeOrWaiting.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (gStrings.isBn) "চলমান অনলাইন রুম" else "Active Online Rooms",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        activeOrWaiting.take(3).forEach { rm ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = rm.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (rm.status == "PLAYING") Color(0xFF059669) else Color(0xFFD97706),
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

        // Settlement Cashback / Prize Banner
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
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, skinPalette.accentBorder.copy(alpha = 0.5f)),
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
                                .background(skinPalette.frameEnd.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = skinPalette.accentBorder, modifier = Modifier.size(32.dp))
                        }
                        Text(gStrings.waitingForFriendLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = room.roomCode,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 4.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("Room Code", room.roomCode))
                                Toast.makeText(context, "Copied: ${room.roomCode}", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copy")
                            }
                            Button(onClick = {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Join my FinPlay Pro ${room.gameType} match! Room Code: ${room.roomCode}"
                                    )
                                }
                                runCatching { context.startActivity(Intent.createChooser(share, gStrings.shareCodeLabel)) }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(gStrings.shareCodeLabel)
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
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(resultText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Button(onClick = onLeaveOrForfeit) {
                                Text(if (gStrings.isBn) "এরিনায় ফিরে যান" else "Back to Arena")
                            }
                        }
                    }
                }

                // Live Interactive Synced Board per Game Type
                when (room.gameType) {
                    "CONNECT_FOUR" -> OnlineConnectFourBoard(
                        room = room,
                        isHost = isHost,
                        isMyTurn = isMyTurn,
                        localUid = localUid,
                        opponentUid = opponentUid,
                        skinPalette = skinPalette,
                        onSubmitMove = onSubmitMove
                    )
                    "REVERSI" -> OnlineReversiBoard(
                        room = room,
                        isHost = isHost,
                        isMyTurn = isMyTurn,
                        localUid = localUid,
                        opponentUid = opponentUid,
                        skinPalette = skinPalette,
                        onSubmitMove = onSubmitMove
                    )
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
                    "AETHER_CROWN" -> OnlineAetherControlBoard(
                        room = room,
                        isHost = isHost,
                        isMyTurn = isMyTurn,
                        localUid = localUid,
                        opponentUid = opponentUid,
                        skinPalette = skinPalette,
                        gStrings = gStrings,
                        onSubmitMove = onSubmitMove
                    )
                }
            }

            else -> {
                Text(
                    text = if (gStrings.isBn) "রুম বাতিল করা হয়েছে।" else "Room was cancelled.",
                    style = MaterialTheme.typography.bodyMedium
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
    val isCashback = receipt.outcome == GameMatchResult.LOSS
    val isWin = receipt.outcome == GameMatchResult.WIN
    val bg = when {
        isWin -> listOf(Color(0xFF065F46), Color(0xFF059669))
        isCashback -> listOf(Color(0xFF312E81), Color(0xFF6D28D9))
        else -> listOf(Color(0xFF334155), Color(0xFF475569))
    }
    val title = when {
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
                text = "+৳${receipt.creditedAmount.toInt()} → ${receipt.creditedWalletBucket} WALLET",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFDE68A)
            )
        }
    }
}

// ---- Synced Interactive Online Boards ----

@Composable
private fun OnlineConnectFourBoard(
    room: MultiplayerRoomDoc,
    isHost: Boolean,
    isMyTurn: Boolean,
    localUid: String,
    opponentUid: String,
    skinPalette: BoardSkinPalette,
    onSubmitMove: (String, String, String, Int, String, String, Boolean) -> Unit
) {
    val state = remember(room.statePayload) {
        OnlineMatchSerializer.decodeConnectFour(room.statePayload) ?: ConnectFourEngine.newGame()
    }
    val legalCols = remember(state) { ConnectFourEngine.legalColumns(state).toSet() }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = skinPalette.feltSurface,
        border = BorderStroke(1.5.dp, skinPalette.accentBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until ConnectFourState.COLS).forEach { col ->
                    val enabled = isMyTurn && col in legalCols
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (enabled) skinPalette.accentBorder else Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clickable(enabled = enabled) {
                                GameSoundEngine.play(GameSoundEffect.MOVE)
                                val next = ConnectFourEngine.dropDisc(state, col)
                                val finished = next.outcome != ConnectFourOutcome.ONGOING
                                val winner = when (next.outcome) {
                                    ConnectFourOutcome.SOLAR_WINS -> room.hostUid
                                    ConnectFourOutcome.CRIMSON_WINS -> room.guestUid
                                    ConnectFourOutcome.DRAW -> "DRAW"
                                    ConnectFourOutcome.ONGOING -> ""
                                }
                                onSubmitMove(
                                    room.roomCode,
                                    OnlineMatchSerializer.encodeConnectFour(next),
                                    if (finished) localUid else opponentUid,
                                    room.moveCount + 1,
                                    "Dropped in Col ${col + 1}",
                                    winner,
                                    finished
                                )
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("↓${col + 1}", fontWeight = FontWeight.Bold, color = if (enabled) Color.Black else Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
            for (r in 0 until ConnectFourState.ROWS) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in 0 until ConnectFourState.COLS) {
                        val disc = state.board[r * ConnectFourState.COLS + c]
                        val fill = when (disc) {
                            ConnectFourDisc.SOLAR -> Color(0xFFF59E0B)
                            ConnectFourDisc.CRIMSON -> Color(0xFFE11D48)
                            ConnectFourDisc.EMPTY -> Color.Black.copy(alpha = 0.35f)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(fill)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineReversiBoard(
    room: MultiplayerRoomDoc,
    isHost: Boolean,
    isMyTurn: Boolean,
    localUid: String,
    opponentUid: String,
    skinPalette: BoardSkinPalette,
    onSubmitMove: (String, String, String, Int, String, String, Boolean) -> Unit
) {
    val state = remember(room.statePayload) {
        OnlineMatchSerializer.decodeReversi(room.statePayload) ?: ReversiEngine.newGame()
    }
    val legalByIndex = remember(state) { ReversiEngine.legalMoves(state).associateBy { it.index } }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = skinPalette.feltSurface,
        border = BorderStroke(1.5.dp, skinPalette.accentBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Emerald: ${state.emeraldScore}", color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                Text("Pearl: ${state.pearlScore}", color = Color(0xFFFEF3C7), fontWeight = FontWeight.Bold)
            }
            for (r in 0 until 8) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (c in 0 until 8) {
                        val idx = r * 8 + c
                        val disc = state.board[idx]
                        val move = legalByIndex[idx]
                        val canTap = isMyTurn && move != null
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if ((r + c) % 2 == 0) skinPalette.boardDark else skinPalette.feltSurface)
                                .clickable(enabled = canTap) {
                                    GameSoundEngine.play(GameSoundEffect.MOVE)
                                    val next = ReversiEngine.applyMove(state, move!!.index)
                                    val finished = next.outcome != ReversiOutcome.ONGOING
                                    val nextTurnUid = when {
                                        finished -> localUid
                                        next.turn == ReversiDisc.EMERALD -> room.hostUid
                                        else -> room.guestUid
                                    }
                                    val winner = when (next.outcome) {
                                        ReversiOutcome.EMERALD_WINS -> room.hostUid
                                        ReversiOutcome.PEARL_WINS -> room.guestUid
                                        ReversiOutcome.DRAW -> "DRAW"
                                        ReversiOutcome.ONGOING -> ""
                                    }
                                    onSubmitMove(
                                        room.roomCode,
                                        OnlineMatchSerializer.encodeReversi(next),
                                        nextTurnUid,
                                        room.moveCount + 1,
                                        "Flipped ${move.flippedIndices.size} discs",
                                        winner,
                                        finished
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (disc) {
                                ReversiDisc.EMERALD -> Box(Modifier.fillMaxSize(0.76f).clip(CircleShape).background(Color(0xFF10B981)))
                                ReversiDisc.PEARL -> Box(Modifier.fillMaxSize(0.76f).clip(CircleShape).background(Color(0xFFFEF3C7)))
                                ReversiDisc.EMPTY -> if (canTap) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(skinPalette.accentBorder))
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

            // Progress track overview for both players
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

@Composable
private fun OnlineAetherControlBoard(
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
        OnlineMatchSerializer.decodeAether(room.statePayload) ?: AetherCrownEngine.newGame(playVsBot = false)
    }
    val actions = remember(state) { AetherCrownEngine.legalActions(state) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = skinPalette.feltSurface,
        border = BorderStroke(1.5.dp, skinPalette.accentBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Solar: ⚡${state.charges[AetherPlayer.SOLAR] ?: 0}",
                    color = Color(0xFF34D399),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${gStrings.roundLabel} ${state.roundCount}",
                    color = Color(0xFFFDE68A),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Lunar: ⚡${state.charges[AetherPlayer.LUNAR] ?: 0}",
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold
                )
            }
            Text(state.statusLog, color = Color.White, style = MaterialTheme.typography.bodySmall)

            if (isMyTurn && state.phase == AetherTurnPhase.ROLL_DICE) {
                Button(
                    onClick = {
                        GameSoundEngine.play(GameSoundEffect.DICE_TICK)
                        val d1 = Random.nextInt(1, 5)
                        val d2 = Random.nextInt(1, 5)
                        val next = AetherCrownEngine.rollDice(state, d1, d2)
                        val nextTurnUid = if (next.turn == AetherPlayer.SOLAR) room.hostUid else room.guestUid
                        onSubmitMove(
                            room.roomCode,
                            OnlineMatchSerializer.encodeAether(next),
                            nextTurnUid,
                            room.moveCount + 1,
                            "Rolled [$d1, $d2]",
                            "",
                            false
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✦ ${gStrings.rollTwinDice}", fontWeight = FontWeight.Bold)
                }
            } else if (isMyTurn && state.phase == AetherTurnPhase.ALLOCATE_DICE) {
                actions.take(8).forEach { act ->
                    OutlinedButton(
                        onClick = {
                            GameSoundEngine.play(GameSoundEffect.MOVE)
                            val next = AetherCrownEngine.applyAction(state, act)
                            val finished = next.phase == AetherTurnPhase.GAME_OVER
                            val nextTurnUid = when {
                                finished -> localUid
                                next.turn == AetherPlayer.SOLAR -> room.hostUid
                                else -> room.guestUid
                            }
                            val winner = when (next.winner) {
                                AetherPlayer.SOLAR -> room.hostUid
                                AetherPlayer.LUNAR -> room.guestUid
                                null -> ""
                            }
                            onSubmitMove(
                                room.roomCode,
                                OnlineMatchSerializer.encodeAether(next),
                                nextTurnUid,
                                room.moveCount + 1,
                                act.description,
                                winner,
                                finished
                            )
                        },
                        border = BorderStroke(1.dp, skinPalette.accentBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(act.description, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
