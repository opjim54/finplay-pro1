package com.example.domain.games

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.data.local.PlayWalletEntity
import com.example.data.local.WalletPaymentChannel
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class ArmoryItemCategory {
    BOARD_SKIN,
    AVATAR,
    SKILL,
    WAR_CHARACTER,
    WAR_WEAPON_SKIN,
    EMOTE,
    CONSUMABLE_CARD
}

data class BoardSkinPalette(
    val id: String,
    val frameStart: Color,
    val frameEnd: Color,
    val accentBorder: Color,
    val boardLight: Color,
    val boardDark: Color,
    val feltSurface: Color
)

data class ArmoryItem(
    val id: String,
    val category: ArmoryItemCategory,
    val titleEn: String,
    val titleBn: String,
    val descEn: String,
    val descBn: String,
    val priceGameCoins: Double,
    val iconGlyph: String,
    val previewAccentHex: Long,
    val isConsumable: Boolean = false,
    val validityDays: Int? = null // null means permanent, e.g. 7 or 30 days
)

@Serializable
data class RankPrizeTier(
    val rankLabel: String,
    val amount: Double
)

enum class FreeFireMapBanner(
    val key: String,
    val displayName: String,
    val defaultMode: String,
    @DrawableRes val bannerRes: Int
) {
    BERMUDA("BERMUDA", "Bermuda Map", "Battle Royale", R.drawable.img_ff_bermuda),
    CLASH_SQUAD("CLASH_SQUAD", "Clash Squad", "CS 4v4", R.drawable.img_ff_clash_squad),
    CRAFTLAND("CRAFTLAND", "Craftland", "Craftland Custom", R.drawable.img_ff_craftland),
    LONE_WOLF("LONE_WOLF", "Lone Wolf", "1v1 / 2v2 Duel", R.drawable.img_ff_lone_wolf),
    CUSTOM_MAP("CUSTOM_MAP", "Custom Map", "Custom Room", R.drawable.img_ff_custom_map),
    WAR_LAND("WAR_LAND", "War Land 3D", "Battle Royale 3D", R.drawable.img_ff_bermuda);

    companion object {
        fun fromKeyOrMap(bannerKey: String, mapName: String = ""): FreeFireMapBanner {
            val k = bannerKey.trim().uppercase()
            entries.firstOrNull { it.key == k }?.let { return it }
            val m = mapName.trim().lowercase()
            return when {
                m.contains("war") || m.contains("land") -> WAR_LAND
                m.contains("clash") || m.contains("cs") -> CLASH_SQUAD
                m.contains("craft") -> CRAFTLAND
                m.contains("lone") || m.contains("wolf") || m.contains("1v1") -> LONE_WOLF
                m.contains("custom") || m.contains("purgatory") || m.contains("kalahari") -> CUSTOM_MAP
                else -> BERMUDA
            }
        }
    }
}

object TournamentPrizeCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun parseRanks(raw: String): List<RankPrizeTier> {
        if (raw.isBlank() || raw == "[]") return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(RankPrizeTier.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    fun serializeRanks(ranks: List<RankPrizeTier>): String {
        val clean = ranks.filter { it.rankLabel.isNotBlank() && it.amount > 0.0 }
        return json.encodeToString(ListSerializer(RankPrizeTier.serializer()), clean)
    }
}

data class PlayPaymentConfig(
    val bkashNumber: String = "01700-000000",
    val nagadNumber: String = "01800-000000",
    val rocketNumber: String = "01900-000000",
    val binancePayId: String = "FINPLAY-PAY-8899",
    val binanceTrc20Address: String = "TFinPlayProUSDTTRC20Wallet",
    val binanceRateBdt: Double = 120.0,
    val minDeposit: Double = 20.0,
    val minWithdraw: Double = 50.0,
    val bkashDepositEnabled: Boolean = true,
    val nagadDepositEnabled: Boolean = true,
    val rocketDepositEnabled: Boolean = true,
    val binanceDepositEnabled: Boolean = true,
    val bkashWithdrawEnabled: Boolean = true,
    val nagadWithdrawEnabled: Boolean = true,
    val rocketWithdrawEnabled: Boolean = true,
    val binanceWithdrawEnabled: Boolean = true
) {
    fun enabledDepositChannels(): List<WalletPaymentChannel> = buildList {
        if (bkashDepositEnabled) add(WalletPaymentChannel.BKASH)
        if (nagadDepositEnabled) add(WalletPaymentChannel.NAGAD)
        if (rocketDepositEnabled) add(WalletPaymentChannel.ROCKET)
        if (binanceDepositEnabled) add(WalletPaymentChannel.BINANCE)
    }

    fun enabledWithdrawChannels(): List<WalletPaymentChannel> = buildList {
        if (bkashWithdrawEnabled) add(WalletPaymentChannel.BKASH)
        if (nagadWithdrawEnabled) add(WalletPaymentChannel.NAGAD)
        if (rocketWithdrawEnabled) add(WalletPaymentChannel.ROCKET)
        if (binanceWithdrawEnabled) add(WalletPaymentChannel.BINANCE)
    }

    fun isDepositEnabled(channel: WalletPaymentChannel): Boolean = when (channel) {
        WalletPaymentChannel.BKASH -> bkashDepositEnabled
        WalletPaymentChannel.NAGAD -> nagadDepositEnabled
        WalletPaymentChannel.ROCKET -> rocketDepositEnabled
        WalletPaymentChannel.BINANCE -> binanceDepositEnabled
    }

    fun isWithdrawEnabled(channel: WalletPaymentChannel): Boolean = when (channel) {
        WalletPaymentChannel.BKASH -> bkashWithdrawEnabled
        WalletPaymentChannel.NAGAD -> nagadWithdrawEnabled
        WalletPaymentChannel.ROCKET -> rocketWithdrawEnabled
        WalletPaymentChannel.BINANCE -> binanceWithdrawEnabled
    }
}

object PlayArmoryCatalog {
    const val SKIN_ROYAL_EMERALD = "skin_royal_emerald"
    const val SKIN_OBSIDIAN_GOLD = "skin_obsidian_gold"
    const val SKIN_IMPERIAL_RUBY = "skin_imperial_ruby"
    const val SKIN_COSMIC_SAPPHIRE = "skin_cosmic_sapphire"

    const val AVATAR_MONARCH = "avatar_monarch"
    const val AVATAR_KNIGHT = "avatar_knight"
    const val AVATAR_PHOENIX = "avatar_phoenix"
    const val AVATAR_GRANDMASTER = "avatar_grandmaster"

    const val SKILL_CHESS_HINT = "skill_chess_hint"
    const val SKILL_LUDO_REROLL = "skill_ludo_reroll"
    const val SKILL_UNDO_PASS = "skill_undo_pass"
    const val SKILL_FLUX_CHARGE = "skill_flux_charge"

    // War Land Character Outfits
    const val WAR_CHAR_COMMANDO = "war_char_commando"
    const val WAR_CHAR_EVA = "war_char_eva"
    const val WAR_CHAR_SOPHIA = "war_char_sophia"
    const val WAR_CHAR_ZANE = "war_char_zane"
    const val WAR_CHAR_NEON = "war_char_neon"
    const val WAR_CHAR_CRIMSON = "war_char_crimson"
    const val WAR_CHAR_CYBORG = "war_char_cyborg"

    // War Land Weapon Skins & Power-ups
    const val WAR_GUN_DRAGON_AK = "war_gun_dragon_ak"
    const val WAR_GUN_GOLD_AWM = "war_gun_gold_awm"
    const val WAR_GUN_CYBER_SHOTGUN = "war_gun_cyber_shotgun"
    const val WAR_POWER_GLOOWALL = "war_power_gloowall"

    // Emotes
    const val EMOTE_WAVE = "emote_wave"
    const val EMOTE_HEART = "emote_heart"
    const val EMOTE_LAUGH = "emote_laugh"
    const val EMOTE_DANCE = "emote_dance"

    // Utility Cards & Multi-game Skins
    const val CARD_NAME_CHANGE = "card_name_change"
    const val SKIN_LUDO_AMETHYST = "skin_ludo_amethyst"
    const val SKIN_CHESS_CYBERPUNK = "skin_chess_cyberpunk"
    const val SKIN_AETHER_NEBULA = "skin_aether_nebula"

    val TOURNAMENT_ENTRY_TIERS: List<Double> = listOf(0.0, 20.0, 50.0, 100.0, 250.0)

    const val TELEGRAM_SUPPORT_USERNAME = "jim151"
    const val TELEGRAM_APP_URI = "tg://resolve?domain=jim151"
    const val TELEGRAM_WEB_URL = "https://t.me/jim151"

    val items: List<ArmoryItem> = listOf(
        // Board Skins
        ArmoryItem(
            id = SKIN_ROYAL_EMERALD,
            category = ArmoryItemCategory.BOARD_SKIN,
            titleEn = "Royal Emerald Felt",
            titleBn = "রয়্যাল এমারেল্ড বোর্ড",
            descEn = "Classic championship emerald & champagne gold board finish.",
            descBn = "ক্লাসিক চ্যাম্পিয়নশিপ পান্না সবুজ ও গোল্ড বোর্ড থিম।",
            priceGameCoins = 0.0,
            iconGlyph = "✦",
            previewAccentHex = 0xFF059669
        ),
        ArmoryItem(
            id = SKIN_OBSIDIAN_GOLD,
            category = ArmoryItemCategory.BOARD_SKIN,
            titleEn = "Obsidian & 24K Gold",
            titleBn = "অবসিডিয়ান ও ২৪কে গোল্ড",
            descEn = "Midnight matte obsidian frame inlaid with luminous gold accents.",
            descBn = "মিডনাইট অবসিডিয়ান ফ্রেম ও উজ্জ্বল সোনালি বর্ডার ফিনিশ।",
            priceGameCoins = 40.0,
            iconGlyph = "♛",
            previewAccentHex = 0xFFD97706
        ),
        ArmoryItem(
            id = SKIN_IMPERIAL_RUBY,
            category = ArmoryItemCategory.BOARD_SKIN,
            titleEn = "Imperial Ruby Velvet",
            titleBn = "ইম্পেরিয়াল রুবি ভেলভেট",
            descEn = "Regal crimson velvet board with rose-gold highlight trim.",
            descBn = "রাজকীয় ক্রিমসন ভেলভেট বোর্ড ও রোজ-গোল্ড হাইলাইট।",
            priceGameCoins = 60.0,
            iconGlyph = "♦",
            previewAccentHex = 0xFFBE123C
        ),
        ArmoryItem(
            id = SKIN_COSMIC_SAPPHIRE,
            category = ArmoryItemCategory.BOARD_SKIN,
            titleEn = "Cosmic Sapphire Flux",
            titleBn = "কসমিক স্যাফায়ার ফ্লাক্স",
            descEn = "Deep astral sapphire & violet nebula tournament table.",
            descBn = "ডিপ অ্যাস্ট্রাল স্যাফায়ার ও ভায়োলেট নেবুলা টুর্নামেন্ট বোর্ড।",
            priceGameCoins = 80.0,
            iconGlyph = "✧",
            previewAccentHex = 0xFF4F46E5
        ),

        // Avatars
        ArmoryItem(
            id = AVATAR_MONARCH,
            category = ArmoryItemCategory.AVATAR,
            titleEn = "Emerald Monarch",
            titleBn = "এমারেল্ড মনার্ক",
            descEn = "Default FinPlay Pro strategist crest.",
            descBn = "FinPlay Pro-এর স্ট্যান্ডার্ড স্ট্র্যাটেজিস্ট ব্যাজ।",
            priceGameCoins = 0.0,
            iconGlyph = "👑",
            previewAccentHex = 0xFF10B981
        ),
        ArmoryItem(
            id = AVATAR_KNIGHT,
            category = ArmoryItemCategory.AVATAR,
            titleEn = "Obsidian Knight",
            titleBn = "অবসিডিয়ান নাইট",
            descEn = "Armored tactical crest shown in online lobbies.",
            descBn = "অনলাইন ম্যাচ লবিতে প্রদর্শিত আর্মার্ড নাইট ব্যাজ।",
            priceGameCoins = 30.0,
            iconGlyph = "♞",
            previewAccentHex = 0xFF334155
        ),
        ArmoryItem(
            id = AVATAR_PHOENIX,
            category = ArmoryItemCategory.AVATAR,
            titleEn = "Solar Phoenix",
            titleBn = "সোলার ফিনিক্স",
            descEn = "Blazing gold crest for fierce tournament challengers.",
            descBn = "টুর্নামেন্ট চ্যাম্পিয়নদের জন্য গোল্ডেন ফিনিক্স অ্যাভাটার।",
            priceGameCoins = 50.0,
            iconGlyph = "🔥",
            previewAccentHex = 0xFFEA580C
        ),
        ArmoryItem(
            id = AVATAR_GRANDMASTER,
            category = ArmoryItemCategory.AVATAR,
            titleEn = "Aether Grandmaster",
            titleBn = "Aether গ্র্যান্ডমাস্টার",
            descEn = "Elite glowing astral crown badge for ranked rooms.",
            descBn = "অনলাইন র‍্যাঙ্কড রুমের জন্য এলিট গ্র্যান্ডমাস্টার ব্যাজ।",
            priceGameCoins = 90.0,
            iconGlyph = "⚡",
            previewAccentHex = 0xFF7C3AED
        ),

        // Consumable In-Game Skills
        ArmoryItem(
            id = SKILL_CHESS_HINT,
            category = ArmoryItemCategory.SKILL,
            titleEn = "Tactical Hint (x1)",
            titleBn = "ট্যাকটিক্যাল হিন্ট (১x)",
            descEn = "Reveals the engine's best move in Chess, Reversi, or Connect 4.",
            descBn = "দাবা, রিভার্সি বা কানেক্ট ফোরে সেরা চাল হাইলাইট করে।",
            priceGameCoins = 15.0,
            iconGlyph = "💡",
            previewAccentHex = 0xFFF59E0B,
            isConsumable = true
        ),
        ArmoryItem(
            id = SKILL_LUDO_REROLL,
            category = ArmoryItemCategory.SKILL,
            titleEn = "Dice Reroll Shield (x1)",
            titleBn = "ডাইস রিরোল শিল্ড (১x)",
            descEn = "Instantly reroll an unfavorable Ludo or Aether Crown dice roll.",
            descBn = "লুডু বা Aether Crown-এ খারাপ ডাইস পড়লে পুনরায় রোল করুন।",
            priceGameCoins = 15.0,
            iconGlyph = "🎲",
            previewAccentHex = 0xFF10B981,
            isConsumable = true
        ),
        ArmoryItem(
            id = SKILL_UNDO_PASS,
            category = ArmoryItemCategory.SKILL,
            titleEn = "Universal Undo Pass (x1)",
            titleBn = "ইউনিভার্সাল আনডু পাস (১x)",
            descEn = "Rewind your last move in offline strategy matches.",
            descBn = "অফলাইন স্ট্র্যাটেজি ম্যাচে শেষ চাল ফিরিয়ে আনুন।",
            priceGameCoins = 10.0,
            iconGlyph = "↩️",
            previewAccentHex = 0xFF0284C7,
            isConsumable = true
        ),
        ArmoryItem(
            id = SKILL_FLUX_CHARGE,
            category = ArmoryItemCategory.SKILL,
            titleEn = "Aether Flux Surge (x1)",
            titleBn = "Aether ফ্লাক্স সার্জ (১x)",
            descEn = "Instantly adds +1 Aether Charge during an Aether Crown match.",
            descBn = "Aether Crown ম্যাচে সাথে সাথে +১ Aether চার্জ যোগ করে।",
            priceGameCoins = 20.0,
            iconGlyph = "⚡",
            previewAccentHex = 0xFF6D28D9,
            isConsumable = true
        ),

        // War Land Characters & Skins
        ArmoryItem(
            id = WAR_CHAR_EVA,
            category = ArmoryItemCategory.WAR_CHARACTER,
            titleEn = "Agent Eva (Special Ops)",
            titleBn = "এজেন্ট ইভা (স্পেশাল অপস)",
            descEn = "Elite operative with customized tactical combat uniform.",
            descBn = "অভিজাত স্পেশাল অপস অপারেটিভ ও আধুনিক ট্যাকটিক্যাল কমব্যাট ইউনিফর্ম।",
            priceGameCoins = 0.0,
            iconGlyph = "🧕",
            previewAccentHex = 0xFF10B981
        ),
        ArmoryItem(
            id = WAR_CHAR_SOPHIA,
            category = ArmoryItemCategory.WAR_CHARACTER,
            titleEn = "Agent Sophia (Vanguard)",
            titleBn = "এজেন্ট সোফিয়া (ভ্যানগার্ড)",
            descEn = "High-spec frontline operative with reactive armor.",
            descBn = "হাই-স্পেক ফ্রন্টলাইন অপারেটিভ ও রিয়্যাক্টিভ আর্মার।",
            priceGameCoins = 30.0,
            iconGlyph = "👩‍🎤",
            previewAccentHex = 0xFFEC4899,
            validityDays = 14
        ),
        ArmoryItem(
            id = WAR_CHAR_ZANE,
            category = ArmoryItemCategory.WAR_CHARACTER,
            titleEn = "Zane Commando (Heavy Armor)",
            titleBn = "জেইন কমান্ডো (হেভি আর্মার)",
            descEn = "Heavy infantry commando built for protracted warfare.",
            descBn = "হেভি ইনফ্যান্ট্রি কমান্ডো ও সর্বোচ্চ কমব্যাট রেজিসট্যান্স।",
            priceGameCoins = 80.0,
            iconGlyph = "💂",
            previewAccentHex = 0xFF6366F1,
            validityDays = 30
        ),
        ArmoryItem(
            id = WAR_CHAR_COMMANDO,
            category = ArmoryItemCategory.WAR_CHARACTER,
            titleEn = "Desert Commando",
            titleBn = "মরু কমান্ডো",
            descEn = "Tactical combat operative equipped with desert camo armor.",
            descBn = "স্ট্যান্ডার্ড ট্যাকটিক্যাল কমব্যাট অপারেটিভ ও ক্যামো আর্মার।",
            priceGameCoins = 0.0,
            iconGlyph = "🪖",
            previewAccentHex = 0xFFD97706
        ),
        ArmoryItem(
            id = WAR_CHAR_NEON,
            category = ArmoryItemCategory.WAR_CHARACTER,
            titleEn = "Neon Assassin (7 Days)",
            titleBn = "নিয়ন অ্যাসাসিন (৭ দিন)",
            descEn = "High-agility cyber assassin with glowing cyan neon streaks.",
            descBn = "উজ্জ্বল নিয়ন সাইবার আর্মার ও ক্ষিপ্র গতির অ্যাসাসিন লুক।",
            priceGameCoins = 50.0,
            iconGlyph = "🥷",
            previewAccentHex = 0xFF06B6D4,
            validityDays = 7
        ),
        ArmoryItem(
            id = WAR_CHAR_CRIMSON,
            category = ArmoryItemCategory.WAR_CHARACTER,
            titleEn = "Crimson Striker (30 Days)",
            titleBn = "ক্রিমসন স্ট্রাইকার (৩০ দিন)",
            descEn = "Heavily armored elite spec-ops soldier with crimson highlights.",
            descBn = "ভারী রক্তিম আর্মার্ড স্পেক-অপস এলিট সোলজার।",
            priceGameCoins = 75.0,
            iconGlyph = "🥊",
            previewAccentHex = 0xFFDC2626,
            validityDays = 30
        ),
        ArmoryItem(
            id = WAR_CHAR_CYBORG,
            category = ArmoryItemCategory.WAR_CHARACTER,
            titleEn = "Apex Cyborg (Permanent)",
            titleBn = "অ্যাপেক্স সাইবর্গ (স্থায়ী)",
            descEn = "Futuristic titanium alloy combat cyborg chassis.",
            descBn = "ভবিষ্যতের টাইটানিয়াম অ্যালয় কমব্যাট সাইবর্গ বডি।",
            priceGameCoins = 120.0,
            iconGlyph = "🤖",
            previewAccentHex = 0xFF8B5CF6
        ),

        // War Land Weapon Skins
        ArmoryItem(
            id = WAR_GUN_DRAGON_AK,
            category = ArmoryItemCategory.WAR_WEAPON_SKIN,
            titleEn = "Dragon Flame AK-47 (14 Days)",
            titleBn = "ড্রাগন ফ্লেম AK-47 (১৪ দিন)",
            descEn = "Fiery draconic barrel emitting fiery orange muzzle bursts.",
            descBn = "আগুনে ড্রাগন খোদাই করা ব্যারেল ও কমব্যাট ফায়ার বুস্ট।",
            priceGameCoins = 45.0,
            iconGlyph = "🔥",
            previewAccentHex = 0xFFEA580C,
            validityDays = 14
        ),
        ArmoryItem(
            id = WAR_GUN_GOLD_AWM,
            category = ArmoryItemCategory.WAR_WEAPON_SKIN,
            titleEn = "Golden Emperor AWM (30 Days)",
            titleBn = "গোল্ডেন এম্পারর AWM (৩০ দিন)",
            descEn = "24K solid gold sniper rifle with luminous optic crosshair.",
            descBn = "২৪ ক্যারেট সলিড গোল্ড স্নাইপার ও গোল্ডেন স্কোপ ক্রসহেয়ার।",
            priceGameCoins = 60.0,
            iconGlyph = "🔱",
            previewAccentHex = 0xFFF59E0B,
            validityDays = 30
        ),
        ArmoryItem(
            id = WAR_GUN_CYBER_SHOTGUN,
            category = ArmoryItemCategory.WAR_WEAPON_SKIN,
            titleEn = "Cyber Vortex Shotgun (7 Days)",
            titleBn = "সাইবার ভর্টেক্স শটগান (৭ দিন)",
            descEn = "High-voltage neon shotgun with electric pellet spread.",
            descBn = "ইলেকট্রিক ব্লু নিয়ন পালস ও ক্লোজ-রেঞ্জ ড্যামেজ বুস্ট।",
            priceGameCoins = 40.0,
            iconGlyph = "⚡",
            previewAccentHex = 0xFF3B82F6,
            validityDays = 7
        ),
        ArmoryItem(
            id = WAR_POWER_GLOOWALL,
            category = ArmoryItemCategory.CONSUMABLE_CARD,
            titleEn = "Gloo Wall Cryo Shield Pack (x5)",
            titleBn = "গ্লু ওয়াল ক্রায়ো শিল্ড প্যাক (৫x)",
            descEn = "Instant deployable bulletproof ice shield for defensive cover.",
            descBn = "ফ্রি ফায়ারের মতো তাত্ক্ষণিক বুলেটপ্রুফ বরফের দেওয়াল প্রতিরক্ষা কভার।",
            priceGameCoins = 15.0,
            iconGlyph = "🛡️",
            previewAccentHex = 0xFF06B6D4,
            isConsumable = true
        ),

        // Emotes
        ArmoryItem(
            id = EMOTE_WAVE,
            category = ArmoryItemCategory.EMOTE,
            titleEn = "Wave / Hi Emote",
            titleBn = "হাই / ওয়েভ ইমোট",
            descEn = "Friendly combat greeting shown to teammates and enemies.",
            descBn = "যুদ্ধক্ষেত্রে ফ্রেন্ডস ও এনিমিদের সালাম/হাই জানান।",
            priceGameCoins = 10.0,
            iconGlyph = "👋",
            previewAccentHex = 0xFF10B981
        ),
        ArmoryItem(
            id = EMOTE_HEART,
            category = ArmoryItemCategory.EMOTE,
            titleEn = "Love / Heart Emote (30 Days)",
            titleBn = "লাভ / হার্ট ইমোট (৩০ দিন)",
            descEn = "Radiates glowing neon hearts overhead in 3D.",
            descBn = "মাথার উপর থ্রি-ডি নিয়ন লাভ হার্ট প্রদর্শন করে।",
            priceGameCoins = 25.0,
            iconGlyph = "❤️",
            previewAccentHex = 0xFFF43F5E,
            validityDays = 30
        ),
        ArmoryItem(
            id = EMOTE_LAUGH,
            category = ArmoryItemCategory.EMOTE,
            titleEn = "Smily / Laugh Emote",
            titleBn = "হাসি / স্মাইলি ইমোট",
            descEn = "Joyful chuckling animation after securing a victory.",
            descBn = "কিল করার পর বিজয়ের হাসির অ্যানিমেশন।",
            priceGameCoins = 15.0,
            iconGlyph = "😂",
            previewAccentHex = 0xFFFBBF24
        ),
        ArmoryItem(
            id = EMOTE_DANCE,
            category = ArmoryItemCategory.EMOTE,
            titleEn = "Victory Dance Emote (30 Days)",
            titleBn = "ভিক্টরি ড্যান্স ইমোট (৩০ দিন)",
            descEn = "Epic breakdance celebration visible to all live players.",
            descBn = "সব প্লেয়ারদের সামনে দৃষ্টিনন্দন ব্রেকড্যান্স শো।",
            priceGameCoins = 35.0,
            iconGlyph = "🕺",
            previewAccentHex = 0xFF8B5CF6,
            validityDays = 30
        ),

        // Cards & Consumables
        ArmoryItem(
            id = CARD_NAME_CHANGE,
            category = ArmoryItemCategory.CONSUMABLE_CARD,
            titleEn = "Name Change Card (x1)",
            titleBn = "নেম চেঞ্জ কার্ড (১x)",
            descEn = "Change your player nickname displayed across all games and leaderboards.",
            descBn = "আপনার গেমের নাম ও লিডারবোর্ডের নিকনেম পরিবর্তন করুন।",
            priceGameCoins = 50.0,
            iconGlyph = "🏷️",
            previewAccentHex = 0xFFEC4899,
            isConsumable = true
        ),

        // Cross-Game Extras
        ArmoryItem(
            id = SKIN_LUDO_AMETHYST,
            category = ArmoryItemCategory.BOARD_SKIN,
            titleEn = "Royal Amethyst Star (30 Days)",
            titleBn = "রয়্যাল অ্যামেথিস্ট স্টার (৩০ দিন)",
            descEn = "Enchanted violet velvet board with glowing star lanes.",
            descBn = "মোহনীয় বেগুনি ভেলভেট বোর্ড ও গ্লোয়িং স্টার লেন।",
            priceGameCoins = 35.0,
            iconGlyph = "⭐",
            previewAccentHex = 0xFF9333EA,
            validityDays = 30
        ),
        ArmoryItem(
            id = SKIN_CHESS_CYBERPUNK,
            category = ArmoryItemCategory.BOARD_SKIN,
            titleEn = "Neon Cyberpunk Chess (30 Days)",
            titleBn = "নিয়ন সাইবারপাঙ্ক দাবা (৩০ দিন)",
            descEn = "High-tech hologram squares and neon light edges.",
            descBn = "হালকা নীল ও গোলাপী নিয়ন গ্রিড দাবা বোর্ড।",
            priceGameCoins = 45.0,
            iconGlyph = "♟️",
            previewAccentHex = 0xFF06B6D4,
            validityDays = 30
        ),
        ArmoryItem(
            id = SKIN_AETHER_NEBULA,
            category = ArmoryItemCategory.BOARD_SKIN,
            titleEn = "Cosmic Nebula Flux Core",
            titleBn = "কসমিক নেবুলা ফ্লাক্স কোর",
            descEn = "Astral deep galaxy ring board skin with floating starlight.",
            descBn = "গ্যালাক্সি স্টারলাইট রিং ও মহাজাগতিক ফ্লাক্স বোর্ড।",
            priceGameCoins = 65.0,
            iconGlyph = "🌌",
            previewAccentHex = 0xFF6366F1
        )
    )

    fun itemById(id: String): ArmoryItem? = items.firstOrNull { it.id == id }

    fun boardSkinPalette(skinId: String): BoardSkinPalette = when (skinId) {
        SKIN_OBSIDIAN_GOLD -> BoardSkinPalette(
            id = SKIN_OBSIDIAN_GOLD,
            frameStart = Color(0xFF111827),
            frameEnd = Color(0xFF292524),
            accentBorder = Color(0xFFF59E0B),
            boardLight = Color(0xFFF5ECD7),
            boardDark = Color(0xFF44403C),
            feltSurface = Color(0xFF1C1917)
        )
        SKIN_IMPERIAL_RUBY -> BoardSkinPalette(
            id = SKIN_IMPERIAL_RUBY,
            frameStart = Color(0xFF4C0519),
            frameEnd = Color(0xFF881337),
            accentBorder = Color(0xFFFDA4AF),
            boardLight = Color(0xFFFFF1F2),
            boardDark = Color(0xFF9F1239),
            feltSurface = Color(0xFF500724)
        )
        SKIN_COSMIC_SAPPHIRE -> BoardSkinPalette(
            id = SKIN_COSMIC_SAPPHIRE,
            frameStart = Color(0xFF1E1B4B),
            frameEnd = Color(0xFF312E81),
            accentBorder = Color(0xFF818CF8),
            boardLight = Color(0xFFEEF2FF),
            boardDark = Color(0xFF3730A3),
            feltSurface = Color(0xFF172554)
        )
        SKIN_LUDO_AMETHYST -> BoardSkinPalette(
            id = SKIN_LUDO_AMETHYST,
            frameStart = Color(0xFF3B0764),
            frameEnd = Color(0xFF581C87),
            accentBorder = Color(0xFFC084FC),
            boardLight = Color(0xFFFAF5FF),
            boardDark = Color(0xFF6B21A8),
            feltSurface = Color(0xFF2E1065)
        )
        SKIN_CHESS_CYBERPUNK -> BoardSkinPalette(
            id = SKIN_CHESS_CYBERPUNK,
            frameStart = Color(0xFF0F172A),
            frameEnd = Color(0xFF1E293B),
            accentBorder = Color(0xFF22D3EE),
            boardLight = Color(0xFFE0F2FE),
            boardDark = Color(0xFF0369A1),
            feltSurface = Color(0xFF082F49)
        )
        SKIN_AETHER_NEBULA -> BoardSkinPalette(
            id = SKIN_AETHER_NEBULA,
            frameStart = Color(0xFF1E1B4B),
            frameEnd = Color(0xFF312E81),
            accentBorder = Color(0xFFA855F7),
            boardLight = Color(0xFFF3E8FF),
            boardDark = Color(0xFF4C1D95),
            feltSurface = Color(0xFF241442)
        )
        else -> BoardSkinPalette(
            id = SKIN_ROYAL_EMERALD,
            frameStart = Color(0xFF042F2E),
            frameEnd = Color(0xFF065F46),
            accentBorder = Color(0xFFD4AF37),
            boardLight = Color(0xFFF3EFE0),
            boardDark = Color(0xFF0F766E),
            feltSurface = Color(0xFF064E3B)
        )
    }

    fun avatarGlyph(avatarId: String): String =
        items.firstOrNull { it.id == avatarId }?.iconGlyph ?: "👑"

    fun ownedItemSet(wallet: PlayWalletEntity, nowMillis: Long = System.currentTimeMillis()): Set<String> {
        val rawTokens = wallet.ownedItemsCsv
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val active = mutableSetOf<String>()
        rawTokens.forEach { token ->
            if (token.contains(':')) {
                val parts = token.split(':')
                val itemId = parts[0]
                val expiry = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                if (nowMillis <= expiry) {
                    active.add(itemId)
                }
            } else {
                active.add(token)
            }
        }
        return active + setOf(SKIN_ROYAL_EMERALD, AVATAR_MONARCH, WAR_CHAR_COMMANDO, WAR_CHAR_EVA, EMOTE_WAVE)
    }

    fun getRemainingDays(wallet: PlayWalletEntity, itemId: String, nowMillis: Long = System.currentTimeMillis()): Int? {
        val rawTokens = wallet.ownedItemsCsv.split(',').map { it.trim() }
        val token = rawTokens.firstOrNull { it.startsWith("$itemId:") } ?: return null
        val expiry = token.substringAfter(':').toLongOrNull() ?: return null
        val diff = expiry - nowMillis
        return if (diff <= 0) 0 else (diff / (24L * 60L * 60L * 1000L)).toInt() + 1
    }

    fun isItemExpired(wallet: PlayWalletEntity, itemId: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val rawTokens = wallet.ownedItemsCsv.split(',').map { it.trim() }
        val token = rawTokens.firstOrNull { it.startsWith("$itemId:") } ?: return false
        val expiry = token.substringAfter(':').toLongOrNull() ?: return false
        return nowMillis > expiry
    }

    fun encodeOwnedItemToken(item: ArmoryItem, nowMillis: Long = System.currentTimeMillis()): String {
        return if (item.validityDays != null) {
            val expiry = nowMillis + (item.validityDays * 24L * 60L * 60L * 1000L)
            "${item.id}:$expiry"
        } else {
            item.id
        }
    }

    fun parseSkillCharges(jsonRaw: String): Map<String, Int> {
        if (jsonRaw.isBlank() || jsonRaw == "{}") return emptyMap()
        return runCatching {
            val obj = Json.parseToJsonElement(jsonRaw).jsonObject
            obj.mapValues { (_, v) -> v.jsonPrimitive.intOrNull ?: 0 }
                .filterValues { it > 0 }
        }.getOrDefault(emptyMap())
    }

    fun serializeSkillCharges(charges: Map<String, Int>): String {
        val clean = charges.filterValues { it > 0 }
        val jsonObj = JsonObject(clean.mapValues { (_, count) -> JsonPrimitive(count) })
        return jsonObj.toString()
    }
}
