package com.example.ui.screens.games

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.games.CloudTournamentDoc
import com.example.data.games.CloudTournamentEntryDoc
import com.example.data.games.CloudWalletLedgerDoc
import com.example.data.games.CloudWalletSummaryDoc
import com.example.data.games.FirestoreMultiplayerRepository
import com.example.data.games.MultiplayerRoomDoc
import com.example.data.games.TournamentHubItem
import com.example.data.games.TournamentSettlementReceipt
import com.example.data.local.BroadcastNotificationEntity
import com.example.data.local.ExternalTournamentEntity
import com.example.data.local.ExternalTournamentEntryEntity
import com.example.data.local.GameBotConfigEntity
import com.example.data.local.GameLeaderboardEntity
import com.example.data.local.GameMatchResult
import com.example.data.local.PlayWalletEntity
import com.example.data.local.PlayWalletLedgerEntity
import com.example.data.local.PlayWalletRequestEntity
import com.example.data.local.TournamentTierConfigEntity
import com.example.data.local.WalletPaymentChannel
import com.example.data.local.winRatePercent
import com.example.domain.games.BoardSkinPalette
import com.example.domain.games.GameSoundEffect
import com.example.domain.games.GameSoundEngine
import com.example.domain.games.PlayArmoryCatalog
import com.example.domain.games.PlayPaymentConfig
import com.example.i18n.LanguageManager
import com.example.i18n.LocalStrings
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.MatchmakingScanState
import com.example.ui.viewmodel.OnlineAuthProfile
import kotlin.math.roundToInt

private data class ArenaGameVisuals(
    val icon: ImageVector,
    val gradient: List<Color>
)

private fun visualsFor(game: ArenaGameId): ArenaGameVisuals = when (game) {
    ArenaGameId.LUDO -> ArenaGameVisuals(Icons.Default.Casino, listOf(Color(0xFF065F46), Color(0xFF059669)))
    ArenaGameId.CHESS -> ArenaGameVisuals(Icons.Default.GridOn, listOf(Color(0xFF1E293B), Color(0xFF0F766E)))
    ArenaGameId.AETHER_CROWN -> ArenaGameVisuals(Icons.Default.AutoAwesome, listOf(Color(0xFF312E81), Color(0xFF6D28D9)))
    ArenaGameId.REVERSI -> ArenaGameVisuals(Icons.Default.Apps, listOf(Color(0xFF047857), Color(0xFF115E59)))
    ArenaGameId.CONNECT_FOUR -> ArenaGameVisuals(Icons.Default.ViewColumn, listOf(Color(0xFF9F1239), Color(0xFFD97706)))
    ArenaGameId.WAR_LAND -> ArenaGameVisuals(Icons.Default.SportsEsports, listOf(Color(0xFFB45309), Color(0xFFD97706)))
    ArenaGameId.CARROM -> ArenaGameVisuals(Icons.Default.SportsEsports, listOf(Color(0xFF78350F), Color(0xFFD97706)))
    ArenaGameId.SNAKES_LADDERS -> ArenaGameVisuals(Icons.Default.Casino, listOf(Color(0xFF065F46), Color(0xFF10B981)))
    ArenaGameId.FRUIT_SLASH -> ArenaGameVisuals(Icons.Default.SportsEsports, listOf(Color(0xFFE11D48), Color(0xFFF59E0B)))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GamesArenaScreen(
    leaderboard: List<GameLeaderboardEntity> = emptyList(),
    playWallet: PlayWalletEntity = PlayWalletEntity(ownerUserId = 0L),
    walletRequests: List<PlayWalletRequestEntity> = emptyList(),
    walletLedger: List<PlayWalletLedgerEntity> = emptyList(),
    cloudWalletSummary: CloudWalletSummaryDoc? = null,
    cloudWalletLedger: List<CloudWalletLedgerDoc> = emptyList(),
    paymentConfig: PlayPaymentConfig = PlayPaymentConfig(),
    tournamentTiers: List<TournamentTierConfigEntity> = emptyList(),
    gameBotConfigs: Map<String, GameBotConfigEntity> = emptyMap(),
    externalTournaments: List<ExternalTournamentEntity> = emptyList(),
    cloudTournaments: List<CloudTournamentDoc> = emptyList(),
    myExternalEntries: List<ExternalTournamentEntryEntity> = emptyList(),
    allExternalEntries: List<ExternalTournamentEntryEntity> = emptyList(),
    joinedCloudEntries: Map<String, CloudTournamentEntryDoc> = emptyMap(),
    inspectedCloudRoster: List<CloudTournamentEntryDoc> = emptyList(),
    currentUserId: Long = 0L,
    broadcastNotifications: List<BroadcastNotificationEntity> = emptyList(),
    liveSeatStatuses: Map<String, FirestoreMultiplayerRepository.LiveGameSeatStatus> = emptyMap(),
    matchmakingScanState: MatchmakingScanState = MatchmakingScanState.Idle,
    onlineAuth: OnlineAuthProfile? = null,
    activeOnlineRoom: MultiplayerRoomDoc? = null,
    recentOnlineRooms: List<MultiplayerRoomDoc> = emptyList(),
    selectedEntryFee: Double = 0.0,
    lastSettlement: TournamentSettlementReceipt? = null,
    playFeedbackMessage: String? = null,
    isOnlineBusy: Boolean = false,
    onClearPlayFeedback: () -> Unit = {},
    onClearSettlement: () -> Unit = {},
    onClearScanState: () -> Unit = {},
    onRefreshLiveSeats: () -> Unit = {},
    onSelectTournamentFee: (Double) -> Unit = {},
    onEnterTournamentMatch: (gameId: String, entryFee: Double, onEntered: () -> Unit) -> Unit = { _, _, cb -> cb() },
    onStartSmartMatchmaking: (gameId: String, tier: TournamentTierConfigEntity, onBotFallbackLaunch: (Int, Double, Double) -> Unit) -> Unit = { _, tier, cb -> cb(65, tier.entryFee, tier.winPrize) },
    onSettleTournamentMatch: (gameId: String, entryFee: Double, outcome: GameMatchResult, explicitWinPrize: Double?) -> Unit = { _, _, _, _ -> },
    onJoinExternalTournament: (Long, String, String, () -> Unit) -> Unit = { _, _, _, _ -> },
    onJoinHubTournament: (TournamentHubItem, String, String, () -> Unit) -> Unit = { _, _, _, _ -> },
    onInspectTournamentRoster: (String?) -> Unit = {},
    onTriggerPreMatchAlert: (ExternalTournamentEntity) -> Unit = {},
    onSubmitDeposit: (WalletPaymentChannel, Double, String, String, () -> Unit) -> Unit = { _, _, _, _, _ -> },
    onSubmitWithdraw: (WalletPaymentChannel, Double, String, () -> Unit) -> Unit = { _, _, _, _ -> },
    onTransferTournamentToGame: (Double, () -> Unit) -> Unit = { _, _ -> },
    onBuyArmoryItem: (String) -> Unit = {},
    onEquipArmoryItem: (String) -> Unit = {},
    onConsumeSkill: (String, () -> Unit) -> Unit = { _, _ -> },
    onSignInGoogleForOnline: () -> Unit = {},
    onSignOutOnline: () -> Unit = {},
    onCreateOnlineRoom: (String, Boolean, Double) -> Unit = { _, _, _ -> },
    onJoinOnlineRoom: (String) -> Unit = {},
    onQuickMatchOnline: (String, Double) -> Unit = { _, _ -> },
    onReopenOnlineRoom: (String) -> Unit = {},
    onSubmitOnlineMove: (String, String, String, Int, String, String, Boolean) -> Unit = { _, _, _, _, _, _, _ -> },
    onLeaveOnlineRoom: () -> Unit = {},
    onRecordMatch: (gameId: String, outcome: GameMatchResult, score: Int, vsBot: Boolean, botAccuracy: Int) -> Unit = { _, _, _, _, _ -> },
    onResetLeaderboard: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        GameSoundEngine.init(context)
        onRefreshLiveSeats()
    }

    val strings = LocalStrings.current
    val gStrings by LanguageManager.gameStrings.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    val soundEnabled by GameSoundEngine.soundEnabled.collectAsStateWithLifecycle()

    var selectedHubTab by remember { mutableIntStateOf(0) } // 0 = Arena, 1 = Esports (FF), 2 = Wallets, 3 = Armory
    var activeGame by remember { mutableStateOf<ArenaGameId?>(null) }
    var activeMatchFee by remember { mutableDoubleStateOf(0.0) }
    var activeMatchWinPrize by remember { mutableDoubleStateOf(0.0) }
    var tierPickerGame by remember { mutableStateOf<ArenaGameId?>(null) }
    var previewRulesGame by remember { mutableStateOf<ArenaGameId?>(null) }
    var showLeaderboardSheet by remember { mutableStateOf(false) }
    var activeSkillTip by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(playFeedbackMessage) {
        playFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearPlayFeedback()
        }
    }

    val leaderboardById = remember(leaderboard) { leaderboard.associateBy { it.gameId } }
    val skinPalette = remember(playWallet.equippedBoardSkinId) {
        PlayArmoryCatalog.boardSkinPalette(playWallet.equippedBoardSkinId)
    }
    val avatarGlyph = remember(playWallet.equippedAvatarId) {
        PlayArmoryCatalog.avatarGlyph(playWallet.equippedAvatarId)
    }
    val skillCharges = remember(playWallet.skillChargesJson) {
        PlayArmoryCatalog.parseSkillCharges(playWallet.skillChargesJson)
    }

    BackHandler {
        when {
            activeOnlineRoom != null -> onLeaveOnlineRoom()
            activeGame != null -> {
                activeGame = null
                activeMatchFee = 0.0
                activeSkillTip = null
                onClearSettlement()
            }
            else -> onBack()
        }
    }

    previewRulesGame?.let { g ->
        GameRulesModalSheet(gameId = g, onDismiss = { previewRulesGame = null })
    }

    if (showLeaderboardSheet) {
        GameLeaderboardModalSheet(
            entries = leaderboard,
            onResetLeaderboard = onResetLeaderboard,
            onDismiss = { showLeaderboardSheet = false }
        )
    }

    tierPickerGame?.let { g ->
        GameLobbyModalSheet(
            game = g,
            gStrings = gStrings,
            tournamentTiers = tournamentTiers,
            userWalletBalance = playWallet.gameBalanceCoins,
            onDismiss = { tierPickerGame = null },
            onLaunchGame = { mode, botAcc, fee, prize, roomCode ->
                tierPickerGame = null
                when (mode) {
                    GameLaunchMode.OFFLINE -> {
                        activeMatchFee = 0.0
                        activeMatchWinPrize = 0.0
                        activeSkillTip = null
                        activeGame = g
                    }
                    GameLaunchMode.BOT -> {
                        activeMatchFee = 0.0
                        activeMatchWinPrize = 0.0
                        activeSkillTip = null
                        activeGame = g
                    }
                    GameLaunchMode.FRIENDS_HOST -> {
                        onCreateOnlineRoom(g.name, true, selectedEntryFee)
                    }
                    GameLaunchMode.FRIENDS_JOIN -> {
                        if (!roomCode.isNullOrBlank()) {
                            onJoinOnlineRoom(roomCode)
                        }
                    }
                    GameLaunchMode.ONLINE_MULTIPLAYER -> {
                        onQuickMatchOnline(g.name, selectedEntryFee)
                    }
                    GameLaunchMode.TOURNAMENT -> {
                        onEnterTournamentMatch(g.name, fee) {
                            activeMatchFee = fee
                            activeMatchWinPrize = prize
                            activeSkillTip = null
                            activeGame = g
                        }
                    }
                }
            }
        )
    }

    MatchmakingScanDialog(
        scanState = matchmakingScanState,
        isBn = gStrings.isBn,
        onCancel = onClearScanState
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when {
                                activeOnlineRoom != null -> "${activeOnlineRoom.gameType} • #${activeOnlineRoom.roomCode}"
                                activeGame != null -> gStrings.gameTitle(activeGame!!)
                                else -> strings.gamesArenaTitle
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = when {
                                activeOnlineRoom != null -> gStrings.onlineFriendsTitle
                                activeGame != null -> gStrings.gameSubtitle(activeGame!!)
                                else -> gStrings.arenaHeaderSubtitle
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            GameSoundEngine.play(GameSoundEffect.TAP)
                            when {
                                activeOnlineRoom != null -> onLeaveOnlineRoom()
                                activeGame != null -> {
                                    activeGame = null
                                    activeMatchFee = 0.0
                                    activeSkillTip = null
                                    onClearSettlement()
                                }
                                else -> onBack()
                            }
                        },
                        modifier = Modifier.testTag("games_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.cancel)
                    }
                },
                actions = {
                    // Live Chat Telegram @jim151 button
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF0284C7).copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                GameSoundEngine.play(GameSoundEffect.TAP)
                                openTelegramSupportChat(context)
                            }
                            .testTag("games_topbar_live_chat_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = gStrings.liveChatLabel,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = gStrings.liveChatLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }

                    // Instant zero-restart Bangla <-> English toggle via LanguageManager singleton
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                GameSoundEngine.play(GameSoundEffect.TAP)
                                LanguageManager.toggleLanguage()
                            }
                            .testTag("games_language_toggle_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.Translate,
                                contentDescription = gStrings.liveLanguageBadge,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = gStrings.liveLanguageBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            GameSoundEngine.play(GameSoundEffect.TAP)
                            showLeaderboardSheet = true
                        },
                        modifier = Modifier.testTag("games_topbar_leaderboard_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = gStrings.leaderboardTitle,
                            tint = financeColors.champagneGold
                        )
                    }

                    IconButton(
                        onClick = { GameSoundEngine.toggleSound(context) },
                        modifier = Modifier.testTag("games_topbar_sound_btn")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (soundEnabled) gStrings.soundOn else gStrings.soundOff,
                            tint = if (soundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            contentAlignment = Alignment.TopCenter
        ) {
            // 1. Live Online Multiplayer Room view
            if (activeOnlineRoom != null && onlineAuth != null) {
                Box(modifier = Modifier.widthIn(max = 680.dp).fillMaxSize()) {
                    OnlineRoomLiveScreen(
                        room = activeOnlineRoom,
                        localUid = onlineAuth.uid,
                        skinPalette = skinPalette,
                        settlementReceipt = lastSettlement,
                        gStrings = gStrings,
                        onSubmitMove = onSubmitOnlineMove,
                        onMatchFinished = { gId, fee, outcome ->
                            val score = when (outcome) {
                                GameMatchResult.WIN -> 180
                                GameMatchResult.DRAW -> 80
                                GameMatchResult.LOSS -> 35
                            }
                            onRecordMatch(gId, outcome, score, false, 100)
                            if (fee > 0.0) onSettleTournamentMatch(gId, fee, outcome, null)
                        },
                        onLeaveOrForfeit = onLeaveOnlineRoom
                    )
                }
                return@Box
            }

            // 2. Hub Tabs or Active Offline/Tournament Game with Shared Element Transitions
            SharedTransitionLayout(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = activeGame,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(260)) + scaleIn(
                            initialScale = 0.94f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )) togetherWith (fadeOut(animationSpec = tween(180)) + scaleOut(
                            targetScale = 0.96f,
                            animationSpec = tween(180)
                        ))
                    },
                    label = "games_arena_shared_content",
                    modifier = Modifier.fillMaxSize()
                ) { current ->
                    when (current) {
                        null -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                TabRow(selectedTabIndex = selectedHubTab) {
                                    Tab(
                                        selected = selectedHubTab == 0,
                                        onClick = { selectedHubTab = 0 },
                                        text = { Text(gStrings.tabArena, maxLines = 1) },
                                        icon = { Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        modifier = Modifier.testTag("play_tab_arena")
                                    )
                                    Tab(
                                        selected = selectedHubTab == 1,
                                        onClick = { selectedHubTab = 1 },
                                        text = { Text(if (gStrings.isBn) "লাইভ হাব" else "Live Hub", maxLines = 1) },
                                        icon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        modifier = Modifier.testTag("play_tab_esports")
                                    )
                                    Tab(
                                        selected = selectedHubTab == 2,
                                        onClick = { selectedHubTab = 2 },
                                        text = { Text(gStrings.tabWallets, maxLines = 1) },
                                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        modifier = Modifier.testTag("play_tab_wallets")
                                    )
                                    Tab(
                                        selected = selectedHubTab == 3,
                                        onClick = { selectedHubTab = 3 },
                                        text = { Text(gStrings.tabArmory, maxLines = 1) },
                                        icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        modifier = Modifier.testTag("play_tab_armory")
                                    )
                                }

                                when (selectedHubTab) {
                                    0 -> LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        broadcastNotifications.firstOrNull()?.let { latestBroadcast ->
                                            item(key = "latest_admin_broadcast") {
                                                LatestBroadcastBanner(broadcast = latestBroadcast)
                                            }
                                        }
                                        item(key = "arena_hero") {
                                            Surface(
                                                shape = RoundedCornerShape(22.dp),
                                                border = BorderStroke(1.dp, skinPalette.accentBorder.copy(alpha = 0.65f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .background(
                                                            Brush.linearGradient(
                                                                listOf(skinPalette.frameStart, skinPalette.frameEnd, Color(0xFF312E81))
                                                            )
                                                        )
                                                        .padding(18.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "$avatarGlyph ${gStrings.arenaBannerTitle}",
                                                        style = MaterialTheme.typography.titleLarge.copy(
                                                            fontFamily = FontFamily.Serif,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = gStrings.arenaBannerDesc,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFFD1FAE5)
                                                    )
                                                }
                                            }
                                        }

                                        item(key = "dual_wallet_strip") {
                                            DualWalletCompactStrip(
                                                wallet = playWallet,
                                                gStrings = gStrings,
                                                onOpenWalletsTab = { selectedHubTab = 2 }
                                            )
                                        }

                                        // Tournament Entry Tier Selector
                                        item(key = "tournament_tier_selector") {
                                            Surface(
                                                shape = RoundedCornerShape(18.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, financeColors.champagneGold.copy(alpha = 0.45f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = gStrings.tournamentEntryLabel,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        PlayArmoryCatalog.TOURNAMENT_ENTRY_TIERS.forEach { tier ->
                                                            FilterChip(
                                                                selected = selectedEntryFee == tier,
                                                                onClick = { onSelectTournamentFee(tier) },
                                                                label = {
                                                                    Text(
                                                                        text = if (tier == 0.0) gStrings.freePlayLabel else "৳${tier.toInt()}",
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                },
                                                                modifier = Modifier.testTag("tournament_fee_chip_${tier.toInt()}")
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = gStrings.lossCashbackBanner,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF059669),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }

                                        // Online Multiplayer & Friends Card
                                        item(key = "online_multiplayer_lobby") {
                                            OnlineMultiplayerLobbyCard(
                                                onlineAuth = onlineAuth,
                                                avatarGlyph = avatarGlyph,
                                                selectedEntryFee = selectedEntryFee,
                                                recentRooms = recentOnlineRooms,
                                                isBusy = isOnlineBusy,
                                                gStrings = gStrings,
                                                onSignInGoogle = onSignInGoogleForOnline,
                                                onSignOutOnline = onSignOutOnline,
                                                onCreatePrivateRoom = { game ->
                                                    onCreateOnlineRoom(game.name, false, selectedEntryFee)
                                                },
                                                onQuickMatch = { game ->
                                                    onQuickMatchOnline(game.name, selectedEntryFee)
                                                },
                                                onJoinByCode = onJoinOnlineRoom,
                                                onReopenRoom = onReopenOnlineRoom
                                            )
                                        }

                                        item(key = "arena_leaderboard_overview") {
                                            GameLeaderboardOverviewCard(
                                                entries = leaderboard,
                                                onOpenFullSheet = { showLeaderboardSheet = true }
                                            )
                                        }

                                        items(ArenaGameId.entries, key = { it.name }) { game ->
                                            val seatStatus = liveSeatStatuses[game.name]
                                            ArenaGameMenuCard(
                                                game = game,
                                                stats = leaderboardById[game.name],
                                                selectedEntryFee = selectedEntryFee,
                                                seatStatus = seatStatus,
                                                gStrings = gStrings,
                                                sharedTransitionScope = this@SharedTransitionLayout,
                                                animatedVisibilityScope = this@AnimatedContent,
                                                onPlay = {
                                                    GameSoundEngine.play(GameSoundEffect.TAP)
                                                    onRefreshLiveSeats()
                                                    tierPickerGame = game
                                                },
                                                onRules = {
                                                    GameSoundEngine.play(GameSoundEffect.TAP)
                                                    previewRulesGame = game
                                                }
                                            )
                                        }

                                        item { Spacer(Modifier.height(24.dp)) }
                                    }

                                    1 -> TournamentHubView(
                                        cloudTournaments = cloudTournaments,
                                        localTournaments = externalTournaments,
                                        myLocalEntries = myExternalEntries,
                                        allLocalEntries = allExternalEntries,
                                        joinedCloudEntries = joinedCloudEntries,
                                        inspectedCloudRoster = inspectedCloudRoster,
                                        onlineAuth = onlineAuth,
                                        currentUserId = currentUserId,
                                        wallet = playWallet,
                                        isBn = gStrings.isBn,
                                        onSignInGoogle = onSignInGoogleForOnline,
                                        onInspectRoster = onInspectTournamentRoster,
                                        onTriggerPreMatchAlert = onTriggerPreMatchAlert,
                                        onJoinHubTournament = { item, ign, uid, done ->
                                            if (item.cloudDocId != null) {
                                                onJoinHubTournament(item, ign, uid, done)
                                            } else if (item.localId != null) {
                                                onJoinExternalTournament(item.localId, ign, uid, done)
                                            }
                                        }
                                    )

                                    2 -> PlayWalletsTabContent(
                                        wallet = playWallet,
                                        requests = walletRequests,
                                        ledger = walletLedger,
                                        paymentConfig = paymentConfig,
                                        gStrings = gStrings,
                                        cloudWallet = cloudWalletSummary,
                                        cloudLedger = cloudWalletLedger,
                                        onlineAuth = onlineAuth,
                                        onSignInGoogle = onSignInGoogleForOnline,
                                        onSubmitDeposit = onSubmitDeposit,
                                        onSubmitWithdraw = onSubmitWithdraw,
                                        onTransferToGame = onTransferTournamentToGame
                                    )

                                    3 -> PlayArmoryTabContent(
                                        wallet = playWallet,
                                        gStrings = gStrings,
                                        onBuyItem = onBuyArmoryItem,
                                        onEquipItem = onEquipArmoryItem
                                    )
                                }
                            }
                        }

                        else -> {
                            val gameStats = leaderboardById[current.name]
                            Column(modifier = Modifier.fillMaxSize()) {
                                ActiveGameHeroHeader(
                                    game = current,
                                    stats = gameStats,
                                    skinPalette = skinPalette,
                                    entryFee = activeMatchFee,
                                    skillCharges = skillCharges,
                                    gStrings = gStrings,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@AnimatedContent,
                                    onUseSkill = { skillId, tipMsg ->
                                        onConsumeSkill(skillId) {
                                            GameSoundEngine.play(GameSoundEffect.SPECIAL)
                                            activeSkillTip = tipMsg
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )

                                activeSkillTip?.let { tip ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = skinPalette.accentBorder.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, skinPalette.accentBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "⚡ $tip",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                if (lastSettlement != null && lastSettlement.creditedAmount > 0.0) {
                                    TournamentSettlementBanner(
                                        receipt = lastSettlement,
                                        gStrings = gStrings,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(skinPalette.feltSurface.copy(alpha = 0.08f))
                                ) {
                                    val settleAndRecord: (GameMatchResult, Int, Boolean, Int) -> Unit = { outcome, score, vsBot, acc ->
                                        onRecordMatch(current.name, outcome, score, vsBot, acc)
                                        if (activeMatchFee > 0.0) {
                                            val feeToSettle = activeMatchFee
                                            val prizeToSettle = activeMatchWinPrize.takeIf { it > 0.0 }
                                            activeMatchFee = 0.0
                                            activeMatchWinPrize = 0.0
                                            onSettleTournamentMatch(current.name, feeToSettle, outcome, prizeToSettle)
                                        }
                                    }

                                    when (current) {
                                        ArenaGameId.LUDO -> LudoBoardScreen(onRecordMatch = settleAndRecord)
                                        ArenaGameId.CHESS -> ChessBoardScreen(onRecordMatch = settleAndRecord)
                                        ArenaGameId.AETHER_CROWN -> AetherCrownScreen(onRecordMatch = settleAndRecord)
                                        ArenaGameId.REVERSI -> ReversiBoardScreen(onRecordMatch = settleAndRecord)
                                        ArenaGameId.CONNECT_FOUR -> ConnectFourScreen(onRecordMatch = settleAndRecord)
                                        ArenaGameId.WAR_LAND -> WarLandScreen(
                                            onRecordMatch = settleAndRecord,
                                            onExit = { activeGame = null }
                                        )
                                        ArenaGameId.CARROM -> CarromBoardScreen(
                                            onRecordMatch = settleAndRecord,
                                            onExit = { activeGame = null }
                                        )
                                        ArenaGameId.SNAKES_LADDERS -> SnakesLaddersScreen(
                                            onRecordMatch = settleAndRecord,
                                            onExit = { activeGame = null }
                                        )
                                        ArenaGameId.FRUIT_SLASH -> FruitSlashScreen(
                                            onRecordMatch = settleAndRecord,
                                            onExit = { activeGame = null }
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ArenaGameMenuCard(
    game: ArenaGameId,
    stats: GameLeaderboardEntity?,
    selectedEntryFee: Double,
    seatStatus: FirestoreMultiplayerRepository.LiveGameSeatStatus? = null,
    gStrings: GameUiStrings,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onPlay: () -> Unit,
    onRules: () -> Unit
) {
    val financeColors = LocalFinanceColors.current
    val visuals = visualsFor(game)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "menu_card_spring_${game.name}"
    )

    with(sharedTransitionScope) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, financeColors.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "game_container_${game.name}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ ->
                        spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    }
                )
                .clip(RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onPlay
                )
                .testTag("game_card_${game.name.lowercase()}")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Professional 3D Game Banner Artwork
                val bannerRes = gStrings.bannerResFor(game)
                if (bannerRes != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(136.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        Image(
                            painter = painterResource(bannerRes),
                            contentDescription = gStrings.gameTitle(game),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Depth overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xFF0F172A).copy(alpha = 0.55f))
                                    )
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .sharedElement(
                                state = rememberSharedContentState(key = "game_icon_${game.name}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                }
                            )
                            .clip(CircleShape)
                            .background(Brush.linearGradient(visuals.gradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(visuals.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = gStrings.gameTitle(game),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState(key = "game_title_${game.name}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = financeColors.champagneGold.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = gStrings.gameBadge(game),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = financeColors.champagneGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = gStrings.gameSubtitle(game),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (stats != null && stats.gamesPlayed > 0) {
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "🏆 ${stats.highScore} pts",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = financeColors.champagneGold
                                )
                                Text(
                                    text = "• ${stats.winRatePercent().roundToInt()}% ${gStrings.winRateLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        seatStatus?.let { status ->
                            Spacer(Modifier.height(6.dp))
                            val isBusy = status.isServerBusy
                            val pillColor = if (isBusy) Color(0xFFEF4444) else Color(0xFF10B981)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = pillColor.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, pillColor.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = if (!status.multiplayerEnabled) "🔴 Online Paused"
                                        else if (isBusy) "🔴 Server Busy (${status.waitingRoomsCount}/${status.maxConcurrentSeats})"
                                        else "🟢 Seats Open (${status.openSeats}/${status.maxConcurrentSeats})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = pillColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                if (status.waitingPlayerNames.isNotEmpty() && !isBusy) {
                                    Text(
                                        text = "Live: ${status.waitingPlayerNames.take(2).joinToString(", ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPlay,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("play_${game.name.lowercase()}_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (selectedEntryFee > 0.0) "${gStrings.playNow} (৳${selectedEntryFee.toInt()})"
                            else gStrings.playNow
                        )
                    }
                    OutlinedButton(
                        onClick = onRules,
                        modifier = Modifier.testTag("rules_${game.name.lowercase()}_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(gStrings.rules)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ActiveGameHeroHeader(
    game: ArenaGameId,
    stats: GameLeaderboardEntity?,
    skinPalette: BoardSkinPalette,
    entryFee: Double,
    skillCharges: Map<String, Int>,
    gStrings: GameUiStrings,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onUseSkill: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = visualsFor(game)
    val recommendedSkillId = when (game) {
        ArenaGameId.CHESS, ArenaGameId.REVERSI, ArenaGameId.CONNECT_FOUR -> PlayArmoryCatalog.SKILL_CHESS_HINT
        ArenaGameId.LUDO, ArenaGameId.SNAKES_LADDERS -> PlayArmoryCatalog.SKILL_LUDO_REROLL
        ArenaGameId.AETHER_CROWN -> PlayArmoryCatalog.SKILL_FLUX_CHARGE
        ArenaGameId.WAR_LAND, ArenaGameId.CARROM, ArenaGameId.FRUIT_SLASH -> PlayArmoryCatalog.SKILL_UNDO_PASS
    }
    val recommendedSkill = PlayArmoryCatalog.itemById(recommendedSkillId)
    val chargeCount = skillCharges[recommendedSkillId] ?: 0

    with(sharedTransitionScope) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, skinPalette.accentBorder.copy(alpha = 0.7f)),
            modifier = modifier
                .fillMaxWidth()
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "game_container_${game.name}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ ->
                        spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    }
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .sharedElement(
                                state = rememberSharedContentState(key = "game_icon_${game.name}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                }
                            )
                            .clip(CircleShape)
                            .background(Brush.linearGradient(visuals.gradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(visuals.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = gStrings.gameTitle(game),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "game_title_${game.name}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        )
                        Text(
                            text = when {
                                entryFee > 0.0 -> "🏆 ৳${entryFee.toInt()} Tournament (100% Cashback)"
                                stats != null && stats.gamesPlayed > 0 ->
                                    "${gStrings.highScoreLabel}: ${stats.highScore} • ${gStrings.winRateLabel}: ${stats.winRatePercent().roundToInt()}%"
                                else -> gStrings.gameBadge(game)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = skinPalette.accentBorder
                        )
                    }
                }

                if (recommendedSkill != null && chargeCount > 0) {
                    FilterChip(
                        selected = true,
                        onClick = {
                            val tip = if (gStrings.isBn) {
                                "${recommendedSkill.titleBn} চালু হয়েছে! ${recommendedSkill.descBn}"
                            } else {
                                "${recommendedSkill.titleEn} activated! ${recommendedSkill.descEn}"
                            }
                            onUseSkill(recommendedSkill.id, tip)
                        },
                        label = {
                            Text(
                                text = "${recommendedSkill.iconGlyph} x$chargeCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier.testTag("active_game_use_skill_chip")
                    )
                }
            }
        }
    }
}
