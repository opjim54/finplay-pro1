package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.GoogleAccountSignInOutcome
import com.example.data.games.CloudBroadcastDoc
import com.example.data.games.CloudTournamentDoc
import com.example.data.games.CloudTournamentEntryDoc
import com.example.data.games.CloudWalletLedgerDoc
import com.example.data.games.CloudWalletSummaryDoc
import com.example.data.games.FirestoreMultiplayerRepository
import com.example.data.games.FirestoreTournamentRepository
import com.example.data.games.MultiplayerRoomDoc
import com.example.data.games.PlayWalletRepository
import com.example.data.games.TournamentHubItem
import com.example.data.games.TournamentSettlementReceipt
import com.example.data.local.BroadcastNotificationEntity
import com.example.data.local.ExternalTournamentEntity
import com.example.data.local.ExternalTournamentEntryEntity
import com.example.data.local.GameBotConfigEntity
import com.example.data.local.GameMatchResult
import com.example.data.local.PlayWalletEntity
import com.example.data.local.PlayWalletLedgerEntity
import com.example.data.local.PlayWalletRequestEntity
import com.example.data.local.TournamentTierConfigEntity
import com.example.data.local.WalletPaymentChannel
import com.example.data.repository.FinanceRepository
import com.example.domain.NotificationHelper
import com.example.domain.games.PlayArmoryCatalog
import com.example.domain.games.PlayPaymentConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OnlineAuthProfile(
    val uid: String,
    val displayName: String,
    val email: String
)

sealed interface MatchmakingScanState {
    data object Idle : MatchmakingScanState
    data class Scanning(
        val gameId: String,
        val tierLabel: String,
        val entryFee: Double,
        val winPrize: Double,
        val secondsRemaining: Int,
        val detectedPlayerNames: List<String>,
        val botAccuracyFallback: Int
    ) : MatchmakingScanState

    data class ServerBusy(
        val gameId: String,
        val activeSeats: Int,
        val maxSeats: Int,
        val message: String
    ) : MatchmakingScanState
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlayEarnViewModel(
    private val appContext: Context,
    private val repository: FinanceRepository
) : ViewModel() {

    private val walletRepo = repository.playWalletRepo
    private val multiplayerRepo = FirestoreMultiplayerRepository(appContext)
    private val cloudTournamentRepo = FirestoreTournamentRepository(appContext)

    private val activeUserIdFlow = repository.prefsFlow
        .map { it.currentUserId }
        .distinctUntilChanged()

    val playWallet: StateFlow<PlayWalletEntity> = activeUserIdFlow
        .flatMapLatest { uid ->
            if (uid <= 0L) flowOf(PlayWalletEntity(ownerUserId = 0L))
            else walletRepo.observeWallet(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayWalletEntity(ownerUserId = 0L))

    val walletRequests: StateFlow<List<PlayWalletRequestEntity>> = activeUserIdFlow
        .flatMapLatest { uid ->
            if (uid <= 0L) flowOf(emptyList()) else walletRepo.observeUserRequests(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val walletLedger: StateFlow<List<PlayWalletLedgerEntity>> = activeUserIdFlow
        .flatMapLatest { uid ->
            if (uid <= 0L) flowOf(emptyList()) else walletRepo.observeLedger(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentConfig: StateFlow<PlayPaymentConfig> = walletRepo.observePaymentConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayPaymentConfig())

    val tournamentTiers: StateFlow<List<TournamentTierConfigEntity>> = walletRepo.observeAllTiers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gameBotConfigs: StateFlow<Map<String, GameBotConfigEntity>> = walletRepo.observeAllBotConfigs()
        .map { list -> list.associateBy { it.gameId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val externalTournaments: StateFlow<List<ExternalTournamentEntity>> = walletRepo.observeExternalTournaments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myExternalEntries: StateFlow<List<ExternalTournamentEntryEntity>> = activeUserIdFlow
        .flatMapLatest { uid ->
            if (uid <= 0L) flowOf(emptyList()) else walletRepo.observeUserExternalEntries(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExternalEntries: StateFlow<List<ExternalTournamentEntryEntity>> = walletRepo.observeAllExternalEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val broadcastNotifications: StateFlow<List<BroadcastNotificationEntity>> = walletRepo.observeBroadcastNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _liveSeatStatuses = MutableStateFlow<Map<String, FirestoreMultiplayerRepository.LiveGameSeatStatus>>(emptyMap())
    val liveSeatStatuses: StateFlow<Map<String, FirestoreMultiplayerRepository.LiveGameSeatStatus>> = _liveSeatStatuses.asStateFlow()

    private val _matchmakingScanState = MutableStateFlow<MatchmakingScanState>(MatchmakingScanState.Idle)
    val matchmakingScanState: StateFlow<MatchmakingScanState> = _matchmakingScanState.asStateFlow()
    private var scanJob: Job? = null

    private val _onlineAuth = MutableStateFlow<OnlineAuthProfile?>(readCurrentAuthProfile())
    val onlineAuth: StateFlow<OnlineAuthProfile?> = _onlineAuth.asStateFlow()

    val cloudWalletSummary: StateFlow<CloudWalletSummaryDoc?> = _onlineAuth
        .flatMapLatest { auth ->
            if (auth == null) flowOf(null) else multiplayerRepo.observeCloudWalletSummary()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val cloudWalletLedger: StateFlow<List<CloudWalletLedgerDoc>> = _onlineAuth
        .flatMapLatest { auth ->
            if (auth == null) flowOf(emptyList()) else multiplayerRepo.observeCloudWalletLedger()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudBroadcasts: StateFlow<List<CloudBroadcastDoc>> = _onlineAuth
        .flatMapLatest { auth ->
            if (auth == null) flowOf(emptyList()) else multiplayerRepo.observeCloudBroadcasts()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudTournaments: StateFlow<List<CloudTournamentDoc>> = _onlineAuth
        .flatMapLatest { auth ->
            if (auth == null) flowOf(emptyList()) else cloudTournamentRepo.observeTournaments()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _inspectedCloudTournamentId = MutableStateFlow<String?>(null)
    val inspectedCloudRoster: StateFlow<List<CloudTournamentEntryDoc>> = _inspectedCloudTournamentId
        .flatMapLatest { docId ->
            if (docId.isNullOrBlank() || _onlineAuth.value == null) flowOf(emptyList())
            else cloudTournamentRepo.observeTournamentEntries(docId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _joinedCloudEntries = MutableStateFlow<Map<String, CloudTournamentEntryDoc>>(emptyMap())
    val joinedCloudEntries: StateFlow<Map<String, CloudTournamentEntryDoc>> = _joinedCloudEntries.asStateFlow()

    private val seenCloudBroadcastIds = mutableSetOf<String>()
    private val alertedPreMatchIds = mutableSetOf<Long>()

    private val authStateListener = FirebaseAuth.AuthStateListener {
        _onlineAuth.value = readCurrentAuthProfile()
        refreshLiveSeatStatuses()
    }

    init {
        runCatching { Firebase.auth.addAuthStateListener(authStateListener) }
        viewModelScope.launch {
            walletRepo.ensureDefaultGameConfigs()
            refreshLiveSeatStatuses()
        }
        // Listen for real-time Firestore broadcasts & fire heads-up Android Push Notifications
        viewModelScope.launch {
            var initialized = false
            cloudBroadcasts.collect { list ->
                if (!initialized) {
                    list.forEach { seenCloudBroadcastIds.add(it.id) }
                    initialized = true
                } else {
                    list.firstOrNull { it.id !in seenCloudBroadcastIds }?.let { fresh ->
                        seenCloudBroadcastIds.add(fresh.id)
                        NotificationHelper.postBroadcastPushNotification(
                            context = appContext,
                            broadcastId = fresh.id.hashCode().toLong(),
                            title = fresh.title,
                            message = fresh.message,
                            priority = fresh.priority
                        )
                        _feedbackMessage.value = "[${fresh.priority}] ${fresh.title}: ${fresh.message}"
                    }
                }
            }
        }
    }

    override fun onCleared() {
        scanJob?.cancel()
        runCatching { Firebase.auth.removeAuthStateListener(authStateListener) }
        super.onCleared()
    }

    private fun readCurrentAuthProfile(): OnlineAuthProfile? {
        val user = runCatching { Firebase.auth.currentUser }.getOrNull() ?: return null
        return OnlineAuthProfile(
            uid = user.uid,
            displayName = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
                ?: "FinPlay Player",
            email = user.email.orEmpty()
        )
    }

    private val _activeRoomCode = MutableStateFlow<String?>(null)
    val activeRoomCode: StateFlow<String?> = _activeRoomCode.asStateFlow()

    val activeRoom: StateFlow<MultiplayerRoomDoc?> = _activeRoomCode
        .flatMapLatest { code ->
            if (code.isNullOrBlank()) flowOf(null) else multiplayerRepo.observeRoom(code)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentOnlineRooms: StateFlow<List<MultiplayerRoomDoc>> = _onlineAuth
        .flatMapLatest { auth ->
            if (auth == null) flowOf(emptyList()) else multiplayerRepo.observeMyRecentRooms()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedEntryFee = MutableStateFlow(0.0)
    val selectedEntryFee: StateFlow<Double> = _selectedEntryFee.asStateFlow()

    private val _selectedWinPrize = MutableStateFlow(0.0)
    val selectedWinPrize: StateFlow<Double> = _selectedWinPrize.asStateFlow()

    private val _lastSettlement = MutableStateFlow<TournamentSettlementReceipt?>(null)
    val lastSettlement: StateFlow<TournamentSettlementReceipt?> = _lastSettlement.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun clearSettlement() {
        _lastSettlement.value = null
    }

    fun clearScanState() {
        scanJob?.cancel()
        _matchmakingScanState.value = MatchmakingScanState.Idle
    }

    fun selectTournamentFee(fee: Double, winPrize: Double = fee * 1.8) {
        _selectedEntryFee.value = fee.coerceAtLeast(0.0)
        _selectedWinPrize.value = winPrize.coerceAtLeast(0.0)
    }

    private suspend fun currentLocalUserId(): Long = repository.prefs.getSnapshot().currentUserId

    private fun decoratedPlayerName(): String {
        val avatar = PlayArmoryCatalog.avatarGlyph(playWallet.value.equippedAvatarId)
        val baseName = _onlineAuth.value?.displayName ?: multiplayerRepo.currentUserDisplayName()
        return "$avatar $baseName".take(70)
    }

    fun refreshLiveSeatStatuses() {
        viewModelScope.launch {
            val map = mutableMapOf<String, FirestoreMultiplayerRepository.LiveGameSeatStatus>()
            PlayWalletRepository.ALL_INBUILT_GAME_IDS.forEach { gid ->
                val cfg = walletRepo.getBotConfigOnce(gid)
                map[gid] = multiplayerRepo.fetchLiveSeatAvailability(
                    gameType = gid,
                    maxConcurrentSeats = cfg.maxConcurrentSeats,
                    multiplayerEnabled = cfg.multiplayerEnabled
                )
            }
            _liveSeatStatuses.value = map
        }
    }

    /**
     * Smart Online Player Scan + Seat Capacity Check + Automatic Bot Fallback:
     * 1. Checks Admin's live seat cap for [gameId]. If full/disabled -> displays Server Busy alert.
     * 2. Deducts [tier.entryFee] cleanly once from Tournament/Winning balance.
     * 3. Scans Firestore for live real players for 5 seconds, showing real player names.
     *    - If a real human player joins -> opens live synced Online Multiplayer match with both real names!
     *    - If no real human player is found -> cancels temp room & launches the match vs Arena Bot
     *      locked to Admin's exact configured Bot Accuracy %!
     */
    fun startSmartMatchmaking(
        gameId: String,
        tier: TournamentTierConfigEntity,
        onBotFallbackLaunch: (botAccuracy: Int, entryFee: Double, winPrize: Double) -> Unit
    ) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) return@launch
            val botCfg = walletRepo.getBotConfigOnce(gameId)
            val seatStatus = multiplayerRepo.fetchLiveSeatAvailability(
                gameType = gameId,
                maxConcurrentSeats = botCfg.maxConcurrentSeats,
                multiplayerEnabled = botCfg.multiplayerEnabled
            )
            _liveSeatStatuses.value = _liveSeatStatuses.value + (gameId to seatStatus)

            if (seatStatus.isServerBusy) {
                _matchmakingScanState.value = MatchmakingScanState.ServerBusy(
                    gameId = gameId,
                    activeSeats = seatStatus.waitingRoomsCount,
                    maxSeats = seatStatus.maxConcurrentSeats,
                    message = "Server is busy (${seatStatus.waitingRoomsCount}/${seatStatus.maxConcurrentSeats} seats filled). Please wait for a match to finish and try again!"
                )
                return@launch
            }

            if (tier.entryFee > 0.0) {
                val entryRes = walletRepo.enterTournamentMatch(uid, gameId, tier.entryFee)
                if (entryRes.isFailure) {
                    _feedbackMessage.value = entryRes.exceptionOrNull()?.message
                        ?: "Insufficient Tournament Wallet balance"
                    return@launch
                }
                launch { multiplayerRepo.syncWalletSummaryToCloud(entryRes.getOrThrow()) }
            }

            _selectedEntryFee.value = tier.entryFee
            _selectedWinPrize.value = tier.winPrize
            _lastSettlement.value = null

            // If user is signed in with Google/Firebase, scan Firestore for real players first
            val isSignedInOnline = _onlineAuth.value != null
            var tempWaitingRoom: MultiplayerRoomDoc? = null

            if (isSignedInOnline) {
                val quickRes = multiplayerRepo.findOrCreateQuickMatch(
                    gameType = gameId,
                    playerDisplayLabel = decoratedPlayerName(),
                    entryFee = tier.entryFee
                )
                val room = quickRes.getOrNull()
                if (room != null && room.status == "PLAYING" && room.guestUid.isNotBlank()) {
                    // Instant real player match found!
                    _matchmakingScanState.value = MatchmakingScanState.Idle
                    _activeRoomCode.value = room.roomCode
                    _feedbackMessage.value = "Real opponent matched: ${room.hostName} vs ${room.guestName}!"
                    return@launch
                }
                tempWaitingRoom = room
            }

            // Animated 5-second radar scan checking for incoming real player
            for (sec in 5 downTo 1) {
                _matchmakingScanState.value = MatchmakingScanState.Scanning(
                    gameId = gameId,
                    tierLabel = tier.tierLabel,
                    entryFee = tier.entryFee,
                    winPrize = tier.winPrize,
                    secondsRemaining = sec,
                    detectedPlayerNames = seatStatus.waitingPlayerNames,
                    botAccuracyFallback = botCfg.botAccuracyPercent
                )
                delay(1000L)

                if (tempWaitingRoom != null) {
                    val latest = multiplayerRepo.observeRoom(tempWaitingRoom.roomCode).firstOrNull()
                    if (latest != null && latest.status == "PLAYING" && latest.guestUid.isNotBlank()) {
                        _matchmakingScanState.value = MatchmakingScanState.Idle
                        _activeRoomCode.value = latest.roomCode
                        _feedbackMessage.value = "Player joined: ${latest.guestName}!"
                        return@launch
                    }
                }
            }

            // No real online opponent joined within 5s -> cancel temp waiting room & fall back to Admin-configured Bot
            if (tempWaitingRoom != null) {
                runCatching { multiplayerRepo.cancelOrForfeitRoom(tempWaitingRoom) }
            }
            _matchmakingScanState.value = MatchmakingScanState.Idle
            _feedbackMessage.value = "No online player free — Matched vs Arena Bot (${botCfg.botAccuracyPercent}% Accuracy)"
            onBotFallbackLaunch(botCfg.botAccuracyPercent, tier.entryFee, tier.winPrize)
        }
    }

    // ---- External Esports Tournaments (Free Fire etc.) ----

    fun inspectTournamentRoster(cloudDocId: String?) {
        _inspectedCloudTournamentId.value = cloudDocId
    }

    fun joinHubTournament(
        item: TournamentHubItem,
        inGameName: String,
        inGameUid: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) return@launch
            val cloudDocId = item.cloudDocId
            val localId = item.localId

            // 1. If local mirror exists, perform atomic local join + fee deduction first
            if (localId != null && externalTournaments.value.any { it.id == localId }) {
                walletRepo.joinExternalTournament(uid, localId, inGameName, inGameUid)
                    .onSuccess { entry ->
                        launch {
                            val updated = walletRepo.getWalletOnce(uid)
                            multiplayerRepo.syncWalletSummaryToCloud(updated)
                            if (entry.entryFeePaid > 0.0) {
                                multiplayerRepo.appendCloudWalletLedgerEntry(
                                    walletBucket = "TOURNAMENT",
                                    kind = "EXTERNAL_TOURNAMENT_ENTRY",
                                    amountDelta = -entry.entryFeePaid,
                                    note = "Joined ${item.title} as ${entry.inGameName}"
                                )
                            }
                            if (cloudDocId != null && _onlineAuth.value != null) {
                                cloudTournamentRepo.joinCloudTournamentAtomic(
                                    tournamentDocId = cloudDocId,
                                    usernameSnapshot = entry.usernameSnapshot.ifBlank { decoratedPlayerName() },
                                    inGameName = entry.inGameName,
                                    inGameUid = entry.inGameUid,
                                    entryFeePaid = entry.entryFeePaid
                                ).onSuccess { ce ->
                                    _joinedCloudEntries.value = _joinedCloudEntries.value + (cloudDocId to ce)
                                }
                            }
                            repository.triggerAdminMasterDriveAutoSync()
                        }
                        _feedbackMessage.value = "Joined ${item.title}! Room details & Live Roster unlocked."
                        onSuccess()
                    }
                    .onFailure { err ->
                        _feedbackMessage.value = err.message ?: "Could not join tournament"
                    }
                return@launch
            }

            // 2. Cloud-only tournament (launched from another device by Admin!)
            if (cloudDocId != null) {
                if (_onlineAuth.value == null) {
                    _feedbackMessage.value = "Sign in with Google to join live cloud tournaments"
                    return@launch
                }
                if (item.entryFee > 0.0) {
                    val feeRes = walletRepo.enterTournamentMatch(uid, item.gameCategory, item.entryFee)
                    if (feeRes.isFailure) {
                        _feedbackMessage.value = feeRes.exceptionOrNull()?.message
                            ?: "Insufficient Tournament Wallet balance"
                        return@launch
                    }
                }
                val username = _onlineAuth.value?.displayName ?: decoratedPlayerName()
                cloudTournamentRepo.joinCloudTournamentAtomic(
                    tournamentDocId = cloudDocId,
                    usernameSnapshot = username,
                    inGameName = inGameName,
                    inGameUid = inGameUid,
                    entryFeePaid = item.entryFee
                ).onSuccess { ce ->
                    _joinedCloudEntries.value = _joinedCloudEntries.value + (cloudDocId to ce)
                    launch {
                        val updated = walletRepo.getWalletOnce(uid)
                        multiplayerRepo.syncWalletSummaryToCloud(updated)
                        if (item.entryFee > 0.0) {
                            multiplayerRepo.appendCloudWalletLedgerEntry(
                                walletBucket = "TOURNAMENT",
                                kind = "EXTERNAL_TOURNAMENT_ENTRY",
                                amountDelta = -item.entryFee,
                                note = "Joined ${item.title} as ${ce.inGameName}"
                            )
                        }
                        repository.triggerAdminMasterDriveAutoSync()
                    }
                    _feedbackMessage.value = "Joined ${item.title} LIVE on Firestore! Roster updated."
                    onSuccess()
                }.onFailure { err ->
                    // Refund local fee if Firestore rejected the join
                    if (item.entryFee > 0.0) {
                        walletRepo.settleTournamentMatch(uid, item.gameCategory, item.entryFee, GameMatchResult.DRAW)
                    }
                    _feedbackMessage.value = err.message ?: "Could not join cloud tournament"
                }
            }
        }
    }

    fun joinExternalTournament(
        tournamentId: Long,
        inGameName: String,
        inGameUid: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) return@launch
            walletRepo.joinExternalTournament(uid, tournamentId, inGameName, inGameUid)
                .onSuccess { entry ->
                    launch {
                        val updated = walletRepo.getWalletOnce(uid)
                        multiplayerRepo.syncWalletSummaryToCloud(updated)
                        if (entry.entryFeePaid > 0.0) {
                            multiplayerRepo.appendCloudWalletLedgerEntry(
                                walletBucket = "TOURNAMENT",
                                kind = "EXTERNAL_TOURNAMENT_ENTRY",
                                amountDelta = -entry.entryFeePaid,
                                note = "Joined Free Fire Tournament (#$tournamentId) as ${entry.inGameName}"
                            )
                        }
                        if (_onlineAuth.value != null) {
                            val docId = FirestoreTournamentRepository.stableDocIdForLocal(tournamentId)
                            cloudTournamentRepo.joinCloudTournamentAtomic(
                                tournamentDocId = docId,
                                usernameSnapshot = entry.usernameSnapshot.ifBlank { decoratedPlayerName() },
                                inGameName = entry.inGameName,
                                inGameUid = entry.inGameUid,
                                entryFeePaid = entry.entryFeePaid
                            ).onSuccess { ce ->
                                _joinedCloudEntries.value = _joinedCloudEntries.value + (docId to ce)
                            }
                        }
                        repository.triggerAdminMasterDriveAutoSync()
                    }
                    _feedbackMessage.value = "Joined tournament! Room ID & Password unlocked on card."
                    onSuccess()
                }
                .onFailure { err ->
                    _feedbackMessage.value = err.message ?: "Could not join tournament"
                }
        }
    }

    /**
     * Automatically fires a high-priority pre-match Room ID & Password push alert
     * once when a joined player's live countdown enters the pre-match alert window.
     */
    fun triggerPreMatchRoomAlertIfDue(tournament: ExternalTournamentEntity) {
        if (tournament.id in alertedPreMatchIds) return
        alertedPreMatchIds.add(tournament.id)
        NotificationHelper.postTournamentRoomAlert(
            context = appContext,
            tournamentId = tournament.id,
            tournamentTitle = tournament.title,
            roomId = tournament.roomIdCode,
            roomPassword = tournament.roomPassword
        )
        _feedbackMessage.value = if (tournament.roomIdCode.isNotBlank()) {
            "🔥 ${tournament.title}: Room ID ${tournament.roomIdCode} | Pass ${tournament.roomPassword} — দ্রুত Free Fire-এ জয়েন করুন!"
        } else {
            "🔥 ${tournament.title} ম্যাচ শুরু হতে যাচ্ছে! দ্রুত Room ID/Pass চেক করে Free Fire-এ জয়েন করুন!"
        }
    }

    // ---- Google Sign-In for Online Multiplayer & Cloud Mirroring ----

    fun signInWithGoogleForOnline(activity: Activity) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                when (val outcome = repository.driveClient.signInWithGoogleAccount(activity)) {
                    is GoogleAccountSignInOutcome.Success -> {
                        _onlineAuth.value = readCurrentAuthProfile()
                        if (outcome.email.isNotBlank()) {
                            repository.prefs.setDriveConnection(outcome.email, authorized = true)
                        }
                        multiplayerRepo.syncWalletSummaryToCloud(playWallet.value)
                        refreshLiveSeatStatuses()
                        _feedbackMessage.value = "Signed in as ${outcome.displayName ?: outcome.email}"
                    }
                    is GoogleAccountSignInOutcome.Cancelled -> {
                        _feedbackMessage.value = "Google sign-in cancelled"
                    }
                    is GoogleAccountSignInOutcome.NoAccount -> {
                        _feedbackMessage.value = outcome.message
                    }
                    is GoogleAccountSignInOutcome.Error -> {
                        _feedbackMessage.value = outcome.message
                    }
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun signOutOnlineAccount() {
        viewModelScope.launch {
            _activeRoomCode.value = null
            repository.driveClient.signOutGoogleAccount()
            _onlineAuth.value = null
            _feedbackMessage.value = "Signed out from Online Multiplayer"
        }
    }

    // ---- Dual Wallet & Manual Deposit / Withdrawal ----

    fun submitDeposit(
        channel: WalletPaymentChannel,
        amount: Double,
        senderNumber: String,
        trxId: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) return@launch
            walletRepo.submitDepositRequest(uid, channel, amount, senderNumber, trxId)
                .onSuccess { req ->
                    launch {
                        multiplayerRepo.mirrorWalletRequestToCloud(req)
                        repository.triggerAdminMasterDriveAutoSync()
                    }
                    _feedbackMessage.value = "Deposit request submitted (Pending Admin approval)"
                    onSuccess()
                }
                .onFailure { err ->
                    _feedbackMessage.value = err.message ?: "Deposit request failed"
                }
        }
    }

    fun submitWithdraw(
        channel: WalletPaymentChannel,
        amount: Double,
        receiverNumber: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) return@launch
            walletRepo.submitWithdrawRequest(uid, channel, amount, receiverNumber)
                .onSuccess { req ->
                    launch {
                        multiplayerRepo.mirrorWalletRequestToCloud(req)
                        multiplayerRepo.syncWalletSummaryToCloud(walletRepo.getWalletOnce(uid))
                        multiplayerRepo.appendCloudWalletLedgerEntry(
                            walletBucket = "WINNING",
                            kind = "WITHDRAW_REQUESTED",
                            amountDelta = -amount,
                            note = "${channel.name} Winning Withdraw to $receiverNumber"
                        )
                        repository.triggerAdminMasterDriveAutoSync()
                    }
                    _feedbackMessage.value = "Withdrawal requested from Winning Coins! Admin alerted."
                    onSuccess()
                }
                .onFailure { err ->
                    _feedbackMessage.value = err.message ?: "Withdrawal failed"
                }
        }
    }

    /**
     * Strictly ONE-WAY transfer: Tournament Wallet -> Game Wallet.
     */
    fun transferTournamentToGame(amount: Double, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) return@launch
            walletRepo.transferTournamentToGameWallet(uid, amount)
                .onSuccess { updated ->
                    launch {
                        multiplayerRepo.syncWalletSummaryToCloud(updated)
                        multiplayerRepo.appendCloudWalletLedgerEntry(
                            walletBucket = "TOURNAMENT",
                            kind = "TRANSFER_TO_GAME_WALLET",
                            amountDelta = -amount,
                            note = "One-way transfer to Game Wallet"
                        )
                        repository.triggerAdminMasterDriveAutoSync()
                    }
                    _feedbackMessage.value = "Transferred ${amount.toInt()} to Game Wallet"
                    onSuccess()
                }
                .onFailure { err ->
                    _feedbackMessage.value = err.message ?: "Transfer failed"
                }
        }
    }

    // ---- Tournament Entry & 100% Loss Cashback Settlement ----

    fun enterTournamentMatch(
        gameId: String,
        entryFee: Double,
        onEntered: () -> Unit
    ) {
        viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) return@launch
            walletRepo.enterTournamentMatch(uid, gameId, entryFee)
                .onSuccess { updated ->
                    if (entryFee > 0.0) {
                        launch {
                            multiplayerRepo.syncWalletSummaryToCloud(updated)
                            multiplayerRepo.appendCloudWalletLedgerEntry(
                                walletBucket = "TOURNAMENT",
                                kind = "TOURNAMENT_ENTRY",
                                amountDelta = -entryFee,
                                note = "$gameId Tournament Entry"
                            )
                        }
                    }
                    _lastSettlement.value = null
                    onEntered()
                }
                .onFailure { err ->
                    _feedbackMessage.value = err.message ?: "Insufficient Tournament Wallet balance"
                }
        }
    }

    fun settleTournamentMatch(
        gameId: String,
        entryFee: Double,
        outcome: GameMatchResult,
        explicitWinPrize: Double? = _selectedWinPrize.value.takeIf { it > 0.0 }
    ) {
        if (entryFee <= 0.0) return
        viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) return@launch
            walletRepo.settleTournamentMatch(
                userId = uid,
                gameId = gameId,
                entryFee = entryFee,
                outcome = outcome,
                explicitWinPrize = explicitWinPrize
            )
                .onSuccess { receipt ->
                    _lastSettlement.value = receipt
                    launch {
                        multiplayerRepo.syncWalletSummaryToCloud(receipt.updatedWallet)
                        if (receipt.creditedAmount > 0.0 && receipt.creditedWalletBucket != "NONE") {
                            multiplayerRepo.appendCloudWalletLedgerEntry(
                                walletBucket = receipt.creditedWalletBucket,
                                kind = when (outcome) {
                                    GameMatchResult.WIN -> "TOURNAMENT_WIN_PRIZE"
                                    GameMatchResult.LOSS -> "TOURNAMENT_LOSS_CASHBACK"
                                    GameMatchResult.DRAW -> "TOURNAMENT_DRAW_REFUND"
                                },
                                amountDelta = receipt.creditedAmount,
                                note = "$gameId Tournament ${outcome.name}"
                            )
                        }
                        repository.triggerAdminMasterDriveAutoSync()
                    }
                }
                .onFailure { err ->
                    _feedbackMessage.value = err.message
                }
        }
    }

    // ---- Game Armory Shop (Game Wallet Coins ONLY) ----

    fun buyArmoryItem(itemId: String) {
        viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) return@launch
            walletRepo.purchaseArmoryItem(uid, itemId)
                .onSuccess { updated ->
                    val item = PlayArmoryCatalog.itemById(itemId)
                    launch {
                        multiplayerRepo.syncWalletSummaryToCloud(updated)
                        if (item != null && item.priceGameCoins > 0.0) {
                            multiplayerRepo.appendCloudWalletLedgerEntry(
                                walletBucket = "GAME",
                                kind = "ARMORY_PURCHASE",
                                amountDelta = -item.priceGameCoins,
                                note = "Bought ${item.titleEn}"
                            )
                        }
                    }
                    _feedbackMessage.value = "Purchased ${item?.titleEn ?: itemId}!"
                }
                .onFailure { err ->
                    _feedbackMessage.value = err.message ?: "Not enough Game Wallet coins"
                }
        }
    }

    fun equipArmoryItem(itemId: String) {
        viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) return@launch
            walletRepo.equipArmoryItem(uid, itemId)
                .onSuccess { updated ->
                    launch { multiplayerRepo.syncWalletSummaryToCloud(updated) }
                }
                .onFailure { err ->
                    _feedbackMessage.value = err.message
                }
        }
    }

    fun consumeSkill(skillId: String, onConsumed: () -> Unit) {
        viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) return@launch
            walletRepo.consumeSkillCharge(uid, skillId)
                .onSuccess {
                    onConsumed()
                }
                .onFailure { err ->
                    _feedbackMessage.value = err.message ?: "No skill charges remaining"
                }
        }
    }

    fun recordWarLandMatchResult(kills: Int, rank: Int, onComplete: (Double) -> Unit = {}) {
        viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) return@launch
            walletRepo.recordWarLandMatchReward(uid, kills, rank)
                .onSuccess { updated ->
                    val rankBonus = when (rank) {
                        1 -> 50.0
                        2 -> 30.0
                        3 -> 20.0
                        in 4..10 -> 10.0
                        else -> 5.0
                    }
                    val totalWon = rankBonus + (kills * 5.0).coerceAtLeast(0.0)
                    launch {
                        multiplayerRepo.syncWalletSummaryToCloud(updated)
                        multiplayerRepo.appendCloudWalletLedgerEntry(
                            walletBucket = "GAME",
                            kind = "MATCH_WIN",
                            amountDelta = totalWon,
                            note = "War Land 3D Battle Royale: Rank #$rank, $kills Kills"
                        )
                    }
                    _feedbackMessage.value = "🏆 War Land match finished! Earned +$totalWon Game Coins!"
                    onComplete(totalWon)
                }
                .onFailure { err ->
                    _feedbackMessage.value = err.message ?: "Failed to record match reward"
                }
        }
    }

    fun useNameChangeCard(newNickname: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) {
                onComplete(false, "User not signed in")
                return@launch
            }
            val trimmed = newNickname.trim()
            if (trimmed.length < 3) {
                onComplete(false, "Nickname must be at least 3 characters")
                return@launch
            }
            walletRepo.consumeSkillCharge(uid, PlayArmoryCatalog.CARD_NAME_CHANGE)
                .onSuccess {
                    runCatching {
                        val db = com.example.data.local.AppDatabase.getInstance(appContext)
                        val user = db.userDao().getById(uid)
                        if (user != null) {
                            db.userDao().insertReplace(user.copy(displayName = trimmed))
                        }
                    }
                    _feedbackMessage.value = "Player name changed to $trimmed!"
                    onComplete(true, "Name changed successfully")
                }
                .onFailure { err ->
                    onComplete(false, err.message ?: "You need a Name Change Card from Armory!")
                }
        }
    }

    // ---- Online Multiplayer Room Actions ----

    fun createOnlineRoom(gameType: String, publicRoom: Boolean, entryFee: Double) {
        viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) return@launch
            _isBusy.value = true
            try {
                val botCfg = walletRepo.getBotConfigOnce(gameType)
                val seatStatus = multiplayerRepo.fetchLiveSeatAvailability(
                    gameType = gameType,
                    maxConcurrentSeats = botCfg.maxConcurrentSeats,
                    multiplayerEnabled = botCfg.multiplayerEnabled
                )
                if (seatStatus.isServerBusy) {
                    _feedbackMessage.value = "Server busy (${seatStatus.waitingRoomsCount}/${seatStatus.maxConcurrentSeats} seats filled). Please wait!"
                    return@launch
                }
                if (entryFee > 0.0) {
                    val entryRes = walletRepo.enterTournamentMatch(uid, gameType, entryFee)
                    if (entryRes.isFailure) {
                        _feedbackMessage.value = entryRes.exceptionOrNull()?.message
                            ?: "Insufficient Tournament Wallet balance"
                        return@launch
                    }
                }
                multiplayerRepo.createRoom(
                    gameType = gameType,
                    hostDisplayLabel = decoratedPlayerName(),
                    publicRoom = publicRoom,
                    entryFee = entryFee
                ).onSuccess { room ->
                    _activeRoomCode.value = room.roomCode
                }.onFailure { err ->
                    if (entryFee > 0.0) {
                        walletRepo.settleTournamentMatch(uid, gameType, entryFee, GameMatchResult.DRAW)
                    }
                    _feedbackMessage.value = err.message ?: "Could not create room"
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun joinOnlineRoom(code: String) {
        viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) return@launch
            _isBusy.value = true
            try {
                multiplayerRepo.joinRoomByCode(code, decoratedPlayerName())
                    .onSuccess { room ->
                        if (room.entryFee > 0.0 && room.guestUid == _onlineAuth.value?.uid) {
                            val entryRes = walletRepo.enterTournamentMatch(uid, room.gameType, room.entryFee)
                            if (entryRes.isFailure) {
                                _feedbackMessage.value = entryRes.exceptionOrNull()?.message
                                    ?: "Joined room (insufficient Tournament balance for entry fee)"
                            }
                        }
                        _activeRoomCode.value = room.roomCode
                    }
                    .onFailure { err ->
                        _feedbackMessage.value = err.message ?: "Could not join room"
                    }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun startQuickMatch(gameType: String, entryFee: Double) {
        viewModelScope.launch {
            val uid = currentLocalUserId()
            if (uid <= 0L) return@launch
            _isBusy.value = true
            try {
                val botCfg = walletRepo.getBotConfigOnce(gameType)
                val seatStatus = multiplayerRepo.fetchLiveSeatAvailability(
                    gameType = gameType,
                    maxConcurrentSeats = botCfg.maxConcurrentSeats,
                    multiplayerEnabled = botCfg.multiplayerEnabled
                )
                if (seatStatus.isServerBusy) {
                    _feedbackMessage.value = "Server busy (${seatStatus.waitingRoomsCount}/${seatStatus.maxConcurrentSeats} seats full). Wait for a match to finish!"
                    return@launch
                }
                if (entryFee > 0.0) {
                    val entryRes = walletRepo.enterTournamentMatch(uid, gameType, entryFee)
                    if (entryRes.isFailure) {
                        _feedbackMessage.value = entryRes.exceptionOrNull()?.message
                            ?: "Insufficient Tournament Wallet balance"
                        return@launch
                    }
                }
                multiplayerRepo.findOrCreateQuickMatch(
                    gameType = gameType,
                    playerDisplayLabel = decoratedPlayerName(),
                    entryFee = entryFee
                ).onSuccess { room ->
                    _activeRoomCode.value = room.roomCode
                }.onFailure { err ->
                    if (entryFee > 0.0) {
                        walletRepo.settleTournamentMatch(uid, gameType, entryFee, GameMatchResult.DRAW)
                    }
                    _feedbackMessage.value = err.message ?: "Quick match failed"
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun reopenRoom(roomCode: String) {
        _activeRoomCode.value = roomCode
    }

    fun submitOnlineMove(
        roomCode: String,
        nextStatePayload: String,
        nextTurnUid: String,
        nextMoveCount: Int,
        lastMoveSummary: String,
        winnerUid: String = "",
        finished: Boolean = false
    ) {
        viewModelScope.launch {
            multiplayerRepo.submitMove(
                roomCode = roomCode,
                nextStatePayload = nextStatePayload,
                nextTurnUid = nextTurnUid,
                nextMoveCount = nextMoveCount,
                lastMoveSummary = lastMoveSummary,
                winnerUid = winnerUid,
                finished = finished
            ).onFailure { err ->
                _feedbackMessage.value = err.message ?: "Failed to sync move"
            }
        }
    }

    fun leaveOrForfeitActiveRoom() {
        val room = activeRoom.value
        _activeRoomCode.value = null
        if (room == null) return
        if (room.status == "FINISHED" || room.status == "CANCELLED") return
        viewModelScope.launch {
            val uid = currentLocalUserId()
            multiplayerRepo.cancelOrForfeitRoom(room)
            if (room.status == "WAITING" && room.entryFee > 0.0 && uid > 0L) {
                walletRepo.settleTournamentMatch(uid, room.gameType, room.entryFee, GameMatchResult.DRAW)
            } else if (room.status == "PLAYING" && room.entryFee > 0.0 && uid > 0L) {
                walletRepo.settleTournamentMatch(uid, room.gameType, room.entryFee, GameMatchResult.LOSS)
            }
        }
    }
}

class PlayEarnViewModelFactory(
    private val context: Context,
    private val repository: FinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlayEarnViewModel(context.applicationContext, repository) as T
    }
}
