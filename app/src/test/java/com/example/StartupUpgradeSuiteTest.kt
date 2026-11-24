package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.AiProviderEntity
import com.example.data.local.AiProviderKind
import com.example.data.local.AppDatabase
import com.example.data.local.GameLeaderboardEntity
import com.example.data.local.GameMatchResult
import com.example.data.local.KeepNoteEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.games.PlayWalletRepository
import com.example.data.local.WalletPaymentChannel
import com.example.data.local.WalletRequestStatus
import com.example.data.local.allModelsList
import com.example.data.local.winRatePercent
import com.example.data.repository.FinanceRepository
import com.example.domain.AnalyticsEngine
import com.example.domain.DateTimeUtil
import com.example.domain.StylishTextEngine
import com.example.domain.games.OnlineMatchSerializer
import com.example.domain.games.PlayArmoryCatalog
import com.example.domain.games.CarromEngine
import com.example.domain.games.CarromPhase
import com.example.domain.games.ChessBotAi
import com.example.domain.games.ChessEngine
import com.example.domain.games.ChessGameState
import com.example.domain.games.ChessMove
import com.example.domain.games.ChessOutcome
import com.example.domain.games.ChessPiece
import com.example.domain.games.PieceColor
import com.example.domain.games.PieceType
import com.example.domain.games.LudoBotAi
import com.example.domain.games.LudoColor
import com.example.domain.games.LudoEngine
import com.example.domain.games.LudoPhase
import com.example.domain.games.SholoGutiBotAi
import com.example.domain.games.SholoGutiEngine
import com.example.domain.games.SholoGutiMove
import com.example.domain.games.SholoGutiOutcome
import com.example.domain.games.SholoGutiPlayer
import com.example.domain.games.SholoGutiState
import com.example.domain.games.SnakesLaddersEngine
import com.example.i18n.AppLanguage
import com.example.i18n.LanguageManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StartupUpgradeSuiteTest {

    @After
    fun idleLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `ai provider multi model parser deduplicates and preserves primary model`() {
        val p = AiProviderEntity(
            ownerUserId = 1L,
            name = "Gemini Multi",
            providerKind = AiProviderKind.GEMINI,
            baseUrl = "https://generativelanguage.googleapis.com",
            apiKey = "test-key",
            modelName = "gemini-2.5-flash",
            availableModelsCsv = "gemini-2.5-pro, gemini-2.5-flash , gemini-2.0-flash"
        )
        val models = p.allModelsList()
        assertEquals(listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash"), models)
    }

    @Test
    fun `stylish text engine generates SMP fonts symbols and decorative frames`() {
        assertTrue(StylishTextEngine.fontPresets.size >= 25)
        assertTrue(StylishTextEngine.symbolGroups.size >= 10)
        assertTrue(StylishTextEngine.lineTemplates.size >= 15)

        val bold = StylishTextEngine.fontPresets.first { it.id == "bold_serif" }.transform("Abc 123")
        assertTrue(bold.isNotBlank() && bold != "Abc 123")

        val framed = StylishTextEngine.lineTemplates.first { it.id == "double_box" }.wrap("VIP")
        assertTrue(framed.contains("VIP") && framed.contains("╔") && framed.contains("╚"))
    }

    @Test
    fun `analytics engine computes lifetime yearly monthly and today metrics cleanly`() {
        val now = System.currentTimeMillis()
        val accounts = listOf(
            AccountEntity(
                id = 1L,
                ownerUserId = 1L,
                name = "Cash",
                type = AccountType.CASH,
                balance = 5000.0,
                initialBalance = 5000.0
            )
        )
        val txs = listOf(
            TransactionEntity(
                id = 1L,
                ownerUserId = 1L,
                type = TransactionType.INCOME,
                amount = 1000.0,
                categoryId = 1L,
                accountId = 1L,
                timestamp = now
            ),
            TransactionEntity(
                id = 2L,
                ownerUserId = 1L,
                type = TransactionType.EXPENSE,
                amount = 300.0,
                categoryId = 2L,
                accountId = 1L,
                timestamp = now
            ),
            // 2 years ago -> should appear in Lifetime only, not This Year
            TransactionEntity(
                id = 3L,
                ownerUserId = 1L,
                type = TransactionType.INCOME,
                amount = 8000.0,
                categoryId = 1L,
                accountId = 1L,
                timestamp = DateTimeUtil.startOfYear(now) - 400L * 86_400_000L
            )
        )
        val metrics = AnalyticsEngine.computeDashboard(
            accounts = accounts,
            categories = emptyList(),
            transactions = txs,
            budgets = emptyList(),
            savingsGoals = emptyList(),
            now = now
        )
        assertEquals(1000.0, metrics.todayIncome, 0.01)
        assertEquals(300.0, metrics.todayExpense, 0.01)
        assertEquals(1000.0, metrics.yearlyIncome, 0.01)
        assertEquals(300.0, metrics.yearlyExpense, 0.01)
        assertEquals(9000.0, metrics.totalIncome, 0.01)
        assertEquals(300.0, metrics.totalExpense, 0.01)
        assertEquals(5000.0, metrics.totalBalance, 0.01)
    }

    @Test
    fun `profile scoped reset deletes ONLY active profile finance records and preserves sibling profiles and notes`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FinanceRepository(context)
        repo.ensureInitialized()
        val username = "scoped_${System.currentTimeMillis()}"
        val user = repo.registerUser(username, "secret12", "Owner", "Color", "Green").getOrThrow()
        val userId = user.id

        val profiles = repo.observeProfiles(userId).first()
        val profileA = profiles.first().id
        val profileB = repo.createFinanceProfile(userId, "Farm Ledger", "eco", 0xFF059669L, "Farm")

        // Create records on Profile A and Profile B
        val accA = repo.observeAccounts(userId, profileA).first().first()
        val accB = repo.observeAccounts(userId, profileB).first().first()
        val catA = repo.observeCategories(userId, profileA).first().first()
        val catB = repo.observeCategories(userId, profileB).first().first()

        repo.saveTransaction(
            TransactionEntity(
                ownerUserId = userId,
                profileId = profileA,
                type = TransactionType.INCOME,
                amount = 1500.0,
                categoryId = catA.id,
                accountId = accA.id,
                note = "ProfileA Sale"
            )
        )
        repo.saveTransaction(
            TransactionEntity(
                ownerUserId = userId,
                profileId = profileB,
                type = TransactionType.INCOME,
                amount = 9900.0,
                categoryId = catB.id,
                accountId = accB.id,
                note = "Farm Harvest"
            )
        )
        val sharedNoteId = repo.saveKeepNote(
            KeepNoteEntity(ownerUserId = userId, title = "Keep Me Safe", content = "Important Note")
        )

        // Reset ONLY Profile A
        val ok = repo.resetProfileFinanceData(userId, profileA)
        assertTrue(ok)

        val txA = repo.observeTransactions(userId, profileA).first()
        val txB = repo.observeTransactions(userId, profileB).first()
        assertTrue(txA.isEmpty())
        assertEquals(1, txB.size)
        assertEquals("Farm Harvest", txB.first().note)

        val updatedAccA = repo.observeAccounts(userId, profileA).first().first()
        val updatedAccB = repo.observeAccounts(userId, profileB).first().first()
        assertEquals(0.0, updatedAccA.balance, 0.01)
        assertEquals(9900.0, updatedAccB.balance, 0.01)

        val notes = repo.observeKeepNotes(userId).first()
        assertTrue(notes.any { it.id == sharedNoteId && it.title == "Keep Me Safe" })

        repo.logout()
    }

    @Test
    fun `ludo engine enforces 6 deployment bonus roll safe stars captures and bot move`() {
        var state = LudoEngine.newGame(playerCount = 2, playVsBot = true)
        assertEquals(LudoPhase.WAIT_ROLL, state.phase)

        // Rolling 3 with all tokens in yard passes turn
        state = LudoEngine.applyRoll(state, 3)
        assertEquals(LudoColor.YELLOW, state.currentTurn)

        // Yellow rolls 6 and deploys token 0
        state = LudoEngine.applyRoll(state, 6)
        assertEquals(LudoPhase.WAIT_MOVE, state.phase)
        val botPick = LudoBotAi.chooseToken(state, accuracyPercent = 95, rng = Random(42))
        state = LudoEngine.applyMove(state, botPick)
        assertEquals(0, state.tokens[LudoColor.YELLOW]!![botPick].progress)
        // Bonus roll after 6 keeps turn on Yellow
        assertEquals(LudoColor.YELLOW, state.currentTurn)

        // Three consecutive 6s forfeits turn
        state = state.copy(consecutiveSixes = 2, phase = LudoPhase.WAIT_ROLL)
        state = LudoEngine.applyRoll(state, 6)
        assertEquals(LudoColor.RED, state.currentTurn)
    }

    @Test
    fun `chess engine detects fools mate checkmate supports undo and bot move`() {
        var state = ChessEngine.newGame()
        // 1. f3 (f2 = 53 -> f3 = 45)
        state = ChessEngine.applyMove(state, ChessMove(53, 45))
        // 1... e5 (e7 = 12 -> e5 = 28)
        state = ChessEngine.applyMove(state, ChessMove(12, 28))
        // 2. g4 (g2 = 54 -> g4 = 38)
        state = ChessEngine.applyMove(state, ChessMove(54, 38))
        // 2... Qh4# (d8 = 3 -> h4 = 39)
        state = ChessEngine.applyMove(state, ChessMove(3, 39))

        assertTrue(state.inCheck)
        assertEquals(ChessOutcome.BLACK_WINS, state.outcome)

        // Undo restores ongoing state
        val undone = ChessEngine.undoLastMove(state)
        assertEquals(ChessOutcome.ONGOING, undone.outcome)
        assertEquals(PieceColor.BLACK, undone.turn)

        val botMove = ChessBotAi.chooseMove(undone, accuracyPercent = 95, rng = Random(1))
        assertNotNull(botMove)
        assertEquals(ChessOutcome.BLACK_WINS, ChessEngine.applyMove(undone, botMove!!).outcome)
    }

    @Test
    fun `sholo guti 16 beads engine supports adjacent slide multi jump chain capture and bot ai`() {
        var state = SholoGutiEngine.newGame()
        assertEquals(16, state.rubyCount)
        assertEquals(16, state.goldCount)
        assertEquals(SholoGutiPlayer.RUBY, state.turn)

        // Slide Ruby bead from (2,5)=23 into empty center (2,4)=18
        val legal23 = SholoGutiEngine.legalMovesForNode(state, 23)
        assertTrue(legal23.any { it.from == 23 && it.to == 18 && !it.isCapture })
        state = SholoGutiEngine.applyMove(state, SholoGutiMove(from = 23, to = 18))
        assertEquals(SholoGutiPlayer.GOLD, state.turn)

        // Bot AI responds for GOLD
        val botMove = SholoGutiBotAi.chooseMove(state, accuracyPercent = 90, rng = Random(7))
        assertNotNull(botMove)
        state = SholoGutiEngine.applyMove(state, botMove!!)
        assertEquals(SholoGutiPlayer.RUBY, state.turn)

        // Verify double-jump chain capture along col 2: 23 -> 13 (captures 18) -> 4 (captures 8)
        val chainBoard = MutableList<SholoGutiPlayer?>(SholoGutiEngine.NODE_COUNT) { null }
        chainBoard[23] = SholoGutiPlayer.RUBY
        chainBoard[18] = SholoGutiPlayer.GOLD
        chainBoard[8] = SholoGutiPlayer.GOLD
        chainBoard[0] = SholoGutiPlayer.GOLD // extra bead so Gold doesn't lose immediately after 2 captures
        val chainStart = SholoGutiState(board = chainBoard, turn = SholoGutiPlayer.RUBY)
        val firstJump = SholoGutiEngine.applyMove(chainStart, SholoGutiMove(from = 23, to = 13, capturedNode = 18))
        assertEquals(13, firstJump.chainedNodeId)
        assertEquals(SholoGutiPlayer.RUBY, firstJump.turn)
        assertEquals(1, firstJump.rubyCaptured)

        val secondJump = SholoGutiEngine.applyMove(firstJump, SholoGutiMove(from = 13, to = 4, capturedNode = 8))
        assertEquals(null, secondJump.chainedNodeId)
        assertEquals(2, secondJump.rubyCaptured)
        assertEquals(SholoGutiOutcome.ONGOING, secondJump.outcome)
        assertEquals(SholoGutiPlayer.GOLD, secondJump.turn)
    }

    @Test
    fun `language manager toggles bangla and english dynamically without restart`() {
        LanguageManager.resetForTest(AppLanguage.BANGLA)
        assertEquals(AppLanguage.BANGLA, LanguageManager.currentLanguage.value)
        assertTrue(LanguageManager.gameStrings.value.isBn)
        assertEquals("বাংলা", LanguageManager.gameStrings.value.liveLanguageBadge)

        val toggled = LanguageManager.toggleLanguage()
        assertEquals(AppLanguage.ENGLISH, toggled)
        assertEquals(AppLanguage.ENGLISH, LanguageManager.currentLanguage.value)
        assertFalse(LanguageManager.gameStrings.value.isBn)
        assertEquals("EN", LanguageManager.gameStrings.value.liveLanguageBadge)
        assertEquals("Play & Earn", LanguageManager.appStrings.value.gamesArenaTitle)

        LanguageManager.setLanguage(AppLanguage.BANGLA)
        assertEquals(AppLanguage.BANGLA, LanguageManager.currentLanguage.value)
        assertTrue(LanguageManager.gameStrings.value.isBn)
    }

    @Test
    fun `carrom baseline sliding impulse cap chess same color bishop draw and coin rewards work`() {
        // 1. Carrom striker slides freely to either side of baseline even with central coin obstacles & keeps controlled power cap
        assertTrue(CarromEngine.MAX_IMPULSE <= 1.10f)
        var carrom = CarromEngine.newGame(isVsBot = true)
        carrom = carrom.copy(
            pieces = carrom.pieces.map { piece ->
                if (piece.id == 1) piece.copy(x = 0.20f, y = carrom.strikerBaselineY) else piece
            }
        )
        val slidRight = CarromEngine.setStrikerBaselineX(carrom, CarromEngine.BASELINE_MAX_X)
        assertTrue(slidRight.strikerBaselineX > 0.45f)
        val slidLeft = CarromEngine.setStrikerBaselineX(slidRight, -CarromEngine.BASELINE_MAX_X)
        assertTrue(slidLeft.strikerBaselineX < -0.45f)
        val fired = CarromEngine.shootStriker(slidLeft, 5.0f, -5.0f)
        assertEquals(CarromPhase.MOVING, fired.phase)
        val striker = fired.pieces.first { it.type == com.example.domain.games.CarromPieceType.STRIKER }
        val speed = kotlin.math.sqrt(striker.vx * striker.vx + striker.vy * striker.vy)
        assertEquals(CarromEngine.MAX_IMPULSE, speed, 0.01f)

        // 2. Chess FIDE rule: K+B vs K+B on same-colored squares is DRAW_INSUFFICIENT_MATERIAL; opposite-colored stays ONGOING
        val sameColorBoard = arrayOfNulls<ChessPiece>(64).apply {
            this[60] = ChessPiece(PieceColor.WHITE, PieceType.KING)   // e1
            this[58] = ChessPiece(PieceColor.WHITE, PieceType.BISHOP) // c1 (dark: (7+2)%2==1)
            this[4] = ChessPiece(PieceColor.BLACK, PieceType.KING)    // e8
            this[5] = ChessPiece(PieceColor.BLACK, PieceType.BISHOP)  // f8 (dark: (0+5)%2==1)
        }.toList()
        val sameColorNext = ChessEngine.applyMove(ChessGameState(board = sameColorBoard), ChessMove(60, 59))
        assertEquals(ChessOutcome.DRAW_INSUFFICIENT_MATERIAL, sameColorNext.outcome)

        val oppColorBoard = arrayOfNulls<ChessPiece>(64).apply {
            this[60] = ChessPiece(PieceColor.WHITE, PieceType.KING)   // e1
            this[58] = ChessPiece(PieceColor.WHITE, PieceType.BISHOP) // c1 (dark)
            this[4] = ChessPiece(PieceColor.BLACK, PieceType.KING)    // e8
            this[2] = ChessPiece(PieceColor.BLACK, PieceType.BISHOP)  // c8 (light: (0+2)%2==0)
        }.toList()
        val oppColorNext = ChessEngine.applyMove(ChessGameState(board = oppColorBoard), ChessMove(60, 59))
        assertEquals(ChessOutcome.ONGOING, oppColorNext.outcome)

        // 3. Coin reward tier verification: Online/Friend gives 3x Offline rewards
        assertEquals(25.0, PlayWalletRepository.computeMatchCompletionCoins(GameMatchResult.WIN, isOnlineOrFriend = false), 0.01)
        assertEquals(75.0, PlayWalletRepository.computeMatchCompletionCoins(GameMatchResult.WIN, isOnlineOrFriend = true), 0.01)
    }

    @Test
    fun `in memory room database builds cleanly with version 5 schema and tracks game leaderboard`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val memDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        assertNotNull(memDb.aiProviderDao())
        val dao = memDb.gameLeaderboardDao()

        dao.upsert(
            GameLeaderboardEntity(
                gameId = "CHESS",
                gamesPlayed = 4,
                wins = 3,
                losses = 1,
                draws = 0,
                highScore = 1480,
                totalScore = 4100,
                currentWinStreak = 2,
                bestWinStreak = 3,
                highestBotAccuracyBeaten = 90
            )
        )
        val fetched = dao.getByGameId("CHESS")
        assertNotNull(fetched)
        assertEquals(1480, fetched!!.highScore)
        assertEquals(75f, fetched.winRatePercent(), 0.01f)
        assertEquals(90, fetched.highestBotAccuracyBeaten)

        memDb.close()
    }

    @Test
    fun `repository records game match results and computes win rates streaks and high scores`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FinanceRepository(context)
        repo.resetGameLeaderboard()

        val firstWin = repo.recordGameResult("LUDO", GameMatchResult.WIN, score = 1200, vsBot = true, botAccuracy = 80)
        assertEquals(1, firstWin.gamesPlayed)
        assertEquals(1, firstWin.wins)
        assertEquals(1200, firstWin.highScore)
        assertEquals(1, firstWin.bestWinStreak)
        assertEquals(80, firstWin.highestBotAccuracyBeaten)

        val secondWin = repo.recordGameResult("LUDO", GameMatchResult.WIN, score = 1550, vsBot = true, botAccuracy = 95)
        assertEquals(2, secondWin.gamesPlayed)
        assertEquals(2, secondWin.wins)
        assertEquals(1550, secondWin.highScore)
        assertEquals(2, secondWin.currentWinStreak)
        assertEquals(2, secondWin.bestWinStreak)
        assertEquals(95, secondWin.highestBotAccuracyBeaten)

        val thirdLoss = repo.recordGameResult("LUDO", GameMatchResult.LOSS, score = 400, vsBot = true, botAccuracy = 100)
        assertEquals(3, thirdLoss.gamesPlayed)
        assertEquals(2, thirdLoss.wins)
        assertEquals(1, thirdLoss.losses)
        assertEquals(1550, thirdLoss.highScore)
        assertEquals(0, thirdLoss.currentWinStreak)
        assertEquals(2, thirdLoss.bestWinStreak)
        assertEquals(95, thirdLoss.highestBotAccuracyBeaten)
        assertEquals(66.66f, thirdLoss.winRatePercent(), 0.2f)

        val observed = repo.observeGameLeaderboard().first()
        assertTrue(observed.any { it.gameId == "LUDO" && it.highScore == 1550 })
        repo.resetGameLeaderboard()
    }

    @Test
    fun `finplay pro dual wallet enforces one way transfer winning only withdrawal 100 percent loss cashback and armory`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val memDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val walletRepo = PlayWalletRepository(memDb)
        val userId = 42L

        // 1. Submit manual bKash Deposit -> PENDING -> Admin Approve credits Tournament Deposit Wallet
        val depResult = walletRepo.submitDepositRequest(
            userId = userId,
            channel = WalletPaymentChannel.BKASH,
            amount = 500.0,
            senderNumber = "01711000111",
            transactionId = "TRX998877"
        )
        assertTrue(depResult.isSuccess)
        val reqId = depResult.getOrThrow().id
        assertEquals(WalletRequestStatus.PENDING, depResult.getOrThrow().status)
        assertEquals(0.0, walletRepo.getWalletOnce(userId).tournamentBalance, 0.01)

        assertTrue(walletRepo.adminApproveDeposit(reqId, "Verified").isSuccess)
        var wallet = walletRepo.getWalletOnce(userId)
        assertEquals(500.0, wallet.tournamentBalance, 0.01)
        assertEquals(0.0, wallet.winningBalance, 0.01)
        assertEquals(0.0, wallet.gameBalance, 0.01)

        // 2. Strictly One-Way Transfer: Tournament -> Game
        assertTrue(walletRepo.transferTournamentToGameWallet(userId, 100.0).isSuccess)
        wallet = walletRepo.getWalletOnce(userId)
        assertEquals(400.0, wallet.tournamentBalance, 0.01)
        assertEquals(100.0, wallet.gameBalance, 0.01)

        // 3. Withdraw strictly checks WINNING Coins ONLY (Deposit & Game wallets cannot be withdrawn!)
        val depositWithdrawAttempt = walletRepo.submitWithdrawRequest(
            userId = userId,
            channel = WalletPaymentChannel.NAGAD,
            amount = 50.0, // Even though tournamentBalance=400 & gameBalance=100, winningBalance=0!
            receiverNumber = "01811223344"
        )
        assertTrue(depositWithdrawAttempt.isFailure)

        // 4. Paid Tournament Entry + 100% LOSS CASHBACK into Game Wallet
        assertTrue(walletRepo.enterTournamentMatch(userId, "CHESS", 100.0).isSuccess)
        assertEquals(300.0, walletRepo.getWalletOnce(userId).tournamentBalance, 0.01)

        val lossReceipt = walletRepo.settleTournamentMatch(userId, "CHESS", 100.0, GameMatchResult.LOSS).getOrThrow()
        assertEquals("GAME", lossReceipt.creditedWalletBucket)
        assertEquals(100.0, lossReceipt.creditedAmount, 0.01)
        wallet = walletRepo.getWalletOnce(userId)
        assertEquals(300.0, wallet.tournamentBalance, 0.01)
        assertEquals(200.0, wallet.gameBalance, 0.01) // 100 transferred + 100 loss cashback!
        val ledgerAfterLoss = walletRepo.observeLedger(userId).first()
        assertTrue(ledgerAfterLoss.any { it.walletBucket == "GAME" && it.amountDelta == 100.0 })

        // 5. Paid Tournament Entry + WIN credits Winning Prize strictly to WINNING Wallet
        assertTrue(walletRepo.enterTournamentMatch(userId, "LUDO", 50.0).isSuccess)
        val winReceipt = walletRepo.settleTournamentMatch(userId, "LUDO", 50.0, GameMatchResult.WIN, explicitWinPrize = 90.0).getOrThrow()
        assertEquals("WINNING", winReceipt.creditedWalletBucket)
        assertEquals(90.0, winReceipt.creditedAmount, 0.01)
        wallet = walletRepo.getWalletOnce(userId)
        assertEquals(250.0, wallet.tournamentBalance, 0.01)
        assertEquals(90.0, wallet.winningBalance, 0.01)

        // 6. Now user CAN withdraw up to 90 from Winning Coins (and Admin Reject refunds back to Winning Coins)
        val validWithdraw = walletRepo.submitWithdrawRequest(
            userId = userId,
            channel = WalletPaymentChannel.NAGAD,
            amount = 60.0,
            receiverNumber = "01811223344"
        )
        assertTrue(validWithdraw.isSuccess)
        assertEquals(30.0, walletRepo.getWalletOnce(userId).winningBalance, 0.01)

        assertTrue(walletRepo.adminRejectWithdrawal(validWithdraw.getOrThrow().id, "Wrong number").isSuccess)
        assertEquals(90.0, walletRepo.getWalletOnce(userId).winningBalance, 0.01)

        // 7. Armory purchase with Game Wallet coins (Board Skin + Consumable Skill)
        val skinBuy = walletRepo.purchaseArmoryItem(userId, PlayArmoryCatalog.SKIN_OBSIDIAN_GOLD)
        assertTrue(skinBuy.isSuccess)
        assertTrue(walletRepo.equipArmoryItem(userId, PlayArmoryCatalog.SKIN_OBSIDIAN_GOLD).isSuccess)
        val skillBuy = walletRepo.purchaseArmoryItem(userId, PlayArmoryCatalog.SKILL_LUDO_REROLL)
        assertTrue(skillBuy.isSuccess)
        assertEquals(1, PlayArmoryCatalog.parseSkillCharges(walletRepo.getWalletOnce(userId).skillChargesJson)[PlayArmoryCatalog.SKILL_LUDO_REROLL])
        assertTrue(walletRepo.consumeSkillCharge(userId, PlayArmoryCatalog.SKILL_LUDO_REROLL).isSuccess)
        assertEquals(0, PlayArmoryCatalog.parseSkillCharges(walletRepo.getWalletOnce(userId).skillChargesJson)[PlayArmoryCatalog.SKILL_LUDO_REROLL] ?: 0)

        memDb.close()
    }

    @Test
    fun `executive admin panel handles secret login free fire auto cleanup slot cap coin edits broadcasts and bot configs`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val memDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val walletRepo = PlayWalletRepository(memDb)
        walletRepo.ensureDefaultGameConfigs()

        // 1. Verify default tiers & bot accuracy seeded for all 5 games
        val tiers = walletRepo.observeAllTiers().first()
        assertTrue(tiers.size >= 15)
        val ludoBot = walletRepo.getBotConfigOnce("LUDO")
        assertTrue(ludoBot.botAccuracyPercent in 10..100)

        // Admin updates Bot Accuracy & Max Seats
        walletRepo.adminSaveGameBotConfig(ludoBot.copy(botAccuracyPercent = 88, maxConcurrentSeats = 6))
        val updatedBot = walletRepo.getBotConfigOnce("LUDO")
        assertEquals(88, updatedBot.botAccuracyPercent)
        assertEquals(6, updatedBot.maxConcurrentSeats)

        // 2. Seed two users + Admin Coin Edit
        val u1Id = memDb.userDao().insert(
            com.example.data.local.UserEntity(username = "sniper_ff", displayName = "Sniper FF", passwordHash = "h1", salt = "s1")
        )
        val u2Id = memDb.userDao().insert(
            com.example.data.local.UserEntity(username = "rush_pro", displayName = "Rush Pro", passwordHash = "h2", salt = "s2")
        )
        val u3Id = memDb.userDao().insert(
            com.example.data.local.UserEntity(username = "third_player", displayName = "Third Player", passwordHash = "h3", salt = "s3")
        )
        assertTrue(walletRepo.adminAdjustUserWallet(u1Id, tournamentDelta = 200.0, winningDelta = 50.0, gameDelta = 75.0, note = "VIP Bonus").isSuccess)
        assertTrue(walletRepo.adminAdjustUserWallet(u2Id, tournamentDelta = 200.0, winningDelta = 0.0, gameDelta = 0.0, note = "Starter").isSuccess)
        assertTrue(walletRepo.adminAdjustUserWallet(u3Id, tournamentDelta = 200.0, winningDelta = 0.0, gameDelta = 0.0, note = "Starter").isSuccess)
        val u1Wallet = walletRepo.getWalletOnce(u1Id)
        assertEquals(200.0, u1Wallet.tournamentBalance, 0.01)
        assertEquals(50.0, u1Wallet.winningBalance, 0.01)
        assertEquals(75.0, u1Wallet.gameBalance, 0.01)

        // 3. Free Fire Tournament: Slot Cap Enforcement, Winner Payout to Winning Coins, & Auto-Delete Completed History on New Launch
        val ff1 = walletRepo.adminCreateExternalTournament(
            com.example.data.local.ExternalTournamentEntity(
                gameCategory = "FREE_FIRE",
                title = "Bermuda Solo Clash #1",
                modeLabel = "Solo",
                mapName = "Bermuda",
                entryFee = 50.0,
                prizePool = 500.0,
                perKillReward = 20.0,
                maxSlots = 2, // Strict 2-slot cap for test
                startTimeLabel = "Tonight 9 PM",
                roomIdCode = "FF-9901",
                roomPassword = "WIN1"
            )
        ).getOrThrow()

        assertTrue(walletRepo.joinExternalTournament(u1Id, ff1.id, "SniperBD", "11223344").isSuccess)
        assertTrue(walletRepo.joinExternalTournament(u2Id, ff1.id, "RushBD", "55667788").isSuccess)
        // Slot limit is now full (2/2) -> u3 must be blocked with wait message!
        val fullAttempt = walletRepo.joinExternalTournament(u3Id, ff1.id, "ThirdBD", "99887766")
        assertTrue(fullAttempt.isFailure)

        // Complete Tournament #1 & credit Winner Prize to sniper_ff's winningBalance
        assertTrue(walletRepo.adminCompleteExternalTournament(ff1.id, winnerUsername = "sniper_ff", winnerPrizeAmount = 400.0).isSuccess)
        assertEquals(450.0, walletRepo.getWalletOnce(u1Id).winningBalance, 0.01) // 50 + 400
        assertEquals("COMPLETED", walletRepo.observeExternalTournaments().first().first { it.id == ff1.id }.status)

        // Launch NEW Free Fire Tournament #2 -> MUST auto-delete completed Tournament #1 & its entries!
        val ff2 = walletRepo.adminCreateExternalTournament(
            com.example.data.local.ExternalTournamentEntity(
                gameCategory = "FREE_FIRE",
                title = "Bermuda Solo Clash #2",
                modeLabel = "Squad",
                mapName = "Purgatory",
                entryFee = 100.0,
                prizePool = 1500.0,
                maxSlots = 48,
                startTimeLabel = "Tomorrow 8 PM"
            )
        ).getOrThrow()

        val remainingTournaments = walletRepo.observeExternalTournaments().first()
        assertEquals(1, remainingTournaments.size)
        assertEquals(ff2.id, remainingTournaments.first().id)
        assertTrue(walletRepo.observeUserExternalEntries(u1Id).first().isEmpty())

        // 4. Broadcast Notification fans out to ALL users
        assertTrue(walletRepo.adminSendBroadcastNotification("Mega Cup Live!", "Join Bermuda #2 now!", "URGENT").isSuccess)
        val u1Notifs = memDb.notificationDao().observeByUser(u1Id).first()
        val u2Notifs = memDb.notificationDao().observeByUser(u2Id).first()
        assertTrue(u1Notifs.any { it.title.contains("Mega Cup Live!") })
        assertTrue(u2Notifs.any { it.title.contains("Mega Cup Live!") })

        // 5. Same-APK Secret Admin Login + Admin Portal Key verification via FinanceRepository (PBKDF2 120K)
        com.example.security.SecurityShield.resetAdminAttempts()
        val app = ApplicationProvider.getApplicationContext<Context>()
        val repo = FinanceRepository(app)
        repo.ensureInitialized()

        // Verify PBKDF2 pure-HmacSHA256 fallback matches SecretKeyFactory exactly
        val fallbackBytes = com.example.security.CryptoUtil.derivePbkdf2HmacSha256Fallback(
            password = "FINPLAY-ADMIN-2026".toByteArray(Charsets.UTF_8),
            salt = com.example.security.CryptoUtil.SEALED_ADMIN_KEY_SALT.toByteArray(Charsets.UTF_8),
            iterations = 120_000,
            dkLen = 32
        )
        val fallbackHash = com.example.security.CryptoUtil.PBKDF2_PREFIX +
            fallbackBytes.joinToString("") { "%02x".format(it) }
        assertEquals(com.example.security.CryptoUtil.SEALED_ADMIN_KEY_HASH, fallbackHash)

        // Verify Admin Key login (including whitespace trimming) sets isAdminSession = true
        val keyRes = repo.authenticateAdminDetailed("  FINPLAY-ADMIN-2026  ")
        assertTrue(keyRes is FinanceRepository.AdminAuthResult.Success)
        assertTrue(repo.prefs.getSnapshot().isAdminSession)
        repo.logout()
        assertFalse(repo.prefs.getSnapshot().isAdminSession)

        // Verify Admin Password also unlocks the Admin Portal Key dialog
        val passInKeyDialog = repo.authenticateAdminDetailed("FinPlay@Admin2026")
        assertTrue(passInKeyDialog is FinanceRepository.AdminAuthResult.Success)
        assertTrue(repo.prefs.getSnapshot().isAdminSession)
        repo.logout()

        // Verify Secret Admin Login on the main Login screen sets isAdminSession = true
        val adminRes = repo.authenticateUserOrSecretAdmin(
            username = "finplay_root",
            password = "FinPlay@Admin2026",
            remember = false
        )
        assertTrue(adminRes.isSuccess)
        assertTrue(adminRes.getOrThrow() is FinanceRepository.LoginOutcome.AdminSession)
        assertTrue(repo.prefs.getSnapshot().isAdminSession)

        // Verify AdminViewModel defaults FLAG_SECURE shield to opt-in (false) so streaming emulators render cleanly
        val adminVm = com.example.ui.viewmodel.AdminViewModel(app, repo)
        assertFalse(adminVm.screenShieldEnabled.value)

        repo.logout()
        memDb.close()
    }

    @Test
    fun `online match serializer round trips arena game states cleanly`() {
        val chess = ChessEngine.applyMove(ChessEngine.newGame(), ChessEngine.legalMoves(ChessEngine.newGame()).first())
        val chessDecoded = OnlineMatchSerializer.decodeChess(OnlineMatchSerializer.encodeChess(chess))
        assertNotNull(chessDecoded)
        assertEquals(chess.turn, chessDecoded!!.turn)
        assertEquals(chess.board, chessDecoded.board)

        val ludo = LudoEngine.applyRoll(LudoEngine.newGame(2, playVsBot = false), 6)
        val ludoDecoded = OnlineMatchSerializer.decodeLudo(OnlineMatchSerializer.encodeLudo(ludo))
        assertNotNull(ludoDecoded)
        assertEquals(6, ludoDecoded!!.diceValue)
        assertEquals(ludo.phase, ludoDecoded.phase)

        val sholo = SholoGutiEngine.applyMove(SholoGutiEngine.newGame(), SholoGutiMove(23, 18))
        val sholoDecoded = OnlineMatchSerializer.decodeSholoGuti(OnlineMatchSerializer.encodeSholoGuti(sholo))
        assertNotNull(sholoDecoded)
        assertEquals(sholo.turn, sholoDecoded!!.turn)
        assertEquals(sholo.board, sholoDecoded.board)

        val snakes = SnakesLaddersEngine.rollAndMove(SnakesLaddersEngine.newGame(2, playVsBot = false), forcedDice = 4).nextState
        val snakesDecoded = OnlineMatchSerializer.decodeSnakes(OnlineMatchSerializer.encodeSnakes(snakes), hostName = "P1", guestName = "P2")
        assertNotNull(snakesDecoded)
        assertEquals(snakes.players[0].position, snakesDecoded!!.players[0].position)
        assertEquals(snakes.currentTurnIndex, snakesDecoded.currentTurnIndex)
    }

    @Test
    fun `free fire joined players roster custom scheduler and 30 day wallet retention work end to end`() = kotlinx.coroutines.runBlocking {
        val memDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val walletRepo = com.example.data.games.PlayWalletRepository(memDb)
        walletRepo.ensureDefaultGameConfigs()

        val p1 = memDb.userDao().insert(com.example.data.local.UserEntity(username = "sniper_bd", displayName = "Sniper", passwordHash = "h", salt = "s"))
        val p2 = memDb.userDao().insert(com.example.data.local.UserEntity(username = "rusher_ff", displayName = "Rusher", passwordHash = "h", salt = "s"))
        walletRepo.adminAdjustUserWallet(p1, tournamentDelta = 500.0, winningDelta = 0.0, gameDelta = 0.0)
        walletRepo.adminAdjustUserWallet(p2, tournamentDelta = 500.0, winningDelta = 0.0, gameDelta = 0.0)

        // 1. Custom Any-Time Date/Time combination & Live Countdown auto-calculation
        val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(2027, java.util.Calendar.MARCH, 15, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val combinedEpoch = com.example.domain.games.TournamentTimeFormatter.combineUtcDateWithLocalTime(
            utcDateMillis = utcCal.timeInMillis,
            hourOfDay = 21,
            minute = 30
        )
        val verifyLocal = java.util.Calendar.getInstance().apply { timeInMillis = combinedEpoch }
        assertEquals(2027, verifyLocal.get(java.util.Calendar.YEAR))
        assertEquals(java.util.Calendar.MARCH, verifyLocal.get(java.util.Calendar.MONTH))
        assertEquals(15, verifyLocal.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(21, verifyLocal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(30, verifyLocal.get(java.util.Calendar.MINUTE))

        assertEquals("1d 02:03:04", com.example.domain.games.TournamentTimeFormatter.formatCountdown((86_400 + 7_384) * 1000L, false))
        assertEquals("00:15:09", com.example.domain.games.TournamentTimeFormatter.formatCountdown((15 * 60 + 9) * 1000L, false))
        assertEquals("STARTING NOW!", com.example.domain.games.TournamentTimeFormatter.formatCountdown(0L, false))
        assertEquals("শুরু হচ্ছে!", com.example.domain.games.TournamentTimeFormatter.formatCountdown(-500L, true))

        // 2. Multiple players join Free Fire tournament -> all entries exposed via observeAllExternalEntries()
        val match = walletRepo.adminCreateExternalTournament(
            com.example.data.local.ExternalTournamentEntity(
                gameCategory = "FREE_FIRE",
                title = "Bermuda Custom Night Cup",
                modeLabel = "Squad",
                mapName = "Bermuda",
                entryFee = 50.0,
                prizePool = 1000.0,
                maxSlots = 48,
                startTimeLabel = com.example.domain.games.TournamentTimeFormatter.formatScheduleLabel(combinedEpoch),
                matchStartTimeMillis = combinedEpoch
            )
        ).getOrThrow()

        assertTrue(walletRepo.joinExternalTournament(p1, match.id, "BD_SNIPER_PRO", "11223344").isSuccess)
        assertTrue(walletRepo.joinExternalTournament(p2, match.id, "RUSHER_KING", "99887766").isSuccess)

        val allEntries = walletRepo.observeAllExternalEntries().first().filter { it.tournamentId == match.id }
        assertEquals(2, allEntries.size)
        assertEquals("BD_SNIPER_PRO", allEntries[0].inGameName)
        assertEquals("11223344", allEntries[0].inGameUid)
        assertEquals("sniper_bd", allEntries[0].usernameSnapshot)
        assertEquals("RUSHER_KING", allEntries[1].inGameName)
        assertEquals("99887766", allEntries[1].inGameUid)

        // 3. Strict 1-Month (30-Day) Wallet History Rolling Retention & Auto-Cleanup
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L
        val old40Days = now - 40L * dayMs
        val old32Days = now - 32L * dayMs
        val fresh5Days = now - 5L * dayMs

        memDb.playWalletLedgerDao().insertAll(
            listOf(
                com.example.data.local.PlayWalletLedgerEntity(
                    ownerUserId = p1,
                    walletBucket = "TOURNAMENT",
                    kind = com.example.data.local.WalletLedgerKind.DEPOSIT_APPROVED,
                    amountDelta = 100.0,
                    note = "Old 40-day ledger entry",
                    createdAt = old40Days
                ),
                com.example.data.local.PlayWalletLedgerEntity(
                    ownerUserId = p1,
                    walletBucket = "WINNING",
                    kind = com.example.data.local.WalletLedgerKind.TOURNAMENT_WIN_PRIZE,
                    amountDelta = 250.0,
                    note = "Fresh 5-day ledger entry",
                    createdAt = fresh5Days
                )
            )
        )
        memDb.playWalletRequestDao().insertAll(
            listOf(
                com.example.data.local.PlayWalletRequestEntity(
                    ownerUserId = p1,
                    usernameSnapshot = "sniper_bd",
                    requestType = com.example.data.local.WalletRequestType.DEPOSIT,
                    channel = com.example.data.local.WalletPaymentChannel.BKASH,
                    amount = 100.0,
                    accountNumber = "01711000001",
                    transactionId = "OLD_DEP_40D",
                    status = com.example.data.local.WalletRequestStatus.APPROVED,
                    createdAt = old40Days,
                    updatedAt = old40Days
                ),
                com.example.data.local.PlayWalletRequestEntity(
                    ownerUserId = p1,
                    usernameSnapshot = "sniper_bd",
                    requestType = com.example.data.local.WalletRequestType.WITHDRAW,
                    channel = com.example.data.local.WalletPaymentChannel.NAGAD,
                    amount = 200.0,
                    accountNumber = "01811000002",
                    status = com.example.data.local.WalletRequestStatus.PAID,
                    createdAt = old32Days,
                    updatedAt = old32Days
                ),
                com.example.data.local.PlayWalletRequestEntity(
                    ownerUserId = p1,
                    usernameSnapshot = "sniper_bd",
                    requestType = com.example.data.local.WalletRequestType.DEPOSIT,
                    channel = com.example.data.local.WalletPaymentChannel.BKASH,
                    amount = 300.0,
                    accountNumber = "01711000003",
                    transactionId = "FRESH_DEP_5D",
                    status = com.example.data.local.WalletRequestStatus.APPROVED,
                    createdAt = fresh5Days,
                    updatedAt = fresh5Days
                ),
                com.example.data.local.PlayWalletRequestEntity(
                    ownerUserId = p1,
                    usernameSnapshot = "sniper_bd",
                    requestType = com.example.data.local.WalletRequestType.WITHDRAW,
                    channel = com.example.data.local.WalletPaymentChannel.ROCKET,
                    amount = 150.0,
                    accountNumber = "01911000004",
                    status = com.example.data.local.WalletRequestStatus.PENDING,
                    createdAt = old32Days,
                    updatedAt = old32Days
                )
            )
        )

        val (prunedLedgerCount, prunedReqCount) = walletRepo.pruneOldWalletHistory(nowMillis = now)
        assertEquals(1, prunedLedgerCount)
        assertEquals(2, prunedReqCount)

        val keptLedger = walletRepo.observeLedger(p1).first()
        assertFalse(keptLedger.any { it.note == "Old 40-day ledger entry" })
        assertTrue(keptLedger.any { it.note == "Fresh 5-day ledger entry" })

        val keptRequests = walletRepo.observeUserRequests(p1).first()
        assertFalse(keptRequests.any { it.transactionId == "OLD_DEP_40D" })
        assertFalse(keptRequests.any { it.accountNumber == "01811000002" })
        assertTrue(keptRequests.any { it.transactionId == "FRESH_DEP_5D" })
        assertTrue(keptRequests.any { it.status == com.example.data.local.WalletRequestStatus.PENDING })

        memDb.close()
    }
}
