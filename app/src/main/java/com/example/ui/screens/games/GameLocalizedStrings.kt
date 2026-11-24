package com.example.ui.screens.games

import com.example.R
import com.example.domain.games.LudoColor
import com.example.i18n.AppLanguage

enum class ArenaGameId {
    LUDO,
    SNAKES_LADDERS,
    CARROM,
    SHOLO_GUTI,
    CHESS,
    FRUIT_SLASH,
    WAR_LAND
}

data class GameRuleSection(val heading: String, val bullets: List<String>)

data class GameUiStrings(
    val isBn: Boolean,
    val arenaBannerTitle: String,
    val arenaBannerDesc: String,
    val arenaHeaderSubtitle: String,
    val playNow: String,
    val rules: String,
    val newMatch: String,
    val soundOn: String,
    val soundOff: String,
    val playVsBot: String,
    val botAccuracy: String,
    val playersLabel: String,
    val botSuffix: String,
    val undoMove: String,
    val playAnotherMatch: String,
    val winsSuffix: String,
    // Ludo
    val rollDice: String,
    val rollingDice: String,
    val ludoRollPrompt: String,
    val ludoSafeStarTip: String,
    // Chess
    val promotePawnTitle: String,
    val whiteTurn: String,
    val blackTurn: String,
    val botThinking: String,
    val checkmateWhiteWins: String,
    val checkmateBlackWins: String,
    val drawStalemate: String,
    val drawFiftyMove: String,
    val drawInsufficient: String,
    val checkTag: String,
    val capturedLabel: String,
    val movesLabel: String,
    val advantageLabel: String,
    // Leaderboard & Arena Polish
    val leaderboardTitle: String,
    val leaderboardSubtitle: String,
    val totalMatchesLabel: String,
    val winRateLabel: String,
    val highScoreLabel: String,
    val bestStreakLabel: String,
    val maxBotBeatenLabel: String,
    val winsShort: String,
    val lossesShort: String,
    val drawsShort: String,
    val noMatchesYet: String,
    val resetLeaderboard: String,
    val resetLeaderboardConfirm: String,
    val matchScoreRecorded: String,
    val liveLanguageBadge: String,
    // Play & Earn, Dual Wallets, Armory, Live Chat & Online Multiplayer
    val tabArena: String,
    val tabWallets: String,
    val tabArmory: String,
    val tournamentWalletTitle: String,
    val gameWalletTitle: String,
    val tournamentWalletHint: String,
    val gameWalletHint: String,
    val depositLabel: String,
    val withdrawLabel: String,
    val transferToGameLabel: String,
    val oneWayTransferNote: String,
    val reverseTransferLocked: String,
    val lossCashbackBanner: String,
    val liveChatLabel: String,
    val onlineFriendsTitle: String,
    val onlineSignInPrompt: String,
    val signInWithGoogleLabel: String,
    val createPrivateRoomLabel: String,
    val quickMatchLabel: String,
    val joinByCodeLabel: String,
    val enterRoomCodeHint: String,
    val waitingForFriendLabel: String,
    val shareCodeLabel: String,
    val yourTurnBadge: String,
    val opponentTurnBadge: String,
    val tournamentEntryLabel: String,
    val freePlayLabel: String,
    val armoryShopTitle: String,
    val armoryShopSubtitle: String,
    val boardSkinsTab: String,
    val avatarsTab: String,
    val skillsTab: String,
    val buyWithGameCoins: String,
    val equipLabel: String,
    val equippedLabel: String,
    val ownedLabel: String,
    val chargesCountLabel: String,
    val useSkillLabel: String,
    val cashbackReceivedTitle: String,
    val prizeWonTitle: String,
    val entryRefundedTitle: String
) {
    fun gameTitle(id: ArenaGameId): String = when (id) {
        ArenaGameId.LUDO -> if (isBn) "রয়্যাল লুডু 3D" else "Royal Ludo 3D"
        ArenaGameId.SNAKES_LADDERS -> if (isBn) "সাপ-লুডু 3D (Snakes & Ladders)" else "Snakes & Ladders 3D"
        ArenaGameId.CARROM -> if (isBn) "ক্যারম বোর্ড 3D" else "Carrom Board 3D"
        ArenaGameId.SHOLO_GUTI -> if (isBn) "ষোল গুটি 3D (16 Beads)" else "Sholo Guti 3D (16 Beads)"
        ArenaGameId.CHESS -> if (isBn) "গ্র্যান্ডমাস্টার দাবা 3D" else "Grandmaster Chess 3D"
        ArenaGameId.FRUIT_SLASH -> if (isBn) "ফ্রুট স্ল্যাশ 3D নিনজা" else "Fruit Slash 3D Ninja"
        ArenaGameId.WAR_LAND -> if (isBn) "ওয়ার ল্যান্ড (3D ব্যাটল রয়্যাল)" else "War Land (3D Battle Royale)"
    }

    fun gameSubtitle(id: ArenaGameId): String = when (id) {
        ArenaGameId.LUDO -> if (isBn) "২–৫ প্লেয়ার স্টার বোর্ড • ৩D ডাইস কন্ট্রোল, ক্যাপচার ও মাল্টিপ্লেয়ার" else "2–5P Star Board • 3D Dice Controls, Captures & Real Multiplayer"
        ArenaGameId.SNAKES_LADDERS -> if (isBn) "১০০-ঘরের ৩D বোর্ড • গোল্ডেন মই, অ্যানিমেটেড সাপ ও ৪-প্লেয়ার ডাইস" else "100-Tile 3D Board • Golden Ladders, Animated Snakes & 4-Player Dice"
        ArenaGameId.CARROM -> if (isBn) "রিয়েলিস্টিক উড ফিনিশ • প্রফেশনাল ৩D বোর্ড, কুইন কাভার ও Play & Earn" else "Realistic Wood Finish • Pro 3D Board, Queen Cover & Play & Earn"
        ArenaGameId.SHOLO_GUTI -> if (isBn) "বাংলার ঐতিহ্যবাহী ১৬ গুটি • ৩৭ পয়েন্ট, চেইন ক্যাপচার ও মাল্টিপ্লেয়ার" else "Iconic South Asian 16 Beads • 37-Node Board, Chain Captures & Bot"
        ArenaGameId.CHESS -> if (isBn) "সম্পূর্ণ আন্তর্জাতিক নিয়ম • ক্যাসলিং, এন পাসাঁ ও Alpha-Beta বট" else "Full Rules + Castling, En Passant & Grandmaster Bot"
        ArenaGameId.FRUIT_SLASH -> if (isBn) "স্মুথ ফ্লুইড ফিজিক্স • তরমুজ, আনারস ও মাল্টি-কম্বো স্লাইসিং" else "Smooth Fluid Physics • Watermelon, Pineapple & Multi-Combo Slicing"
        ArenaGameId.WAR_LAND -> if (isBn) "লো-পলি ওপেন ওয়ার্ল্ড • থার্ড পার্সন ভিউ, বিল্ডিং স্নাইপিং ও লাইভ বটস" else "Low-Poly Open World • 3rd Person View, Rooftop Sniping & Live Bots"
    }

    fun gameBadge(id: ArenaGameId): String = when (id) {
        ArenaGameId.LUDO -> if (isBn) "২–৫P হট" else "2–5P HOT"
        ArenaGameId.SNAKES_LADDERS -> if (isBn) "৩D ক্লাসিক" else "3D CLASSIC"
        ArenaGameId.CARROM -> if (isBn) "৩D রিয়েল" else "3D REALISTIC"
        ArenaGameId.SHOLO_GUTI -> if (isBn) "১৬ গুটি" else "16 BEADS"
        ArenaGameId.CHESS -> if (isBn) "স্ট্র্যাটেজি" else "STRATEGY"
        ArenaGameId.FRUIT_SLASH -> if (isBn) "৩D আর্কেড" else "3D ARCADE"
        ArenaGameId.WAR_LAND -> if (isBn) "৩D অ্যাকশন" else "3D ACTION"
    }

    fun bannerResFor(id: ArenaGameId): Int = when (id) {
        ArenaGameId.LUDO -> R.drawable.banner_ludo_3d
        ArenaGameId.CARROM -> R.drawable.banner_carrom_3d
        ArenaGameId.SNAKES_LADDERS -> R.drawable.banner_snakes_ladders_3d
        ArenaGameId.SHOLO_GUTI -> R.drawable.img_sholo_guti_banner_1795521318541
        ArenaGameId.CHESS -> R.drawable.banner_chess_3d
        ArenaGameId.FRUIT_SLASH -> R.drawable.banner_fruit_slash_3d
        ArenaGameId.WAR_LAND -> R.drawable.banner_warland_3d
    }

    fun iconResFor(id: ArenaGameId): Int = when (id) {
        ArenaGameId.LUDO -> R.drawable.img_icon_ludo_3d_1795491541946
        ArenaGameId.SNAKES_LADDERS -> R.drawable.img_icon_snakes_3d_1795491533200
        ArenaGameId.CARROM -> R.drawable.img_icon_carrom_3d_1795491534374
        ArenaGameId.SHOLO_GUTI -> R.drawable.img_sholo_guti_banner_1795521318541
        ArenaGameId.CHESS -> R.drawable.img_icon_chess_3d_1795491533234
        ArenaGameId.FRUIT_SLASH -> R.drawable.img_icon_fruitslash_3d_1795491537123
        ArenaGameId.WAR_LAND -> R.drawable.banner_warland_3d
    }

    fun botTier(accuracy: Int): String = when {
        accuracy < 30 -> if (isBn) "সাধারণ ($accuracy%)" else "Casual ($accuracy%)"
        accuracy < 55 -> if (isBn) "ভারসাম্যপূর্ণ ($accuracy%)" else "Balanced ($accuracy%)"
        accuracy < 80 -> if (isBn) "কৌশলী ($accuracy%)" else "Tactical ($accuracy%)"
        accuracy < 94 -> if (isBn) "দক্ষ ($accuracy%)" else "Master ($accuracy%)"
        else -> if (isBn) "গ্র্যান্ডমাস্টার ($accuracy%)" else "Grandmaster ($accuracy%)"
    }

    fun ludoColorName(color: LudoColor): String = when (color) {
        LudoColor.RED -> if (isBn) "পান্না" else "Emerald"
        LudoColor.GREEN -> if (isBn) "নীলকান্ত" else "Sapphire"
        LudoColor.YELLOW -> if (isBn) "সোনালি" else "Gold"
        LudoColor.BLUE -> if (isBn) "রক্তিম" else "Crimson"
        LudoColor.PURPLE -> if (isBn) "বেগুনি" else "Amethyst"
    }

    fun ruleTaglineAndSections(id: ArenaGameId): Pair<String, List<GameRuleSection>> = when (id) {
        ArenaGameId.LUDO -> if (isBn) {
            "রয়্যাল লুডু • ২ থেকে ৫ জন প্লেয়ার (৫-আর্ম স্টার বোর্ড)" to listOf(
                GameRuleSection(
                    "মূল লক্ষ্য",
                    listOf(
                        "২–৪ প্লেয়ারে ক্লাসিক ৫২-ঘরের বোর্ড এবং ৫ প্লেয়ারে এক্সক্লুসিভ ৬৫-ঘরের ৫-আর্ম স্টার বোর্ড অটোমেটিক চালু হয়।",
                        "আপনার ৪টি গুটি ঘড়ির কাঁটার দিকে পুরো ট্র্যাক ঘুরে নিজের রঙের হোম লেন দিয়ে সেন্টার ক্রাউনে পৌঁছান।",
                        "যে প্লেয়ার সবার আগে ৪টি গুটি সেন্টার ক্রাউনে নিয়ে যাবে সেই তাৎক্ষণিক বিজয়ী হবে।"
                    )
                ),
                GameRuleSection(
                    "ডাইস ও বোনাস রোল",
                    listOf(
                        "হোম ইয়ার্ড থেকে গুটি বের করতে ৬ (Six) ফেলতে হয়, অথবা সক্রিয় গুটি ৬ ঘর এগোতে হয়।",
                        "৬ পড়লে, বিপক্ষের গুটি কাটলে অথবা সেন্টার ক্রাউনে পৌঁছালে সাথে সাথে একটি বোনাস রোল পাবেন।",
                        "টানা ৩ বার ৬ পড়লে ৩য় চাল বাতিল হয়ে পরবর্তী প্লেয়ারের টার্ন চলে যায়।"
                    )
                ),
                GameRuleSection(
                    "ক্যাপচার ও সেফ স্টার (★)",
                    listOf(
                        "নরমাল ঘরে বিপক্ষের একক গুটির উপর নামলে সেটি হোম ইয়ার্ডে ফেরত যাবে।",
                        "স্টার্ট ঘর এবং স্টার (★) চিহ্নিত ঘর নিরাপদ (Safe Zone) — ৫ প্লেয়ার বোর্ডে ১০টি সেফ স্টার রয়েছে!",
                        "সেন্টার ক্রাউনে ঢুকতে একদম সঠিক ডাইস সংখ্যা প্রয়োজন।"
                    )
                ),
                GameRuleSection(
                    "বট অ্যাকুরেসি",
                    listOf(
                        "অ্যাকুরেসি যত বাড়াবেন, বট তত কৌশলী হয়ে প্রতিপক্ষের গুটি কাটা, সেফ স্টারে থাকা এবং দ্রুত হোম লেনে ঢোকার চেষ্টা করবে।"
                    )
                )
            )
        } else {
            "Royal Ludo • 2 to 5 Players (Exclusive 5-Arm Star Board)" to listOf(
                GameRuleSection(
                    "Objective",
                    listOf(
                        "2–4P plays on the classic 52-square board; selecting 5P unlocks the symmetrical 65-square 5-Arm Pentagonal Star board with 5th player Amethyst!",
                        "Guide all 4 tokens clockwise around the outer ring and up your colored Home Stretch into the Center Crown.",
                        "First player to bring all 4 tokens safely home wins immediately."
                    )
                ),
                GameRuleSection(
                    "Dice & Bonus Rolls",
                    listOf(
                        "Roll a 6 to deploy a token from your home yard onto your start square, or move an active token 6 steps.",
                        "Rolling a 6, capturing an opponent token, or crowning a token grants an instant bonus roll.",
                        "Rolling three 6s in a row forfeits the third move and passes the turn."
                    )
                ),
                GameRuleSection(
                    "Captures & Safe Stars",
                    listOf(
                        "Landing on an opponent token on a standard square knocks it back to its yard.",
                        "Start squares and Star (★) squares are safe havens (8 stars in 4P, 10 stars in 5P) — no captures occur there.",
                        "Exact dice roll is required to enter the Center Crown."
                    )
                )
            )
        }

        ArenaGameId.CHESS -> if (isBn) {
            "৬৪-ঘরের রয়্যাল দাবা • সম্পূর্ণ আন্তর্জাতিক নিয়ম" to listOf(
                GameRuleSection(
                    "মূল নিয়ম ও স্পেশাল চাল",
                    listOf(
                        "সাদা (White) সবার আগে চাল দেয়। যেকোনো ঘুঁটিতে ট্যাপ করলে বৈধ চালগুলো গোল্ড ডট (সাধারণ) ও লাল রিং (ক্যাপচার) হিসেবে দেখা যাবে।",
                        "Castling (O-O / O-O-O): রাজা ও নৌকা আগে না নড়ে থাকলে এবং মাঝের ঘর ফাঁকা ও নিরাপদ থাকলে রাজা ২ ঘর সরিয়ে ক্যাসলিং করা যায়।",
                        "En Passant: প্রতিপক্ষের সৈন্য ২ ঘর এগোলে ঠিক পরের চালেই পাশের সৈন্য দিয়ে তির্যকভাবে কেটে নেওয়া যায়।",
                        "Pawn Promotion: সৈন্য ৮ম সারিতে পৌঁছালে রানী, নৌকা, গজ বা ঘোড়ায় উন্নীত করার ডায়ালগ আসবে।"
                    )
                ),
                GameRuleSection(
                    "চেক, চেকমেট ও ড্র",
                    listOf(
                        "রাজার উপর আক্রমণ (Check) থাকলে লাল রঙের হ্যালো দেখাবে — এমন কোনো চাল দেওয়া যাবে না যাতে রাজা চেকে থাকে।",
                        "চেকমেট হলে খেলা সাথে সাথে শেষ হয়। স্টেলমেট, ৫০-চাল নিয়ম বা অপর্যাপ্ত ঘুঁটিতে খেলা ড্র হয়।"
                    )
                ),
                GameRuleSection(
                    "Alpha-Beta বট বুদ্ধিমত্তা",
                    listOf(
                        "১০%–৩৫%: সহজ ও পরীক্ষামূলক চাল। ৪৫%–৭৫%: কৌশলী ২-প্লাই বিশ্লেষণ। ৮০%–১০০%: ৩-প্লাই Alpha-Beta প্রুনিং ও পজিশনাল কন্ট্রোল।"
                    )
                )
            )
        } else {
            "Royal 64-Square Strategy • Official FIDE Rules" to listOf(
                GameRuleSection(
                    "Core Rules & Special Moves",
                    listOf(
                        "White moves first. Tap any piece to highlight legal targets (gold dot = quiet move, crimson ring = capture).",
                        "Castling (O-O / O-O-O): Move the King 2 squares toward an unmoved Rook across empty, unattacked squares.",
                        "En Passant: Capture an enemy pawn immediately after it advances two squares past your pawn.",
                        "Pawn Promotion: Reaching the 8th rank prompts promotion to Queen, Rook, Bishop, or Knight."
                    )
                ),
                GameRuleSection(
                    "Check, Checkmate & Draws",
                    listOf(
                        "A crimson halo marks a King in Check. Checkmate ends the game immediately.",
                        "Stalemate, 50-move rule without pawn moves/captures, or insufficient material result in a Draw."
                    )
                ),
                GameRuleSection(
                    "Alpha-Beta Bot Intelligence",
                    listOf(
                        "10%–35%: Casual. 45%–75%: Tactical 2-ply lookahead. 80%–100%: 3-ply Alpha-Beta Pruning with positional control."
                    )
                )
            )
        }

        ArenaGameId.SHOLO_GUTI -> if (isBn) {
            "ষোল গুটি 3D • বাংলার ঐতিহ্যবাহী ৩৭-পয়েন্ট স্ট্র্যাটেজি গেম" to listOf(
                GameRuleSection(
                    "বোর্ড ও ১৬ বনাম ১৬ গুটি",
                    listOf(
                        "মোট ৩৭টি ইন্টারসেকশন (মাঝের ৫×৫ স্কয়ার + উপরে ও নিচে ২টি ত্রিভুজ উইং) রয়েছে।",
                        "রুবি (১৬টি গুটি) ও গোল্ড (১৬টি গুটি) মুখোমুখি বসে, মাঝের সারিটি শুরুতে ফাঁকা থাকে।"
                    )
                ),
                GameRuleSection(
                    "চাল, ক্যাপচার ও মাল্টি-জাম্প",
                    listOf(
                        "লাইন বরাবর পাশের যেকোনো ফাঁকা পয়েন্টে ১ ঘর সরানো যায়।",
                        "সরলরেখায় প্রতিপক্ষের গুটির ঠিক পরের পয়েন্ট ফাঁকা থাকলে লাফ দিয়ে গুটিটি কেটে নেওয়া যায়।",
                        "একটি গুটি কাটার পর ওই একই গুটি দিয়ে আরও কাটার সুযোগ থাকলে একটানা চেইন জাম্প ক্যাপচার করা যায়!"
                    )
                ),
                GameRuleSection(
                    "বিজয় ও রিওয়ার্ড",
                    listOf(
                        "প্রতিপক্ষের সব ১৬টি গুটি কেটে ফেললে অথবা প্রতিপক্ষের কোনো বৈধ চাল না থাকলে তাৎক্ষণিক জয়!",
                        "অফলাইন, স্মার্ট এআই বট, অনলাইন ফ্রেন্ডস রুম এবং Play & Earn টুর্নামেন্টে খেলা যাবে।"
                    )
                )
            )
        } else {
            "Sholo Guti 3D • Traditional 37-Point 16 Beads Strategy" to listOf(
                GameRuleSection(
                    "Board & 16v16 Setup",
                    listOf(
                        "Authentic 37 intersections (central 5×5 diagonal grid plus top and bottom triangular citadels).",
                        "16 Ruby beads start on bottom vs 16 Gold beads on top, with the middle row open."
                    )
                ),
                GameRuleSection(
                    "Slides & Chain Captures",
                    listOf(
                        "Slide one step along any connected board line to an empty adjacent intersection.",
                        "Jump in a straight line over an adjacent enemy bead into the empty point immediately beyond to capture it.",
                        "Multi-jump chains let a single bead keep capturing consecutively in one turn!"
                    )
                ),
                GameRuleSection(
                    "Victory & Game Coins",
                    listOf(
                        "Win by capturing all 16 enemy beads or blocking all opponent moves.",
                        "Online Multiplayer & Friend Rooms award 3x Game Coins!"
                    )
                )
            )
        }

        ArenaGameId.WAR_LAND -> if (isBn) {
            "ওয়ার ল্যান্ড • ৩ডি লো-পলি ব্যাটল রয়্যাল ও ওপেন ওয়ার্ল্ড এরিনা" to listOf(
                GameRuleSection(
                    "গেমপ্লে ও কন্ট্রোল",
                    listOf(
                        "থার্ড পার্সন ক্যামেরা ও ফুলস্ক্রিন ল্যান্ডস্কেপ মোড। জয়স্টিক দিয়ে মুভ করুন এবং ফায়ার বাটনে শুট করুন।",
                        "জাম্প, ক্রাউচ ও প্রোন বাটন দিয়ে শত্রু থেকে লুকান। স্কোপ অন করে দূরপাল্লার স্নাইপিং করুন।",
                        "বিল্ডিংয়ের ভেতরে প্রবেশ করে সিঁড়ি দিয়ে প্রতি ফ্লোর ও ছাদে উঠে উপর থেকে শত্রুদের আক্রমণ করুন।"
                    )
                ),
                GameRuleSection(
                    "লুটিং ও কমব্যাট",
                    listOf(
                        "ম্যাপের বিভিন্ন রুমে ছড়িয়ে থাকা AK-47, স্নাইপার, শটগান, পিস্তল, বুলেট, গ্রেনেড ও মেডিকেট সংগ্রহ করুন।",
                        "এনিমিকে হত্যা করলে তাদের গোল্ডেন ডেথ বক্স ড্রপ হবে যা লুট করা যাবে। প্যারাসুটে ড্রপ হওয়া এয়ারড্রপ থেকে লেজেন্ডারি অস্ত্র পান।",
                        "গ্রেনেড ছুড়ে বিস্ফোরণ ঘটান এবং মেডিকেট দিয়ে HP রিকভার করুন।"
                    )
                ),
                GameRuleSection(
                    "মোড ও রিওয়ার্ড",
                    listOf(
                        "অফলাইনে ২৫টি স্মার্ট বটের সাথে খেলুন অথবা ফ্রেন্ডসদের সাথে লোকাল ওয়াই-ফাই/হটস্পটে মিনি মিলিশিয়া স্টাইলে খেলুন।",
                        "অনলাইন ফ্রি প্লেতে প্রতি কিল ও সারভাইভালের জন্য সরাসরি ওয়ালেটে কয়েন যোগ হবে।",
                        "টুর্নামেন্ট খেলে বিজয়ী হলে প্রাইজ পুলের অংশ ওয়ালেটে জমা হবে।"
                    )
                )
            )
        } else {
            "War Land • 3D Low-Poly Battle Royale & Open World Arena" to listOf(
                GameRuleSection(
                    "Controls & Navigation",
                    listOf(
                        "Third-Person full body camera in immersive Landscape mode. Use virtual joystick to move and target button to fire.",
                        "Use Jump, Crouch, and Prone to evade enemy bullets. Toggle Scope Aim for long-range precision sniping.",
                        "Enter multi-story buildings, climb interior stairs, and reach rooftops to dominate high-ground vantage points."
                    )
                ),
                GameRuleSection(
                    "Looting & Ballistics",
                    listOf(
                        "Scavenge rooms and hangars for AK-47, Sniper Rifle, Shotguns, Pistols, Ammo, Medkits, and Grenades.",
                        "Eliminated enemies drop golden Death Loot Crates. Watch for parachuting Airdrop beacons with elite supplies.",
                        "Cook and throw explosive grenades with trajectory arcs, and use Medkits to regenerate HP."
                    )
                ),
                GameRuleSection(
                    "Modes & Rewards",
                    listOf(
                        "Play Solo offline against 25 smart tactical bots or host a local Wi-Fi / Hotspot room with friends (Mini Militia style).",
                        "Free Play matches reward game coins based on kills and survival rank directly into your Play Wallet.",
                        "Compete in live Esports tournaments for rank prize pools and per-kill bounties."
                    )
                )
            )
        }

        ArenaGameId.CARROM -> if (isBn) {
            "ক্যারম বোর্ড 3D • প্রফেশনাল ও টুর্নামেন্ট খেলার নিয়মাবলী" to listOf(
                GameRuleSection(
                    "বোর্ড সেটআপ ও পয়েন্ট সিস্টেম",
                    listOf(
                        "বোর্ডে মোট ১৯টি ঘুঁটি থাকে: ৯টি সাদা (প্রতিটি ১০ পয়েন্ট), ৯টি কালো (প্রতিটি ৫ পয়েন্ট) এবং ১টি লাল কুইন (২৫ পয়েন্ট)।",
                        "প্লেয়ার ১ (হোয়াইট) নিচের বেসলাইন থেকে এবং প্লেয়ার ২/বট (ব্ল্যাক) উপরের বেসলাইন থেকে স্ট্রাইকার চালায়।",
                        "স্ট্রাইকার বেসলাইনের ভেতরে যেকোনো সুবিধাজনক অবস্থানে ড্র্যাগ করে স্থাপন করা যায়।"
                    )
                ),
                GameRuleSection(
                    "কুইন (রানী) ও কাভার রুলস",
                    listOf(
                        "লাল কুইন বোর্ডে থাকা অবস্থায় পকেট করা যাবে। কুইন পকেট করার পর অবশ্যই অবিলম্বে আরেকটি নিজস্ব রঙের কয়েন পকেট করে কাভার দিতে হবে।",
                        "একই চালে বা পরবর্তী চালে কাভার দিতে ব্যর্থ হলে কুইন পুনরায় বোর্ডের সেন্টারে ফেরত আসবে।",
                        "সফলভাবে কাভার দিলে কুইনের ২৫ পয়েন্ট প্লেয়ারের স্কোরে যোগ হবে।"
                    )
                ),
                GameRuleSection(
                    "ফাউল পেনাল্টি ও মোডস",
                    listOf(
                        "স্ট্রাইকার পকেটে চলে গেলে ফাউল হবে; পূর্ববর্তী পকেট করা একটি কয়েন বোর্ডে ফেরত আসবে এবং প্রতিপক্ষের চাল আসবে।",
                        "ফ্রি প্র্যাকটিস মোডে কোনো কয়েন ছাড়াই আনলিমিটেড শট খেলা যাবে।",
                        "অফলাইন ২-প্লেয়ারে এক ফোনে দুজন খেলা যাবে এবং টুর্নামেন্ট মোডে ওয়ালেট প্রাইজ উইন করা যাবে।"
                    )
                )
            )
        } else {
            "Carrom Board 3D • Professional & Tournament Rules" to listOf(
                GameRuleSection(
                    "Board Setup & Points",
                    listOf(
                        "19 Carrom Men in total: 9 White (10 pts each), 9 Black (5 pts each), and 1 Red Queen (25 pts).",
                        "Player 1 (White) strikes from bottom baseline, Player 2 / Bot (Black) from top baseline.",
                        "Drag striker horizontally along the baseline to find your optimal angle."
                    )
                ),
                GameRuleSection(
                    "Queen & Cover Rules",
                    listOf(
                        "The Red Queen can be pocketed at any time, but must be 'Covered' by pocketing another coin of your color.",
                        "If you fail to cover the Queen on the same turn or immediate next shot, Queen returns to center.",
                        "Covering the Queen awards +25 bonus points."
                    )
                ),
                GameRuleSection(
                    "Fouls & Play Modes",
                    listOf(
                        "Sinking the striker incurs a foul penalty: 1 previously pocketed coin is returned to board.",
                        "Free Practice mode allows infinite trick shots and aim training with zero entry fee.",
                        "Play offline pass & play, vs Smart Bot, or enter real Tournament tiers for coin prizes."
                    )
                )
            )
        }

        ArenaGameId.SNAKES_LADDERS -> if (isBn) {
            "সাপ-লুডু 3D • ক্লাসিক ১০০-ঘরের বোর্ড ও খেলার নিয়মাবলী" to listOf(
                GameRuleSection(
                    "মূল নিয়ম ও লক্ষ্য",
                    listOf(
                        "বোর্ডে ১ থেকে ১০০ পর্যন্ত ঘর রয়েছে। ১ নম্বর ঘর থেকে শুরু করে সবার আগে ১০০ নম্বর ঘরে পৌঁছানোই মূল লক্ষ্য।",
                        "প্রত্যেক প্লেয়ার তাদের নির্ধারিত কর্নার থেকে ডাইস ক্লিক করে চাল দেবেন। ৬ পড়লে অতিরিক্ত আরেকটি চাল পাওয়া যাবে।"
                    )
                ),
                GameRuleSection(
                    "মই ও সাপ",
                    listOf(
                        "কোনো গুটি মইয়ের নিচের ঘরে পৌঁছালে সরাসরি মই বেয়ে ওপরের ঘরে উঠে যাবে।",
                        "কোনো গুটি সাপের মুখে পড়লে সরাসরি সাপের লেজের নিচের ঘরে নেমে যাবে।",
                        "১০০ ঘরে পৌঁছানোর জন্য অবিকল সঠিক সংখ্যা ডাইসে পড়তে হবে।"
                    )
                ),
                GameRuleSection(
                    "৫টি গেম মোড",
                    listOf(
                        "অফলাইন পাস অ্যান্ড প্লে, স্মার্ট রোবট এআই ম্যাচ, বন্ধুদের সাথে প্রাইভেট রুম কোড শেয়ার, অনলাইন মাল্টিপ্লেয়ার ও ওয়ালেট টুর্নামেন্ট।"
                    )
                )
            )
        } else {
            "Snakes & Ladders 3D • Rules & Gameplay" to listOf(
                GameRuleSection(
                    "Objective & Movement",
                    listOf(
                        "The board consists of 100 squares from 1 to 100. First player to reach square 100 wins.",
                        "Each player rolls their dedicated 3D corner dice on their turn. Rolling a 6 grants a bonus turn."
                    )
                ),
                GameRuleSection(
                    "Snakes & Ladders",
                    listOf(
                        "Landing on a ladder bottom transports your token directly to the ladder top.",
                        "Landing on a snake head slides your token down to the snake tail.",
                        "Exact roll required to reach tile 100."
                    )
                ),
                GameRuleSection(
                    "5 Game Modes",
                    listOf(
                        "Supports Offline Pass & Play, Smart Bot AI, Private Rooms with Friends, Online Multiplayer, and Coin Tournaments."
                    )
                )
            )
        }

        ArenaGameId.FRUIT_SLASH -> if (isBn) {
            "ফ্রুট স্ল্যাশ 3D • নিনজা আর্কেড ও টুর্নামেন্ট নিয়মাবলী" to listOf(
                GameRuleSection(
                    "স্লাইসিং ও স্কোরিং",
                    listOf(
                        "স্ক্রিনে সোয়াইপ করে বাতাসে ওড়া তাজা তরমুজ, কমলা, আপেল ও আনারস কেটে ফেলুন।",
                        "এক সোয়াইপে ৩টি বা তার বেশি ফল কাটলে স্পেশাল কম্বো বোনাস স্কোর পাওয়া যায়।",
                        "কালো বোমা (Bomb) ভুলেও কাটবেন না! বোমা কাটলে তাৎক্ষণিক ম্যাচ শেষ হয়ে যাবে।"
                    )
                ),
                GameRuleSection(
                    "লাইভস ও স্পিড",
                    listOf(
                        "ক্লাসিক আর্কেড মোডে মোট ৩টি হার্ট/লাইফ থাকে। না কেটে ফল নিচে পড়ে গেলে ১টি করে লাইফ নষ্ট হবে।",
                        "ফিজিক্স স্পিড অপ্টিমাইজড ও স্মুথ ২.০ সেকেন্ড হ্যাং-টাইমে তৈরি, যা খেলাকে উপভোগ্য করে।"
                    )
                ),
                GameRuleSection(
                    "মোডস ও টুর্নামেন্ট",
                    listOf(
                        "প্র্যাকটিস মোড, রোবট স্কোর চ্যালেঞ্জ, ফ্রেন্ড ডুয়েল রুম ও স্কোর-ভিত্তিক ওয়ালেট টুর্নামেন্ট।"
                    )
                )
            )
        } else {
            "Fruit Slash 3D • Rules & Combat" to listOf(
                GameRuleSection(
                    "Slicing & Scoring",
                    listOf(
                        "Swipe across screen to slice flying 3D fruits in half with fluid juice splash particles.",
                        "Slice 3 or more fruits in a single swipe to trigger Combo Multipliers.",
                        "Avoid slicing explosive bombs! Slicing a bomb detonates and ends the match."
                    )
                ),
                GameRuleSection(
                    "Lives & Physics",
                    listOf(
                        "Classic mode features 3 lives. Dropping an unsliced fruit loses 1 heart.",
                        "Balanced floaty physics with 2.0s hang-time for responsive and accurate slicing."
                    )
                ),
                GameRuleSection(
                    "5 Game Modes",
                    listOf(
                        "Supports Practice, AI Bot High-score Duel, Private Friend Rooms, Online Matches, and Cash Tournaments."
                    )
                )
            )
        }
    }
}

fun gameStringsFor(language: AppLanguage): GameUiStrings = if (language == AppLanguage.BANGLA) {
    GameUiStrings(
        isBn = true,
        arenaBannerTitle = "✦ Play & Earn • ডুয়াল ওয়ালেট এরিনা ✦",
        arenaBannerDesc = "টুর্নামেন্টে হারলেও এন্ট্রি ফি ১০০% গেম ওয়ালেটে ক্যাশব্যাক! অফলাইন বট বা অনলাইন ফ্রেন্ডসদের সাথে খেলুন।",
        arenaHeaderSubtitle = "৭টি প্রিমিয়াম ৩D গেম • অনলাইন ফ্রেন্ডস • ডুয়াল ওয়ালেট ও আর্মরি",
        playNow = "এখনই খেলুন",
        rules = "নিয়ম",
        newMatch = "নতুন ম্যাচ",
        soundOn = "সাউন্ড চালু",
        soundOff = "মিউট",
        playVsBot = "বটের সাথে খেলুন",
        botAccuracy = "বট অ্যাকুরেসি",
        playersLabel = "প্লেয়ার:",
        botSuffix = "(বট)",
        undoMove = "চাল ফেরত নিন",
        playAnotherMatch = "আরেকটি ম্যাচ খেলুন",
        winsSuffix = "জয়ী!",
        rollDice = "ডাইস রোল করুন",
        rollingDice = "ডাইস ঘুরছে...",
        ludoRollPrompt = "ডাইস রোল করতে ট্যাপ করুন",
        ludoSafeStarTip = "★ সেফ স্টার • সঠিক সংখ্যায় ক্রাউনে প্রবেশ",
        promotePawnTitle = "সৈন্য পদোন্নতি (Promotion)",
        whiteTurn = "সাদা পক্ষের চাল",
        blackTurn = "কালো পক্ষের চাল",
        botThinking = "বট ভাবছে...",
        checkmateWhiteWins = "🏆 চেকমেট! সাদা পক্ষ জয়ী!",
        checkmateBlackWins = "🏆 চেকমেট! কালো পক্ষ জয়ী!",
        drawStalemate = "স্টেলমেট — খেলা ড্র",
        drawFiftyMove = "ড্র (৫০-চাল নিয়ম)",
        drawInsufficient = "ড্র (অপর্যাপ্ত ঘুঁটি)",
        checkTag = "চেক!",
        capturedLabel = "ক্যাপচার:",
        movesLabel = "চাল:",
        advantageLabel = "অগ্রগতি:",
        leaderboardTitle = "এরিনা লিডারবোর্ড",
        leaderboardSubtitle = "লোকাল রুম ডাটাবেজে সংরক্ষিত সর্বোচ্চ স্কোর ও জয়ের হার",
        totalMatchesLabel = "মোট ম্যাচ",
        winRateLabel = "জয়ের হার",
        highScoreLabel = "সর্বোচ্চ স্কোর",
        bestStreakLabel = "সেরা স্ট্রিক",
        maxBotBeatenLabel = "সেরা বট জয়",
        winsShort = "জয়",
        lossesShort = "হার",
        drawsShort = "ড্র",
        noMatchesYet = "এখনো কোনো ম্যাচ খেলা হয়নি — প্রথম ম্যাচ জিতে রেকর্ড গড়ুন!",
        resetLeaderboard = "রিসেট করুন",
        resetLeaderboardConfirm = "সব গেমের লিডারবোর্ড ও স্কোর স্থায়ীভাবে মুছে ফেলতে চান?",
        matchScoreRecorded = "ম্যাচ স্কোর সংরক্ষিত",
        liveLanguageBadge = "বাংলা",
        tabArena = "এরিনা",
        tabWallets = "ওয়ালেট",
        tabArmory = "আর্মরি শপ",
        tournamentWalletTitle = "টুর্নামেন্ট ওয়ালেট",
        gameWalletTitle = "গেম ওয়ালেট",
        tournamentWalletHint = "ডিপোজিট, টুর্নামেন্ট এন্ট্রি ও উইথড্রয়াল",
        gameWalletHint = "১০০% লস ক্যাশব্যাক ও স্কিন/স্কিল কেনাকাটা",
        depositLabel = "ডিপোজিট",
        withdrawLabel = "উইথড্র",
        transferToGameLabel = "গেম ওয়ালেটে নিন",
        oneWayTransferNote = "টুর্নামেন্ট → গেম ওয়ালেট (একমুখী ট্রান্সফার)",
        reverseTransferLocked = "গেম → টুর্নামেন্ট ট্রান্সফার বন্ধ",
        lossCashbackBanner = "টুর্নামেন্টে হারলেও ১০০% এন্ট্রি ফি গেম ওয়ালেটে ক্যাশব্যাক!",
        liveChatLabel = "লাইভ চ্যাট",
        onlineFriendsTitle = "অনলাইন মাল্টিপ্লেয়ার ও ফ্রেন্ডস",
        onlineSignInPrompt = "অনলাইন ফ্রেন্ডস ও কুইক ম্যাচ খেলতে Google অ্যাকাউন্ট দিয়ে সাইন ইন করুন।",
        signInWithGoogleLabel = "Google দিয়ে সাইন ইন করুন",
        createPrivateRoomLabel = "ফ্রেন্ড রুম বানান (কোড)",
        quickMatchLabel = "কুইক ম্যাচ",
        joinByCodeLabel = "কোড দিয়ে যোগ দিন",
        enterRoomCodeHint = "৬ অক্ষরের রুম কোড",
        waitingForFriendLabel = "ফ্রেন্ডের অপেক্ষায়...",
        shareCodeLabel = "কোড শেয়ার করুন",
        yourTurnBadge = "আপনার চাল",
        opponentTurnBadge = "প্রতিপক্ষের চাল",
        tournamentEntryLabel = "টুর্নামেন্ট এন্ট্রি ফি:",
        freePlayLabel = "ফ্রি",
        armoryShopTitle = "গেম আর্মরি শপ",
        armoryShopSubtitle = "শুধুমাত্র গেম ওয়ালেট কয়েন দিয়ে বোর্ড স্কিন, অ্যাভাটার ও স্কিল কিনুন",
        boardSkinsTab = "বোর্ড স্কিন",
        avatarsTab = "অ্যাভাটার",
        skillsTab = "ইন-গেম স্কিল",
        buyWithGameCoins = "কিনুন",
        equipLabel = "ব্যবহার করুন",
        equippedLabel = "চালু আছে",
        ownedLabel = "কেনা আছে",
        chargesCountLabel = "চার্জ",
        useSkillLabel = "স্কিল ব্যবহার",
        cashbackReceivedTitle = "১০০% লস ক্যাশব্যাক! গেম ওয়ালেটে যোগ হয়েছে",
        prizeWonTitle = "টুর্নামেন্ট প্রাইজ টুর্নামেন্ট ওয়ালেটে যোগ হয়েছে!",
        entryRefundedTitle = "ড্র! এন্ট্রি ফি টুর্নামেন্ট ওয়ালেটে ফেরত এসেছে"
    )
} else {
    GameUiStrings(
        isBn = false,
        arenaBannerTitle = "✦ Play & Earn • Dual Wallet Arena ✦",
        arenaBannerDesc = "Lose a paid tournament? 100% of your entry fee goes straight into your Game Wallet! Play Bots or Online Friends.",
        arenaHeaderSubtitle = "7 Pro 3D Games • Online Friends • Dual Wallets & Armory",
        playNow = "Play Now",
        rules = "Rules",
        newMatch = "New Match",
        soundOn = "Sound On",
        soundOff = "Muted",
        playVsBot = "Play vs Bot",
        botAccuracy = "Bot Accuracy",
        playersLabel = "Players:",
        botSuffix = "(Bot)",
        undoMove = "Undo Move",
        playAnotherMatch = "Play Another Match",
        winsSuffix = "Wins!",
        rollDice = "Roll Dice",
        rollingDice = "Rolling...",
        ludoRollPrompt = "Tap dice to roll",
        ludoSafeStarTip = "★ Safe Star • Exact roll enters Crown",
        promotePawnTitle = "Promote Pawn",
        whiteTurn = "White's Turn",
        blackTurn = "Black's Turn",
        botThinking = "Bot Thinking...",
        checkmateWhiteWins = "🏆 Checkmate! White Wins",
        checkmateBlackWins = "🏆 Checkmate! Black Wins",
        drawStalemate = "Draw by Stalemate",
        drawFiftyMove = "Draw (50-Move Rule)",
        drawInsufficient = "Draw (Insufficient Material)",
        checkTag = "CHECK!",
        capturedLabel = "Captured:",
        movesLabel = "Moves:",
        advantageLabel = "Advantage:",
        leaderboardTitle = "Arena Leaderboard",
        leaderboardSubtitle = "High scores & win rates persisted locally via Room",
        totalMatchesLabel = "Matches",
        winRateLabel = "Win Rate",
        highScoreLabel = "High Score",
        bestStreakLabel = "Best Streak",
        maxBotBeatenLabel = "Top Bot",
        winsShort = "W",
        lossesShort = "L",
        drawsShort = "D",
        noMatchesYet = "No matches recorded yet — finish a game to claim your rank!",
        resetLeaderboard = "Reset",
        resetLeaderboardConfirm = "Clear all local high scores and game leaderboard records?",
        matchScoreRecorded = "Score Saved",
        liveLanguageBadge = "EN",
        tabArena = "Arena",
        tabWallets = "Wallets",
        tabArmory = "Armory",
        tournamentWalletTitle = "Tournament Wallet",
        gameWalletTitle = "Game Wallet",
        tournamentWalletHint = "Deposits, Tournament Entry & Withdrawals",
        gameWalletHint = "100% Loss Cashback • Buy Skins, Avatars & Skills",
        depositLabel = "Deposit",
        withdrawLabel = "Withdraw",
        transferToGameLabel = "Move to Game",
        oneWayTransferNote = "Tournament → Game Wallet (One-Way Only)",
        reverseTransferLocked = "Game → Tournament Locked",
        lossCashbackBanner = "100% Loss Cashback: Lost Tournament Entry goes directly into your Game Wallet!",
        liveChatLabel = "Live Chat",
        onlineFriendsTitle = "Online Multiplayer & Friends",
        onlineSignInPrompt = "Sign in with Google to host Room Code invites or play Quick Match with friends.",
        signInWithGoogleLabel = "Sign in with Google",
        createPrivateRoomLabel = "Create Friend Room",
        quickMatchLabel = "Quick Match",
        joinByCodeLabel = "Join by Code",
        enterRoomCodeHint = "6-char Room Code",
        waitingForFriendLabel = "Waiting for opponent...",
        shareCodeLabel = "Share Code",
        yourTurnBadge = "Your Turn",
        opponentTurnBadge = "Opponent's Turn",
        tournamentEntryLabel = "Tournament Entry:",
        freePlayLabel = "Free",
        armoryShopTitle = "Game Armory Shop",
        armoryShopSubtitle = "Spend Game Wallet coins only for Board Skins, Avatars & In-Game Skills",
        boardSkinsTab = "Board Skins",
        avatarsTab = "Avatars",
        skillsTab = "In-Game Skills",
        buyWithGameCoins = "Buy",
        equipLabel = "Equip",
        equippedLabel = "Equipped",
        ownedLabel = "Owned",
        chargesCountLabel = "Charges",
        useSkillLabel = "Use Skill",
        cashbackReceivedTitle = "100% Loss Cashback credited to Game Wallet!",
        prizeWonTitle = "Tournament Prize credited to Tournament Wallet!",
        entryRefundedTitle = "Draw! Entry fee refunded to Tournament Wallet"
    )
}
