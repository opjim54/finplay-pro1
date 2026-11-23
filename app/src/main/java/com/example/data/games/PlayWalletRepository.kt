package com.example.data.games

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.AdminEarningEntity
import com.example.data.local.AppConfigEntity
import com.example.data.local.AppDatabase
import com.example.data.local.BroadcastNotificationEntity
import com.example.data.local.ExternalTournamentEntity
import com.example.data.local.ExternalTournamentEntryEntity
import com.example.data.local.GameBotConfigEntity
import com.example.data.local.GameMatchResult
import com.example.data.local.NotificationEntity
import com.example.data.local.PlayWalletEntity
import com.example.data.local.PlayWalletLedgerEntity
import com.example.data.local.PlayWalletRequestEntity
import com.example.data.local.TournamentTierConfigEntity
import com.example.data.local.WalletLedgerKind
import com.example.data.local.WalletPaymentChannel
import com.example.data.local.WalletRequestStatus
import com.example.data.local.WalletRequestType
import com.example.domain.NotificationHelper
import com.example.domain.games.ArmoryItemCategory
import com.example.domain.games.PlayArmoryCatalog
import com.example.domain.games.PlayPaymentConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TournamentSettlementReceipt(
    val gameId: String,
    val outcome: GameMatchResult,
    val entryFee: Double,
    val creditedAmount: Double,
    val creditedWalletBucket: String, // "WINNING", "TOURNAMENT", "GAME", or "NONE"
    val updatedWallet: PlayWalletEntity
)

class PlayWalletRepository(private val db: AppDatabase) {

    companion object {
        const val KEY_BKASH = "play_bkash_number"
        const val KEY_NAGAD = "play_nagad_number"
        const val KEY_ROCKET = "play_rocket_number"
        const val KEY_BINANCE_PAY_ID = "play_binance_pay_id"
        const val KEY_BINANCE_TRC20 = "play_binance_trc20"
        const val KEY_BINANCE_RATE = "play_binance_rate_bdt"
        const val KEY_MIN_DEPOSIT = "play_min_deposit"
        const val KEY_MIN_WITHDRAW = "play_min_withdraw"

        const val KEY_DEP_BKASH = "play_dep_bkash_enabled"
        const val KEY_DEP_NAGAD = "play_dep_nagad_enabled"
        const val KEY_DEP_ROCKET = "play_dep_rocket_enabled"
        const val KEY_DEP_BINANCE = "play_dep_binance_enabled"

        const val KEY_WDR_BKASH = "play_wdr_bkash_enabled"
        const val KEY_WDR_NAGAD = "play_wdr_nagad_enabled"
        const val KEY_WDR_ROCKET = "play_wdr_rocket_enabled"
        const val KEY_WDR_BINANCE = "play_wdr_binance_enabled"

        val ALL_INBUILT_GAME_IDS = listOf("LUDO", "SNAKES_LADDERS", "CARROM", "CHESS", "FRUIT_SLASH", "CONNECT_FOUR", "REVERSI", "AETHER_CROWN")

        const val WALLET_HISTORY_RETENTION_DAYS = 30L
        const val WALLET_HISTORY_RETENTION_MS = WALLET_HISTORY_RETENTION_DAYS * 24L * 60L * 60L * 1000L
    }

    /**
     * Enforces the strict 1-month (30-day) rolling retention cap on wallet transaction history:
     * permanently deletes wallet ledger rows and resolved deposit/withdraw requests older than 30 days
     * from the oldest end while keeping fresh history (and any unreviewed PENDING money requests) intact.
     */
    suspend fun pruneOldWalletHistory(nowMillis: Long = System.currentTimeMillis()): Pair<Int, Int> {
        val cutoff = (nowMillis - WALLET_HISTORY_RETENTION_MS).coerceAtLeast(0L)
        val prunedLedger = db.playWalletLedgerDao().deleteOlderThan(cutoff)
        val prunedRequests = db.playWalletRequestDao().deleteResolvedOlderThan(cutoff)
        return prunedLedger to prunedRequests
    }

    suspend fun ensureDefaultGameConfigs() {
        pruneOldWalletHistory()
        val missingTierDefaults = mutableListOf<TournamentTierConfigEntity>()
        ALL_INBUILT_GAME_IDS.forEach { gameId ->
            if (db.tournamentTierConfigDao().getByGameOnce(gameId).isEmpty()) {
                missingTierDefaults += listOf(
                    TournamentTierConfigEntity(gameId = gameId, tierLabel = "Practice", entryFee = 0.0, winPrize = 0.0, isEnabled = true, sortOrder = 0),
                    TournamentTierConfigEntity(gameId = gameId, tierLabel = "Bronze", entryFee = 20.0, winPrize = 36.0, isEnabled = true, sortOrder = 1),
                    TournamentTierConfigEntity(gameId = gameId, tierLabel = "Silver", entryFee = 50.0, winPrize = 90.0, isEnabled = true, sortOrder = 2),
                    TournamentTierConfigEntity(gameId = gameId, tierLabel = "Gold", entryFee = 100.0, winPrize = 180.0, isEnabled = true, sortOrder = 3),
                    TournamentTierConfigEntity(gameId = gameId, tierLabel = "Diamond", entryFee = 250.0, winPrize = 450.0, isEnabled = true, sortOrder = 4)
                )
            }
        }
        if (missingTierDefaults.isNotEmpty()) {
            db.tournamentTierConfigDao().insertAll(missingTierDefaults)
        }

        val now = System.currentTimeMillis()
        val missingBots = mutableListOf<GameBotConfigEntity>()
        ALL_INBUILT_GAME_IDS.forEach { gameId ->
            if (db.gameBotConfigDao().getByGameOnce(gameId) == null) {
                missingBots += GameBotConfigEntity(
                    gameId = gameId,
                    botAccuracyPercent = when (gameId) {
                        "CHESS" -> 70
                        "AETHER_CROWN" -> 65
                        "CARROM" -> 75
                        else -> 60
                    },
                    maxConcurrentSeats = 10,
                    multiplayerEnabled = true,
                    updatedAt = now
                )
            }
        }
        if (missingBots.isNotEmpty()) {
            db.gameBotConfigDao().insertAll(missingBots)
        }
    }

    fun observeWallet(userId: Long): Flow<PlayWalletEntity> =
        db.playWalletDao().observeByUser(userId).map { existing ->
            existing ?: PlayWalletEntity(ownerUserId = userId)
        }

    fun observeAllWallets(): Flow<List<PlayWalletEntity>> =
        db.playWalletDao().observeAll()

    suspend fun getWalletOnce(userId: Long): PlayWalletEntity =
        db.playWalletDao().getByUserOnce(userId) ?: PlayWalletEntity(ownerUserId = userId).also {
            db.playWalletDao().upsert(it)
        }

    fun observeUserRequests(userId: Long): Flow<List<PlayWalletRequestEntity>> =
        db.playWalletRequestDao().observeByUser(userId).map { list ->
            val cutoff = System.currentTimeMillis() - WALLET_HISTORY_RETENTION_MS
            list.filter { it.createdAt >= cutoff || it.status == WalletRequestStatus.PENDING }
        }

    fun observeAllRequests(): Flow<List<PlayWalletRequestEntity>> =
        db.playWalletRequestDao().observeAll().map { list ->
            val cutoff = System.currentTimeMillis() - WALLET_HISTORY_RETENTION_MS
            list.filter { it.createdAt >= cutoff || it.status == WalletRequestStatus.PENDING }
        }

    fun observeLedger(userId: Long): Flow<List<PlayWalletLedgerEntity>> =
        db.playWalletLedgerDao().observeByUser(userId).map { list ->
            val cutoff = System.currentTimeMillis() - WALLET_HISTORY_RETENTION_MS
            list.filter { it.createdAt >= cutoff }
        }

    fun observeAllTiers(): Flow<List<TournamentTierConfigEntity>> =
        db.tournamentTierConfigDao().observeAll()

    fun observeTiersForGame(gameId: String): Flow<List<TournamentTierConfigEntity>> =
        db.tournamentTierConfigDao().observeByGame(gameId)

    fun observeAllBotConfigs(): Flow<List<GameBotConfigEntity>> =
        db.gameBotConfigDao().observeAll()

    suspend fun getBotConfigOnce(gameId: String): GameBotConfigEntity =
        db.gameBotConfigDao().getByGameOnce(gameId)
            ?: GameBotConfigEntity(gameId = gameId, botAccuracyPercent = 65, maxConcurrentSeats = 10)

    fun observeExternalTournaments(): Flow<List<ExternalTournamentEntity>> =
        db.externalTournamentDao().observeAll()

    fun observeUserExternalEntries(userId: Long): Flow<List<ExternalTournamentEntryEntity>> =
        db.externalTournamentEntryDao().observeByUser(userId)

    fun observeAllExternalEntries(): Flow<List<ExternalTournamentEntryEntity>> =
        db.externalTournamentEntryDao().observeAll()

    fun observeEntriesForTournament(tournamentId: Long): Flow<List<ExternalTournamentEntryEntity>> =
        db.externalTournamentEntryDao().observeByTournament(tournamentId)

    fun observeBroadcastNotifications(): Flow<List<BroadcastNotificationEntity>> =
        db.broadcastNotificationDao().observeAll()

    fun observeAdminEarnings(): Flow<List<AdminEarningEntity>> =
        db.adminEarningDao().observeAll()

    private fun parsePaymentConfig(map: Map<String, String>): PlayPaymentConfig {
        fun flag(key: String): Boolean = map[key]?.lowercase() != "false"
        return PlayPaymentConfig(
            bkashNumber = map[KEY_BKASH]?.takeIf { it.isNotBlank() } ?: "01700-000000",
            nagadNumber = map[KEY_NAGAD]?.takeIf { it.isNotBlank() } ?: "01800-000000",
            rocketNumber = map[KEY_ROCKET]?.takeIf { it.isNotBlank() } ?: "01900-000000",
            binancePayId = map[KEY_BINANCE_PAY_ID]?.takeIf { it.isNotBlank() } ?: "FINPLAY-PAY-8899",
            binanceTrc20Address = map[KEY_BINANCE_TRC20]?.takeIf { it.isNotBlank() } ?: "TFinPlayProUSDTTRC20Wallet",
            binanceRateBdt = map[KEY_BINANCE_RATE]?.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 120.0,
            minDeposit = map[KEY_MIN_DEPOSIT]?.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 20.0,
            minWithdraw = map[KEY_MIN_WITHDRAW]?.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 50.0,
            bkashDepositEnabled = flag(KEY_DEP_BKASH),
            nagadDepositEnabled = flag(KEY_DEP_NAGAD),
            rocketDepositEnabled = flag(KEY_DEP_ROCKET),
            binanceDepositEnabled = flag(KEY_DEP_BINANCE),
            bkashWithdrawEnabled = flag(KEY_WDR_BKASH),
            nagadWithdrawEnabled = flag(KEY_WDR_NAGAD),
            rocketWithdrawEnabled = flag(KEY_WDR_ROCKET),
            binanceWithdrawEnabled = flag(KEY_WDR_BINANCE)
        )
    }

    fun observePaymentConfig(): Flow<PlayPaymentConfig> =
        db.appConfigDao().observeAll().map { configs ->
            parsePaymentConfig(configs.associate { it.key to it.value })
        }

    suspend fun getPaymentConfigOnce(): PlayPaymentConfig =
        parsePaymentConfig(db.appConfigDao().getAllOnce().associate { it.key to it.value })

    suspend fun submitDepositRequest(
        userId: Long,
        channel: WalletPaymentChannel,
        amount: Double,
        senderNumber: String,
        transactionId: String
    ): Result<PlayWalletRequestEntity> = runCatching {
        val cfg = getPaymentConfigOnce()
        require(cfg.isDepositEnabled(channel)) { "${channel.name} deposit is currently turned OFF by Admin" }
        val cleanPhone = senderNumber.trim()
        val cleanTrx = transactionId.trim()
        require(amount >= cfg.minDeposit) { "Minimum deposit is ${cfg.minDeposit.toInt()}" }
        require(cleanPhone.length in 4..64) {
            if (channel == WalletPaymentChannel.BINANCE) "Enter a valid Binance Pay ID or TRC20 address"
            else "Enter a valid sender account number"
        }
        require(cleanTrx.length in 4..64) { "Enter a valid Transaction ID / Order ID" }

        val user = db.userDao().getById(userId)
        val now = System.currentTimeMillis()
        val entity = PlayWalletRequestEntity(
            ownerUserId = userId,
            usernameSnapshot = user?.username ?: "user_$userId",
            requestType = WalletRequestType.DEPOSIT,
            channel = channel,
            amount = amount,
            accountNumber = cleanPhone,
            transactionId = cleanTrx,
            status = WalletRequestStatus.PENDING,
            createdAt = now,
            updatedAt = now
        )
        val id = db.playWalletRequestDao().insert(entity)
        pruneOldWalletHistory(now)
        entity.copy(id = id)
    }

    /**
     * Withdrawals STRICTLY draw ONLY from Winning Coins Balance (`winningBalance`).
     * Deposited Tournament Wallet funds & Game Wallet coins cannot be directly withdrawn.
     * Fires a live Telegram Bot alert upon successful submission.
     */
    suspend fun submitWithdrawRequest(
        userId: Long,
        channel: WalletPaymentChannel,
        amount: Double,
        receiverNumber: String
    ): Result<PlayWalletRequestEntity> = runCatching {
        val cfg = getPaymentConfigOnce()
        require(cfg.isWithdrawEnabled(channel)) { "${channel.name} withdrawal is currently turned OFF by Admin" }
        val cleanPhone = receiverNumber.trim()
        require(amount >= cfg.minWithdraw) { "Minimum withdrawal is ${cfg.minWithdraw.toInt()}" }
        require(cleanPhone.length in 4..64) {
            if (channel == WalletPaymentChannel.BINANCE) "Enter a valid Binance Pay ID or USDT TRC20 address"
            else "Enter a valid receiver account number"
        }

        var remainingWinning = 0.0
        val created = db.withTransaction {
            val wallet = getWalletOnce(userId)
            require(wallet.winningBalance >= amount) {
                "Insufficient Winning Coins! Only Winning Balance (৳${wallet.winningBalance.toInt()}) can be withdrawn."
            }
            val now = System.currentTimeMillis()
            remainingWinning = (wallet.winningBalance - amount).coerceAtLeast(0.0)
            val updatedWallet = wallet.copy(
                winningBalance = remainingWinning,
                updatedAt = now
            )
            db.playWalletDao().upsert(updatedWallet)
            db.playWalletLedgerDao().insert(
                PlayWalletLedgerEntity(
                    ownerUserId = userId,
                    walletBucket = "WINNING",
                    kind = WalletLedgerKind.WITHDRAW_REQUESTED,
                    amountDelta = -amount,
                    note = "${channel.name} winning withdraw to $cleanPhone",
                    createdAt = now
                )
            )
            val user = db.userDao().getById(userId)
            val req = PlayWalletRequestEntity(
                ownerUserId = userId,
                usernameSnapshot = user?.username ?: "user_$userId",
                requestType = WalletRequestType.WITHDRAW,
                channel = channel,
                amount = amount,
                accountNumber = cleanPhone,
                status = WalletRequestStatus.PENDING,
                createdAt = now,
                updatedAt = now
            )
            val id = db.playWalletRequestDao().insert(req)
            req.copy(id = id)
        }

        // Fire real-time Telegram Bot alert outside DB transaction (best-effort network call)
        runCatching {
            TelegramAlertService.sendWithdrawalAlert(
                requestId = created.id,
                username = created.usernameSnapshot,
                amount = created.amount,
                channel = created.channel,
                receiverNumber = created.accountNumber,
                remainingWinningBalance = remainingWinning
            )
        }
        pruneOldWalletHistory()
        created
    }

    /**
     * Strictly ONE-WAY transfer: Tournament Wallet -> Game Wallet.
     * Reverse transfers (Game Wallet -> Tournament Wallet or Winning) are prohibited by design.
     */
    suspend fun transferTournamentToGameWallet(
        userId: Long,
        amount: Double
    ): Result<PlayWalletEntity> = runCatching {
        require(amount > 0.0) { "Transfer amount must be positive" }
        db.withTransaction {
            val wallet = getWalletOnce(userId)
            require(wallet.tournamentBalance >= amount) { "Insufficient Tournament Wallet balance" }
            val now = System.currentTimeMillis()
            val updated = wallet.copy(
                tournamentBalance = (wallet.tournamentBalance - amount).coerceAtLeast(0.0),
                gameBalance = wallet.gameBalance + amount,
                updatedAt = now
            )
            db.playWalletDao().upsert(updated)
            db.playWalletLedgerDao().insert(
                PlayWalletLedgerEntity(
                    ownerUserId = userId,
                    walletBucket = "TOURNAMENT",
                    kind = WalletLedgerKind.TRANSFER_TO_GAME_WALLET,
                    amountDelta = -amount,
                    note = "Transferred to Game Wallet",
                    createdAt = now
                )
            )
            db.playWalletLedgerDao().insert(
                PlayWalletLedgerEntity(
                    ownerUserId = userId,
                    walletBucket = "GAME",
                    kind = WalletLedgerKind.TRANSFER_TO_GAME_WALLET,
                    amountDelta = amount,
                    note = "Received from Tournament Wallet",
                    createdAt = now
                )
            )
            updated
        }.also { pruneOldWalletHistory() }
    }

    suspend fun enterTournamentMatch(
        userId: Long,
        gameId: String,
        entryFee: Double
    ): Result<PlayWalletEntity> = runCatching {
        if (entryFee <= 0.0) return@runCatching getWalletOnce(userId)
        db.withTransaction {
            val wallet = getWalletOnce(userId)
            val totalPlayable = wallet.tournamentBalance + wallet.winningBalance
            require(totalPlayable >= entryFee) {
                "Insufficient Tournament/Winning balance for ৳${entryFee.toInt()} entry"
            }
            val now = System.currentTimeMillis()
            val fromTournament = minOf(wallet.tournamentBalance, entryFee)
            val fromWinning = (entryFee - fromTournament).coerceAtLeast(0.0)
            val updated = wallet.copy(
                tournamentBalance = (wallet.tournamentBalance - fromTournament).coerceAtLeast(0.0),
                winningBalance = (wallet.winningBalance - fromWinning).coerceAtLeast(0.0),
                updatedAt = now
            )
            db.playWalletDao().upsert(updated)
            if (fromTournament > 0.0) {
                db.playWalletLedgerDao().insert(
                    PlayWalletLedgerEntity(
                        ownerUserId = userId,
                        walletBucket = "TOURNAMENT",
                        kind = WalletLedgerKind.TOURNAMENT_ENTRY,
                        amountDelta = -fromTournament,
                        note = "$gameId Tournament Entry",
                        createdAt = now
                    )
                )
            }
            if (fromWinning > 0.0) {
                db.playWalletLedgerDao().insert(
                    PlayWalletLedgerEntity(
                        ownerUserId = userId,
                        walletBucket = "WINNING",
                        kind = WalletLedgerKind.TOURNAMENT_ENTRY,
                        amountDelta = -fromWinning,
                        note = "$gameId Tournament Entry (Winning)",
                        createdAt = now
                    )
                )
            }
            updated
        }
    }

    /**
     * Tournament Match Settlement:
     * - WIN  -> Prize credited to WINNING BALANCE (withdrawable!).
     * - LOSS -> 100% of entryFee automatically credited as Cashback into GAME WALLET!
     * - DRAW -> 100% of entryFee refunded to TOURNAMENT WALLET.
     */
    suspend fun settleTournamentMatch(
        userId: Long,
        gameId: String,
        entryFee: Double,
        outcome: GameMatchResult,
        prizeMultiplier: Double = 1.8,
        explicitWinPrize: Double? = null
    ): Result<TournamentSettlementReceipt> = runCatching {
        if (entryFee <= 0.0) {
            return@runCatching TournamentSettlementReceipt(
                gameId = gameId,
                outcome = outcome,
                entryFee = 0.0,
                creditedAmount = 0.0,
                creditedWalletBucket = "NONE",
                updatedWallet = getWalletOnce(userId)
            )
        }
        db.withTransaction {
            val wallet = getWalletOnce(userId)
            val now = System.currentTimeMillis()
            when (outcome) {
                GameMatchResult.WIN -> {
                    val prize = (explicitWinPrize?.takeIf { it > 0.0 } ?: (entryFee * prizeMultiplier))
                    val updated = wallet.copy(
                        winningBalance = wallet.winningBalance + prize,
                        updatedAt = now
                    )
                    db.playWalletDao().upsert(updated)
                    db.playWalletLedgerDao().insert(
                        PlayWalletLedgerEntity(
                            ownerUserId = userId,
                            walletBucket = "WINNING",
                            kind = WalletLedgerKind.TOURNAMENT_WIN_PRIZE,
                            amountDelta = prize,
                            note = "$gameId Tournament Victory Prize (Withdrawable)",
                            createdAt = now
                        )
                    )
                    val grossPool = entryFee * 2.0
                    val commission = (grossPool - prize).coerceAtLeast(0.0)
                    if (commission > 0.0) {
                        db.adminEarningDao().insert(
                            AdminEarningEntity(
                                gameId = gameId.uppercase(),
                                sourceType = "INBUILT_MATCH",
                                referenceTitle = "$gameId Tournament Commission (Entry ৳${entryFee.toInt()} | Prize ৳${prize.toInt()})",
                                grossPool = grossPool,
                                prizePaid = prize,
                                commissionAmount = commission,
                                createdAt = now
                            )
                        )
                    }
                    TournamentSettlementReceipt(gameId, outcome, entryFee, prize, "WINNING", updated)
                }

                GameMatchResult.LOSS -> {
                    // 100% LOSS CASHBACK into Game Wallet!
                    val cashback = entryFee
                    val updated = wallet.copy(
                        gameBalance = wallet.gameBalance + cashback,
                        updatedAt = now
                    )
                    db.playWalletDao().upsert(updated)
                    db.playWalletLedgerDao().insert(
                        PlayWalletLedgerEntity(
                            ownerUserId = userId,
                            walletBucket = "GAME",
                            kind = WalletLedgerKind.TOURNAMENT_LOSS_CASHBACK,
                            amountDelta = cashback,
                            note = "$gameId 100% Loss Cashback to Game Wallet",
                            createdAt = now
                        )
                    )
                    TournamentSettlementReceipt(gameId, outcome, entryFee, cashback, "GAME", updated)
                }

                GameMatchResult.DRAW -> {
                    val updated = wallet.copy(
                        tournamentBalance = wallet.tournamentBalance + entryFee,
                        updatedAt = now
                    )
                    db.playWalletDao().upsert(updated)
                    db.playWalletLedgerDao().insert(
                        PlayWalletLedgerEntity(
                            ownerUserId = userId,
                            walletBucket = "TOURNAMENT",
                            kind = WalletLedgerKind.TOURNAMENT_DRAW_REFUND,
                            amountDelta = entryFee,
                            note = "$gameId Draw Entry Refund",
                            createdAt = now
                        )
                    )
                    TournamentSettlementReceipt(gameId, outcome, entryFee, entryFee, "TOURNAMENT", updated)
                }
            }
        }.also { pruneOldWalletHistory() }
    }

    suspend fun purchaseArmoryItem(userId: Long, itemId: String): Result<PlayWalletEntity> = runCatching {
        val item = PlayArmoryCatalog.itemById(itemId)
            ?: throw IllegalArgumentException("Item not found in Armory")
        db.withTransaction {
            val wallet = getWalletOnce(userId)
            val now = System.currentTimeMillis()
            val owned = PlayArmoryCatalog.ownedItemSet(wallet, now)
            val isExpired = PlayArmoryCatalog.isItemExpired(wallet, item.id, now)

            if (!item.isConsumable && item.id in owned && !isExpired && item.validityDays == null) {
                return@withTransaction equipArmoryItemInternal(wallet, item)
            }
            require(wallet.gameBalance >= item.priceGameCoins) {
                "Not enough Game Wallet coins! Transfer from Tournament Wallet or earn 100% Loss Cashback."
            }
            val newGameBalance = (wallet.gameBalance - item.priceGameCoins).coerceAtLeast(0.0)
            val updatedWallet = if (item.isConsumable) {
                val charges = PlayArmoryCatalog.parseSkillCharges(wallet.skillChargesJson).toMutableMap()
                charges[item.id] = (charges[item.id] ?: 0) + 1
                wallet.copy(
                    gameBalance = newGameBalance,
                    skillChargesJson = PlayArmoryCatalog.serializeSkillCharges(charges),
                    updatedAt = now
                )
            } else {
                val encodedToken = PlayArmoryCatalog.encodeOwnedItemToken(item, now)
                val existingTokens = wallet.ownedItemsCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                val filteredTokens = existingTokens.filterNot { it == item.id || it.startsWith("${item.id}:") }
                val nextTokens = filteredTokens + encodedToken

                val base = wallet.copy(
                    gameBalance = newGameBalance,
                    ownedItemsCsv = nextTokens.joinToString(","),
                    updatedAt = now
                )
                when (item.category) {
                    ArmoryItemCategory.BOARD_SKIN -> base.copy(equippedBoardSkinId = item.id)
                    ArmoryItemCategory.AVATAR -> base.copy(equippedAvatarId = item.id)
                    ArmoryItemCategory.SKILL,
                    ArmoryItemCategory.WAR_CHARACTER,
                    ArmoryItemCategory.WAR_WEAPON_SKIN,
                    ArmoryItemCategory.EMOTE,
                    ArmoryItemCategory.CONSUMABLE_CARD -> base
                }
            }
            db.playWalletDao().upsert(updatedWallet)
            if (item.priceGameCoins > 0.0) {
                db.playWalletLedgerDao().insert(
                    PlayWalletLedgerEntity(
                        ownerUserId = userId,
                        walletBucket = "GAME",
                        kind = WalletLedgerKind.ARMORY_PURCHASE,
                        amountDelta = -item.priceGameCoins,
                        note = "Bought ${item.titleEn}",
                        createdAt = now
                    )
                )
            }
            updatedWallet
        }
    }

    suspend fun recordWarLandMatchReward(userId: Long, kills: Int, rank: Int): Result<PlayWalletEntity> = runCatching {
        db.withTransaction {
            val wallet = getWalletOnce(userId)
            val rankBonus = when (rank) {
                1 -> 50.0
                2 -> 30.0
                3 -> 20.0
                in 4..10 -> 10.0
                else -> 5.0
            }
            val killReward = (kills * 5.0).coerceAtLeast(0.0)
            val totalReward = rankBonus + killReward
            val now = System.currentTimeMillis()
            val newGameBalance = wallet.gameBalance + totalReward
            val updated = wallet.copy(gameBalance = newGameBalance, updatedAt = now)
            db.playWalletDao().upsert(updated)

            db.playWalletLedgerDao().insert(
                PlayWalletLedgerEntity(
                    ownerUserId = userId,
                    walletBucket = "GAME",
                    kind = WalletLedgerKind.MATCH_WIN,
                    amountDelta = totalReward,
                    note = "War Land 3D Battle Royale: Rank #$rank ($rankBonus coins) + $kills Kills ($killReward coins)",
                    createdAt = now
                )
            )
            updated
        }
    }

    suspend fun equipArmoryItem(userId: Long, itemId: String): Result<PlayWalletEntity> = runCatching {
        val item = PlayArmoryCatalog.itemById(itemId)
            ?: throw IllegalArgumentException("Item not found")
        db.withTransaction {
            val wallet = getWalletOnce(userId)
            val owned = PlayArmoryCatalog.ownedItemSet(wallet)
            require(item.priceGameCoins == 0.0 || item.id in owned) { "Unlock this item first" }
            equipArmoryItemInternal(wallet, item)
        }
    }

    private suspend fun equipArmoryItemInternal(
        wallet: PlayWalletEntity,
        item: com.example.domain.games.ArmoryItem
    ): PlayWalletEntity {
        val updated = when (item.category) {
            ArmoryItemCategory.BOARD_SKIN -> wallet.copy(
                equippedBoardSkinId = item.id,
                updatedAt = System.currentTimeMillis()
            )
            ArmoryItemCategory.AVATAR -> wallet.copy(
                equippedAvatarId = item.id,
                updatedAt = System.currentTimeMillis()
            )
            ArmoryItemCategory.SKILL,
            ArmoryItemCategory.WAR_CHARACTER,
            ArmoryItemCategory.WAR_WEAPON_SKIN,
            ArmoryItemCategory.EMOTE,
            ArmoryItemCategory.CONSUMABLE_CARD -> wallet
        }
        db.playWalletDao().upsert(updated)
        return updated
    }

    suspend fun consumeSkillCharge(userId: Long, skillId: String): Result<PlayWalletEntity> = runCatching {
        db.withTransaction {
            val wallet = getWalletOnce(userId)
            val charges = PlayArmoryCatalog.parseSkillCharges(wallet.skillChargesJson).toMutableMap()
            val current = charges[skillId] ?: 0
            require(current > 0) { "No skill charges remaining. Buy more in Armory!" }
            charges[skillId] = current - 1
            val updated = wallet.copy(
                skillChargesJson = PlayArmoryCatalog.serializeSkillCharges(charges),
                updatedAt = System.currentTimeMillis()
            )
            db.playWalletDao().upsert(updated)
            updated
        }
    }

    // ---- External Esports Tournaments (Free Fire / PUBG / Custom) ----

    /**
     * Creating a NEW tournament for [gameCategory] AUTOMATICALLY DELETES all previously
     * COMPLETED tournaments (and their entries) for that same game category so history stays clean!
     */
    suspend fun adminCreateExternalTournament(
        tournament: ExternalTournamentEntity
    ): Result<ExternalTournamentEntity> = runCatching {
        require(tournament.title.isNotBlank()) { "Tournament title is required" }
        require(tournament.maxSlots >= 2) { "Slots must be at least 2" }
        val category = tournament.gameCategory.trim().uppercase().ifBlank { "FREE_FIRE" }
        db.withTransaction {
            val oldCompletedIds = db.externalTournamentDao().getCompletedIdsForCategory(category)
            oldCompletedIds.forEach { oldId ->
                db.externalTournamentEntryDao().deleteByTournament(oldId)
            }
            if (oldCompletedIds.isNotEmpty()) {
                db.externalTournamentDao().deleteCompletedForCategory(category)
            }
            val now = System.currentTimeMillis()
            val clean = tournament.copy(
                gameCategory = category,
                status = if (tournament.status in listOf("UPCOMING", "LIVE")) tournament.status else "UPCOMING",
                createdAt = now,
                updatedAt = now
            )
            val id = db.externalTournamentDao().upsert(clean)
            clean.copy(id = id)
        }
    }

    suspend fun adminUpdateExternalTournament(tournament: ExternalTournamentEntity): Result<Unit> = runCatching {
        db.externalTournamentDao().update(tournament.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun adminCompleteExternalTournament(
        tournamentId: Long,
        winnerUsername: String,
        winnerPrizeAmount: Double
    ): Result<ExternalTournamentEntity> = runCatching {
        db.withTransaction {
            val existing = db.externalTournamentDao().getById(tournamentId)
                ?: throw IllegalArgumentException("Tournament not found")
            val now = System.currentTimeMillis()
            val cleanWinner = winnerUsername.trim()
            val updated = existing.copy(
                status = "COMPLETED",
                winnerName = cleanWinner.ifBlank { existing.winnerName },
                updatedAt = now
            )
            db.externalTournamentDao().update(updated)

            if (cleanWinner.isNotBlank() && winnerPrizeAmount > 0.0) {
                val winnerUser = db.userDao().getByUsername(cleanWinner)
                if (winnerUser != null) {
                    val wallet = getWalletOnce(winnerUser.id)
                    db.playWalletDao().upsert(
                        wallet.copy(
                            winningBalance = wallet.winningBalance + winnerPrizeAmount,
                            updatedAt = now
                        )
                    )
                    db.playWalletLedgerDao().insert(
                        PlayWalletLedgerEntity(
                            ownerUserId = winnerUser.id,
                            walletBucket = "WINNING",
                            kind = WalletLedgerKind.EXTERNAL_TOURNAMENT_PRIZE,
                            amountDelta = winnerPrizeAmount,
                            note = "${existing.title} Winner Prize (Withdrawable)",
                            createdAt = now
                        )
                    )
                }
            }
            updated
        }
    }

    suspend fun adminDeleteExternalTournament(tournamentId: Long) {
        db.withTransaction {
            db.externalTournamentEntryDao().deleteByTournament(tournamentId)
            db.externalTournamentDao().deleteById(tournamentId)
        }
    }

    suspend fun joinExternalTournament(
        userId: Long,
        tournamentId: Long,
        inGameName: String,
        inGameUid: String
    ): Result<ExternalTournamentEntryEntity> = runCatching {
        require(inGameName.trim().length >= 2) { "Enter your in-game name (IGN)" }
        require(inGameUid.trim().length >= 3) { "Enter your in-game UID" }
        db.withTransaction {
            val t = db.externalTournamentDao().getById(tournamentId)
                ?: throw IllegalArgumentException("Tournament not found")
            require(t.status != "COMPLETED") { "This tournament has already ended" }
            require(t.joinedCount < t.maxSlots) {
                "Tournament is FULL (${t.joinedCount}/${t.maxSlots})! Please wait for the next match."
            }
            val already = db.externalTournamentEntryDao().getByTournamentAndUser(tournamentId, userId)
            require(already == null) { "You have already joined this tournament!" }

            if (t.entryFee > 0.0) {
                val wallet = getWalletOnce(userId)
                val totalPlayable = wallet.tournamentBalance + wallet.winningBalance
                require(totalPlayable >= t.entryFee) {
                    "Insufficient balance! Deposit ৳${t.entryFee.toInt()} to Tournament Wallet first."
                }
                val fromTourney = minOf(wallet.tournamentBalance, t.entryFee)
                val fromWin = (t.entryFee - fromTourney).coerceAtLeast(0.0)
                val now = System.currentTimeMillis()
                db.playWalletDao().upsert(
                    wallet.copy(
                        tournamentBalance = (wallet.tournamentBalance - fromTourney).coerceAtLeast(0.0),
                        winningBalance = (wallet.winningBalance - fromWin).coerceAtLeast(0.0),
                        updatedAt = now
                    )
                )
                db.playWalletLedgerDao().insert(
                    PlayWalletLedgerEntity(
                        ownerUserId = userId,
                        walletBucket = if (fromTourney >= fromWin) "TOURNAMENT" else "WINNING",
                        kind = WalletLedgerKind.EXTERNAL_TOURNAMENT_ENTRY,
                        amountDelta = -t.entryFee,
                        note = "Joined ${t.title} (${t.gameCategory})",
                        createdAt = now
                    )
                )
            }

            val user = db.userDao().getById(userId)
            val now = System.currentTimeMillis()
            val entry = ExternalTournamentEntryEntity(
                tournamentId = tournamentId,
                ownerUserId = userId,
                usernameSnapshot = user?.username ?: "user_$userId",
                inGameName = inGameName.trim(),
                inGameUid = inGameUid.trim(),
                entryFeePaid = t.entryFee,
                joinedAt = now
            )
            val id = db.externalTournamentEntryDao().insert(entry)
            db.externalTournamentDao().update(
                t.copy(joinedCount = t.joinedCount + 1, updatedAt = now)
            )
            // Automatically calculate & log Admin Commission into Admin Earnings Ledger
            if (t.entryFee > 0.0) {
                val pct = t.adminCommissionPercent.coerceIn(0.0, 90.0)
                val commission = (t.entryFee * pct) / 100.0
                if (commission > 0.0) {
                    db.adminEarningDao().insert(
                        AdminEarningEntity(
                            gameId = t.gameCategory.uppercase(),
                            sourceType = "EXTERNAL_TOURNAMENT",
                            referenceTitle = "${t.title} — ${pct.toInt()}% Fee (${entry.inGameName})",
                            grossPool = t.entryFee,
                            prizePaid = (t.entryFee - commission).coerceAtLeast(0.0),
                            commissionAmount = commission,
                            createdAt = now
                        )
                    )
                }
            }
            entry.copy(id = id)
        }
    }

    /**
     * Dispatches an urgent Room ID & Password alert to ALL joined players of [tournamentId]
     * (writes into every joined player's notification inbox + fires Android heads-up push notification).
     */
    suspend fun adminDispatchPreMatchRoomAlert(
        tournamentId: Long,
        context: Context? = null
    ): Result<Int> = runCatching {
        val (tournament, notifiedCount) = db.withTransaction {
            val t = db.externalTournamentDao().getById(tournamentId)
                ?: throw IllegalArgumentException("Tournament not found")
            val entries = db.externalTournamentEntryDao().getByTournamentOnce(tournamentId)
            val now = System.currentTimeMillis()
            val roomLine = if (t.roomIdCode.isNotBlank()) {
                "Room ID: ${t.roomIdCode} | Pass: ${t.roomPassword.ifBlank { "None" }} — দ্রুত Free Fire লগইন করে জয়েন করুন!"
            } else {
                "ম্যাচ শুরু হতে চলেছে! দ্রুত Room ID ও Password দেখে Free Fire-এ জয়েন করুন!"
            }
            val uniqueUserIds = entries.map { it.ownerUserId }.distinct()
            if (uniqueUserIds.isNotEmpty()) {
                db.notificationDao().insertAll(
                    uniqueUserIds.map { uid ->
                        NotificationEntity(
                            ownerUserId = uid,
                            type = "TOURNAMENT_ROOM_ALERT",
                            title = "🔥 Room Alert: ${t.title}",
                            message = roomLine,
                            createdAt = now
                        )
                    }
                )
            }
            db.broadcastNotificationDao().insert(
                BroadcastNotificationEntity(
                    title = "🔥 ${t.title} — Room ID & Pass Ready!",
                    message = roomLine,
                    priority = "URGENT",
                    createdAt = now
                )
            )
            db.externalTournamentDao().update(t.copy(roomAlertSent = true, updatedAt = now))
            t to uniqueUserIds.size
        }
        context?.let { ctx ->
            NotificationHelper.postTournamentRoomAlert(
                context = ctx,
                tournamentId = tournament.id,
                tournamentTitle = tournament.title,
                roomId = tournament.roomIdCode,
                roomPassword = tournament.roomPassword
            )
        }
        notifiedCount
    }

    // ---- Admin Coins Adjustment, Tiers, Bot Accuracy & Broadcasts ----

    suspend fun adminAdjustUserWallet(
        userId: Long,
        tournamentDelta: Double,
        winningDelta: Double,
        gameDelta: Double,
        note: String = "Admin balance adjustment"
    ): Result<PlayWalletEntity> = runCatching {
        db.withTransaction {
            val wallet = getWalletOnce(userId)
            val now = System.currentTimeMillis()
            val updated = wallet.copy(
                tournamentBalance = (wallet.tournamentBalance + tournamentDelta).coerceAtLeast(0.0),
                winningBalance = (wallet.winningBalance + winningDelta).coerceAtLeast(0.0),
                gameBalance = (wallet.gameBalance + gameDelta).coerceAtLeast(0.0),
                updatedAt = now
            )
            db.playWalletDao().upsert(updated)
            val cleanNote = note.trim().ifBlank { "Admin adjustment" }
            if (tournamentDelta != 0.0) {
                db.playWalletLedgerDao().insert(
                    PlayWalletLedgerEntity(
                        ownerUserId = userId,
                        walletBucket = "TOURNAMENT",
                        kind = WalletLedgerKind.ADMIN_ADJUSTMENT,
                        amountDelta = tournamentDelta,
                        note = cleanNote,
                        createdAt = now
                    )
                )
            }
            if (winningDelta != 0.0) {
                db.playWalletLedgerDao().insert(
                    PlayWalletLedgerEntity(
                        ownerUserId = userId,
                        walletBucket = "WINNING",
                        kind = WalletLedgerKind.ADMIN_ADJUSTMENT,
                        amountDelta = winningDelta,
                        note = cleanNote,
                        createdAt = now
                    )
                )
            }
            if (gameDelta != 0.0) {
                db.playWalletLedgerDao().insert(
                    PlayWalletLedgerEntity(
                        ownerUserId = userId,
                        walletBucket = "GAME",
                        kind = WalletLedgerKind.ADMIN_ADJUSTMENT,
                        amountDelta = gameDelta,
                        note = cleanNote,
                        createdAt = now
                    )
                )
            }
            updated
        }
    }

    suspend fun adminSaveTournamentTier(tier: TournamentTierConfigEntity): Result<Unit> = runCatching {
        require(tier.entryFee >= 0.0 && tier.winPrize >= 0.0) { "Entry fee & prize must be >= 0" }
        db.tournamentTierConfigDao().upsert(tier)
    }

    suspend fun adminDeleteTournamentTier(tierId: Long) {
        db.tournamentTierConfigDao().deleteById(tierId)
    }

    suspend fun adminSaveGameBotConfig(config: GameBotConfigEntity) {
        db.gameBotConfigDao().upsert(
            config.copy(
                botAccuracyPercent = config.botAccuracyPercent.coerceIn(10, 100),
                maxConcurrentSeats = config.maxConcurrentSeats.coerceIn(1, 500),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun adminSendBroadcastNotification(
        title: String,
        message: String,
        priority: String = "INFO",
        context: Context? = null
    ): Result<BroadcastNotificationEntity> = runCatching {
        require(title.trim().isNotEmpty()) { "Notification title required" }
        require(message.trim().isNotEmpty()) { "Notification message required" }
        val saved = db.withTransaction {
            val now = System.currentTimeMillis()
            val broadcast = BroadcastNotificationEntity(
                title = title.trim(),
                message = message.trim(),
                priority = priority.trim().uppercase().ifBlank { "INFO" },
                createdAt = now
            )
            val id = db.broadcastNotificationDao().insert(broadcast)
            // Fan-out to every registered user's in-app notifications inbox
            val users = db.userDao().getAllUsersOnce()
            if (users.isNotEmpty()) {
                db.notificationDao().insertAll(
                    users.map { u ->
                        NotificationEntity(
                            ownerUserId = u.id,
                            type = "BROADCAST_${broadcast.priority}",
                            title = "[${broadcast.priority}] ${broadcast.title}",
                            message = broadcast.message,
                            createdAt = now
                        )
                    }
                )
            }
            broadcast.copy(id = id)
        }
        context?.let { ctx ->
            NotificationHelper.postBroadcastPushNotification(
                context = ctx,
                broadcastId = saved.id,
                title = saved.title,
                message = saved.message,
                priority = saved.priority
            )
        }
        saved
    }

    suspend fun adminDeleteBroadcastNotification(id: Long) {
        db.broadcastNotificationDao().deleteById(id)
    }

    // ---- Admin Deposit & Withdrawal Approval Actions ----

    suspend fun adminApproveDeposit(requestId: Long, adminNote: String = "Approved"): Result<PlayWalletRequestEntity> = runCatching {
        db.withTransaction {
            val req = db.playWalletRequestDao().getById(requestId)
                ?: throw IllegalArgumentException("Request not found")
            require(req.requestType == WalletRequestType.DEPOSIT && req.status == WalletRequestStatus.PENDING) {
                "Only PENDING deposit requests can be approved"
            }
            val now = System.currentTimeMillis()
            val wallet = getWalletOnce(req.ownerUserId)
            val updatedWallet = wallet.copy(
                tournamentBalance = wallet.tournamentBalance + req.amount,
                updatedAt = now
            )
            db.playWalletDao().upsert(updatedWallet)
            db.playWalletLedgerDao().insert(
                PlayWalletLedgerEntity(
                    ownerUserId = req.ownerUserId,
                    walletBucket = "TOURNAMENT",
                    kind = WalletLedgerKind.DEPOSIT_APPROVED,
                    amountDelta = req.amount,
                    note = "${req.channel.name} deposit approved (Trx: ${req.transactionId})",
                    createdAt = now
                )
            )
            val updatedReq = req.copy(
                status = WalletRequestStatus.APPROVED,
                adminNote = adminNote.trim(),
                updatedAt = now
            )
            db.playWalletRequestDao().update(updatedReq)
            updatedReq
        }.also { pruneOldWalletHistory() }
    }

    suspend fun adminRejectDeposit(requestId: Long, adminNote: String = "Rejected"): Result<PlayWalletRequestEntity> = runCatching {
        db.withTransaction {
            val req = db.playWalletRequestDao().getById(requestId)
                ?: throw IllegalArgumentException("Request not found")
            require(req.requestType == WalletRequestType.DEPOSIT && req.status == WalletRequestStatus.PENDING) {
                "Only PENDING deposit requests can be rejected"
            }
            val updatedReq = req.copy(
                status = WalletRequestStatus.REJECTED,
                adminNote = adminNote.trim(),
                updatedAt = System.currentTimeMillis()
            )
            db.playWalletRequestDao().update(updatedReq)
            updatedReq
        }
    }

    suspend fun adminMarkWithdrawalPaid(requestId: Long, adminNote: String = "Paid"): Result<PlayWalletRequestEntity> = runCatching {
        db.withTransaction {
            val req = db.playWalletRequestDao().getById(requestId)
                ?: throw IllegalArgumentException("Request not found")
            require(req.requestType == WalletRequestType.WITHDRAW && req.status == WalletRequestStatus.PENDING) {
                "Only PENDING withdrawal requests can be marked paid"
            }
            val updatedReq = req.copy(
                status = WalletRequestStatus.PAID,
                adminNote = adminNote.trim(),
                updatedAt = System.currentTimeMillis()
            )
            db.playWalletRequestDao().update(updatedReq)
            updatedReq
        }.also { pruneOldWalletHistory() }
    }

    suspend fun adminRejectWithdrawal(requestId: Long, adminNote: String = "Refunded"): Result<PlayWalletRequestEntity> = runCatching {
        db.withTransaction {
            val req = db.playWalletRequestDao().getById(requestId)
                ?: throw IllegalArgumentException("Request not found")
            require(req.requestType == WalletRequestType.WITHDRAW && req.status == WalletRequestStatus.PENDING) {
                "Only PENDING withdrawal requests can be rejected"
            }
            val now = System.currentTimeMillis()
            val wallet = getWalletOnce(req.ownerUserId)
            val refundedWallet = wallet.copy(
                winningBalance = wallet.winningBalance + req.amount,
                updatedAt = now
            )
            db.playWalletDao().upsert(refundedWallet)
            db.playWalletLedgerDao().insert(
                PlayWalletLedgerEntity(
                    ownerUserId = req.ownerUserId,
                    walletBucket = "WINNING",
                    kind = WalletLedgerKind.WITHDRAW_REFUNDED,
                    amountDelta = req.amount,
                    note = "${req.channel.name} withdrawal rejected & refunded to Winning Balance",
                    createdAt = now
                )
            )
            val updatedReq = req.copy(
                status = WalletRequestStatus.REJECTED,
                adminNote = adminNote.trim(),
                updatedAt = now
            )
            db.playWalletRequestDao().update(updatedReq)
            updatedReq
        }
    }

    suspend fun adminSavePaymentConfig(config: PlayPaymentConfig) {
        db.withTransaction {
            db.appConfigDao().set(AppConfigEntity(KEY_BKASH, config.bkashNumber.trim()))
            db.appConfigDao().set(AppConfigEntity(KEY_NAGAD, config.nagadNumber.trim()))
            db.appConfigDao().set(AppConfigEntity(KEY_ROCKET, config.rocketNumber.trim()))
            db.appConfigDao().set(AppConfigEntity(KEY_BINANCE_PAY_ID, config.binancePayId.trim()))
            db.appConfigDao().set(AppConfigEntity(KEY_BINANCE_TRC20, config.binanceTrc20Address.trim()))
            db.appConfigDao().set(AppConfigEntity(KEY_BINANCE_RATE, config.binanceRateBdt.coerceAtLeast(1.0).toInt().toString()))
            db.appConfigDao().set(AppConfigEntity(KEY_MIN_DEPOSIT, config.minDeposit.coerceAtLeast(1.0).toInt().toString()))
            db.appConfigDao().set(AppConfigEntity(KEY_MIN_WITHDRAW, config.minWithdraw.coerceAtLeast(1.0).toInt().toString()))

            db.appConfigDao().set(AppConfigEntity(KEY_DEP_BKASH, config.bkashDepositEnabled.toString()))
            db.appConfigDao().set(AppConfigEntity(KEY_DEP_NAGAD, config.nagadDepositEnabled.toString()))
            db.appConfigDao().set(AppConfigEntity(KEY_DEP_ROCKET, config.rocketDepositEnabled.toString()))
            db.appConfigDao().set(AppConfigEntity(KEY_DEP_BINANCE, config.binanceDepositEnabled.toString()))

            db.appConfigDao().set(AppConfigEntity(KEY_WDR_BKASH, config.bkashWithdrawEnabled.toString()))
            db.appConfigDao().set(AppConfigEntity(KEY_WDR_NAGAD, config.nagadWithdrawEnabled.toString()))
            db.appConfigDao().set(AppConfigEntity(KEY_WDR_ROCKET, config.rocketWithdrawEnabled.toString()))
            db.appConfigDao().set(AppConfigEntity(KEY_WDR_BINANCE, config.binanceWithdrawEnabled.toString()))
        }
    }
}
