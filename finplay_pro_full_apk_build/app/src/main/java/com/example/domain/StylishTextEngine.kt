package com.example.domain

data class StylishFontPreset(
    val id: String,
    val name: String,
    val category: String,
    val transform: (String) -> String
)

data class LineArtTemplate(
    val id: String,
    val name: String,
    val wrap: (String) -> String
)

data class SymbolGroup(
    val name: String,
    val symbols: List<String>
)

/**
 * Pure Kotlin engine for Unicode font transformations, categorized symbol vaults,
 * live decorative line frames/borders, and kaomoji/text art.
 * Handles Supplementary Multilingual Plane (SMP) codepoints cleanly via appendCodePoint.
 */
object StylishTextEngine {

    private fun mapAlphaDigits(
        input: String,
        upperStart: Int? = null,
        lowerStart: Int? = null,
        digitStart: Int? = null,
        exceptions: Map<Char, Int> = emptyMap(),
        transformChar: ((Char) -> String)? = null
    ): String {
        val sb = StringBuilder(input.length * 2)
        for (ch in input) {
            val ex = exceptions[ch]
            when {
                ex != null -> sb.appendCodePoint(ex)
                upperStart != null && ch in 'A'..'Z' -> sb.appendCodePoint(upperStart + (ch - 'A'))
                lowerStart != null && ch in 'a'..'z' -> sb.appendCodePoint(lowerStart + (ch - 'a'))
                digitStart != null && ch in '0'..'9' -> sb.appendCodePoint(digitStart + (ch - '0'))
                transformChar != null -> sb.append(transformChar(ch))
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun mapFromTable(input: String, upperTable: String, lowerTable: String, digitTable: String? = null): String {
        val sb = StringBuilder(input.length * 2)
        for (ch in input) {
            when {
                ch in 'A'..'Z' -> sb.append(upperTable[ch - 'A'])
                ch in 'a'..'z' -> sb.append(lowerTable[ch - 'a'])
                digitTable != null && ch in '0'..'9' -> sb.append(digitTable[ch - '0'])
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private val scriptExceptions = mapOf(
        'B' to 0x212C, 'E' to 0x2130, 'F' to 0x2131, 'H' to 0x210B, 'I' to 0x2110,
        'L' to 0x2112, 'M' to 0x2133, 'R' to 0x211B,
        'e' to 0x212F, 'g' to 0x210A, 'o' to 0x2134
    )

    private val frakturExceptions = mapOf(
        'C' to 0x212D, 'H' to 0x210C, 'I' to 0x2111, 'R' to 0x211C, 'Z' to 0x2128
    )

    private val doubleStruckExceptions = mapOf(
        'C' to 0x2102, 'H' to 0x210D, 'N' to 0x2115, 'P' to 0x2119,
        'Q' to 0x211A, 'R' to 0x211D, 'Z' to 0x2124
    )

    private val smallCapsLower = "ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ"
    private val fullWidthUpper = "ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ"
    private val fullWidthLower = "ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ"
    private val fullWidthDigits = "０１２３４５６７８９"
    private val superLower = "ᵃᵇᶜᵈᵉᶠᵍʰⁱʲᵏˡᵐⁿᵒᵖqʳˢᵗᵘᵛʷˣʸᶻ"
    private val superUpper = "ᴬᴮᶜᴰᴱᶠᴳᴴᴵᴶᴷᴸᴹᴺᴼᴾQᴿˢᵀᵁⱽᵂˣʸᶻ"
    private val superDigits = "⁰¹²³⁴⁵⁶⁷⁸⁹"
    private val upsideDownLower = "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz"
    private val upsideDownUpper = "∀qƆpƎℲפHIſʞ˥WNOԀQɹS┴∩ΛMX⅄Z"
    private val upsideDownDigits = "0ƖᄅƐㄣϛ9ㄥ86"
    private val mirrorLower = "ɒdɔbɘᎸǫʜiįʞlmnopqɿƨƚυvwxʏz"
    private val subscriptDigits = "₀₁₂₃₄₅₆₇₈₉"

    private fun toNegativeCircled(input: String): String {
        val sb = StringBuilder(input.length * 2)
        for (ch in input) {
            when {
                ch in 'A'..'Z' -> sb.appendCodePoint(0x1F150 + (ch - 'A'))
                ch in 'a'..'z' -> sb.appendCodePoint(0x1F150 + (ch - 'a'))
                ch == '0' -> sb.append('\u24FF')
                ch in '1'..'9' -> sb.appendCodePoint(0x2776 + (ch - '1'))
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun toSquared(input: String, filled: Boolean): String {
        val base = if (filled) 0x1F170 else 0x1F130
        val sb = StringBuilder(input.length * 2)
        for (ch in input) {
            when {
                ch in 'A'..'Z' -> sb.appendCodePoint(base + (ch - 'A'))
                ch in 'a'..'z' -> sb.appendCodePoint(base + (ch - 'a'))
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun toParenthesized(input: String): String {
        val sb = StringBuilder(input.length * 2)
        for (ch in input) {
            when {
                ch in 'A'..'Z' -> sb.appendCodePoint(0x249C + (ch - 'A'))
                ch in 'a'..'z' -> sb.appendCodePoint(0x249C + (ch - 'a'))
                ch in '1'..'9' -> sb.appendCodePoint(0x2474 + (ch - '1'))
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun toCombining(input: String, combiningMark: Char): String {
        val sb = StringBuilder(input.length * 2)
        for (ch in input) {
            sb.append(ch)
            if (!ch.isWhitespace()) sb.append(combiningMark)
        }
        return sb.toString()
    }

    private fun toZalgoLight(input: String): String {
        val upMarks = charArrayOf('\u030D', '\u030E', '\u0304', '\u0305', '\u033F', '\u0311')
        val midMarks = charArrayOf('\u0334', '\u0335', '\u0336', '\u0337')
        val downMarks = charArrayOf('\u0316', '\u0317', '\u0318', '\u0319', '\u0324', '\u0325')
        val sb = StringBuilder(input.length * 4)
        input.forEachIndexed { idx, ch ->
            sb.append(ch)
            if (!ch.isWhitespace()) {
                sb.append(upMarks[idx % upMarks.size])
                if (idx % 2 == 0) sb.append(midMarks[idx % midMarks.size])
                sb.append(downMarks[idx % downMarks.size])
            }
        }
        return sb.toString()
    }

    val fontPresets: List<StylishFontPreset> = listOf(
        StylishFontPreset("bold_serif", "𝐁𝐨𝐥𝐝 𝐒𝐞𝐫𝐢𝐟", "Classic") {
            mapAlphaDigits(it, upperStart = 0x1D400, lowerStart = 0x1D41A, digitStart = 0x1D7CE)
        },
        StylishFontPreset("italic_serif", "𝐼𝑡𝑎𝑙𝑖𝑐 𝑆𝑒𝑟𝑖𝑓", "Classic") {
            mapAlphaDigits(it, upperStart = 0x1D434, lowerStart = 0x1D44E, exceptions = mapOf('h' to 0x210E))
        },
        StylishFontPreset("bold_italic_serif", "𝑩𝒐𝒍𝒅 𝑰𝒕𝒂𝒍𝒊𝒄", "Classic") {
            mapAlphaDigits(it, upperStart = 0x1D468, lowerStart = 0x1D482)
        },
        StylishFontPreset("bold_sans", "𝗦𝗮𝗻𝘀 𝗕𝗼𝗹𝗱", "Modern") {
            mapAlphaDigits(it, upperStart = 0x1D5D4, lowerStart = 0x1D5EE, digitStart = 0x1D7EC)
        },
        StylishFontPreset("italic_sans", "𝘚𝘢𝘯𝘴 𝘐𝘵𝘢𝘭𝘪𝘤", "Modern") {
            mapAlphaDigits(it, upperStart = 0x1D608, lowerStart = 0x1D622)
        },
        StylishFontPreset("bold_italic_sans", "𝙎𝙖𝙣𝙨 𝘽𝙤𝙡𝙙 𝙄𝙩𝙖𝙡𝙞𝙘", "Modern") {
            mapAlphaDigits(it, upperStart = 0x1D63C, lowerStart = 0x1D656)
        },
        StylishFontPreset("script", "𝒮𝒸𝓇𝒾𝓅𝓉", "Calligraphy") {
            mapAlphaDigits(it, upperStart = 0x1D49C, lowerStart = 0x1D4B6, exceptions = scriptExceptions)
        },
        StylishFontPreset("bold_script", "𝓑𝓸𝓵𝓭 𝓢𝓬𝓻𝓲𝓹𝓽", "Calligraphy") {
            mapAlphaDigits(it, upperStart = 0x1D4D0, lowerStart = 0x1D4EA)
        },
        StylishFontPreset("fraktur", "𝔉𝔯𝔞𝔨𝔱𝔲𝔯", "Gothic") {
            mapAlphaDigits(it, upperStart = 0x1D504, lowerStart = 0x1D51E, exceptions = frakturExceptions)
        },
        StylishFontPreset("bold_fraktur", "𝕲𝖔𝖙𝖍𝖎𝖈 𝕭𝖔𝖑𝖉", "Gothic") {
            mapAlphaDigits(it, upperStart = 0x1D56C, lowerStart = 0x1D586)
        },
        StylishFontPreset("double_struck", "𝔻𝕠𝕦𝕓𝕝𝕖 𝕊𝕥𝕣𝕦𝕔𝕜", "Display") {
            mapAlphaDigits(it, upperStart = 0x1D538, lowerStart = 0x1D552, digitStart = 0x1D7D8, exceptions = doubleStruckExceptions)
        },
        StylishFontPreset("monospace", "𝙼𝚘𝚗𝚘𝚜𝚙𝚊𝚌𝚎", "Modern") {
            mapAlphaDigits(it, upperStart = 0x1D670, lowerStart = 0x1D68A, digitStart = 0x1D7F6)
        },
        StylishFontPreset("small_caps", "ꜱᴍᴀʟʟ ᴄᴀᴘꜱ", "Classic") {
            mapFromTable(it, "ABCDEFGHIJKLMNOPQRSTUVWXYZ", smallCapsLower, "0123456789")
        },
        StylishFontPreset("bubble", "Ⓑⓤⓑⓑⓛⓔ", "Badges") {
            mapAlphaDigits(
                it,
                upperStart = 0x24B6,
                lowerStart = 0x24D0,
                exceptions = mapOf('0' to 0x24EA),
                transformChar = { ch ->
                    if (ch in '1'..'9') String(Character.toChars(0x2460 + (ch - '1'))) else ch.toString()
                }
            )
        },
        StylishFontPreset("dark_bubble", "🅑🅛🅐🅒🅚 🅑🅤🅑🅑🅛🅔", "Badges") { toNegativeCircled(it) },
        StylishFontPreset("squared", "🅂🅀🅄🄰🅁🄴🄳", "Badges") { toSquared(it, filled = false) },
        StylishFontPreset("dark_squared", "🆂🆀🆄🅰🆁🅴🅳", "Badges") { toSquared(it, filled = true) },
        StylishFontPreset("parenthesized", "⒫⒜⒭⒠⒩⒯⒣⒠⒮⒤⒵⒠⒟", "Badges") { toParenthesized(it) },
        StylishFontPreset("fullwidth", "Ｗｉｄｅ Ａｅｓｔｈｅｔｉｃ", "Display") {
            mapFromTable(it, fullWidthUpper, fullWidthLower, fullWidthDigits)
        },
        StylishFontPreset("superscript", "ˢᵘᵖᵉʳˢᶜʳⁱᵖᵗ", "Tiny") {
            mapFromTable(it, superUpper, superLower, superDigits)
        },
        StylishFontPreset("upside_down", "uʍop ǝpᴉsd∩", "Fun") {
            mapFromTable(it, upsideDownUpper, upsideDownLower, upsideDownDigits).reversed()
        },
        StylishFontPreset("mirror", "ɿoɿɿiM", "Fun") {
            mapFromTable(it, "ABCDEFGHIJKLMNOPQRSTUVWXYZ", mirrorLower, subscriptDigits).reversed()
        },
        StylishFontPreset("spaced", "S P A C E D", "Display") {
            it.toCharArray().joinToString(" ")
        },
        StylishFontPreset("slash_strike", "S̷l̷a̷s̷h̷", "Glitch") { toCombining(it, '\u0337') },
        StylishFontPreset("strikethrough", "S̶t̶r̶i̶k̶e̶", "Glitch") { toCombining(it, '\u0336') },
        StylishFontPreset("underline", "U̲n̲d̲e̲r̲l̲i̲n̲e̲", "Glitch") { toCombining(it, '\u0332') },
        StylishFontPreset("double_underline", "D̳o̳u̳b̳l̳e̳", "Glitch") { toCombining(it, '\u0333') },
        StylishFontPreset("zalgo", "Z̷̡a̧̢l̶̨g̡̢o̶̡", "Glitch") { toZalgoLight(it) }
    )

    val symbolGroups: List<SymbolGroup> = listOf(
        SymbolGroup("Stars & Sparkles", listOf("★", "☆", "✦", "✧", "✩", "✪", "✫", "✬", "✭", "✮", "✯", "✰", "✵", "✶", "✷", "✸", "✹", "✺", "✻", "✼", "✽", "✾", "✿", "❀", "❁", "❃", "❇", "❈", "✨", "⚡", "☀", "☽", "☾")),
        SymbolGroup("Crowns & Royalty", listOf("👑", "♛", "♕", "♚", "♔", "⚜", "🔱", "🏆", "💎", "💍", "🎖", "🏅", "🜲", "✧", "◈", "❖", "۩", "۞")),
        SymbolGroup("Hearts & Love", listOf("❤", "❥", "❣", "ღ", "♡", "♥", "💖", "💗", "💓", "💞", "💕", "💘", "💝", "🖤", "🤍", "💚", "💛", "💜", "💙", "🫶")),
        SymbolGroup("Arrows & Pointers", listOf("→", "←", "↑", "↓", "↔", "↕", "➜", "➤", "➣", "➢", "➥", "➦", "➧", "➨", "➩", "➪", "➫", "➬", "➭", "➮", "➯", "➱", "»", "«", "❯", "❮", "▶", "◀")),
        SymbolGroup("Checks & Badges", listOf("✓", "✔", "✅", "✕", "✖", "✗", "✘", "❎", "☑", "⚠", "⛔", "🚫", "💯", "🔥", "⭐", "💎", "🆕", "🆘", "🔴", "🟢", "🟡", "🔵")),
        SymbolGroup("Chess & Cards", listOf("♔", "♕", "♖", "♗", "♘", "♙", "♚", "♛", "♜", "♝", "♞", "♟", "♠", "♣", "♥", "♦", "♤", "♧", "♡", "♢", "🎲", "🎯", "🃏")),
        SymbolGroup("Brackets & Wings", listOf("『", "』", "「", "」", "【", "】", "〔", "〕", "《", "》", "〈", "〉", "〖", "〗", "〘", "〙", "〚", "〛", "꧁", "꧂", "༺", "༻", "╰", "╯", "╭", "╮")),
        SymbolGroup("Currency & Math", listOf("৳", "$", "€", "£", "¥", "₹", "₿", "¢", "₩", "₽", "₫", "₱", "∞", "∑", "∏", "√", "∫", "≈", "≠", "≤", "≥", "±", "×", "÷", "π", "Ω", "Δ", "°")),
        SymbolGroup("Music & Tech", listOf("♩", "♪", "♫", "♬", "♭", "♮", "♯", "🎧", "🎵", "🎶", "⌘", "⌥", "⇧", "⏎", "⌫", "⚙", "⚛", "☣", "☢", "☯", "☮", "⚖")),
        SymbolGroup("Box Borders", listOf("─", "━", "│", "┃", "┌", "┐", "└", "┘", "╔", "╗", "╚", "╝", "═", "║", "╠", "╣", "╦", "╩", "╬", "▀", "▄", "█", "▌", "▐", "░", "▒", "▓"))
    )

    val lineTemplates: List<LineArtTemplate> = listOf(
        LineArtTemplate("royal_wings", "꧁ Royal Wings ꧂") { "꧁༒☬ $it ☬༒꧂" },
        LineArtTemplate("crown_banner", "👑 Crown Banner") { "♛┈⛧┈•༶ $it ༶•┈⛧┈♛" },
        LineArtTemplate("double_box", "╔═ Double Box ═╗") { t ->
            val clean = t.ifBlank { "Smart Finance Life" }
            val bar = "═".repeat((clean.length + 2).coerceAtMost(32))
            "╔$bar╗\n║ $clean ║\n╚$bar╝"
        },
        LineArtTemplate("rounded_box", "╭─ Clean Card ─╮") { t ->
            val clean = t.ifBlank { "Smart Finance Life" }
            val bar = "─".repeat((clean.length + 2).coerceAtMost(32))
            "╭$bar╮\n│ $clean │\n╰$bar╯"
        },
        LineArtTemplate("sparkle_ribbon", "✦ Sparkle Ribbon ✦") { "✧･ﾟ: *✧･ﾟ:* $it *:･ﾟ✧*:･ﾟ✧" },
        LineArtTemplate("japanese_frame", "『 Japanese Frame 』") { "『★ $it ★』" },
        LineArtTemplate("bracket_gold", "【 Bold Bracket 】") { "【✦ $it ✦】" },
        LineArtTemplate("emerald_divider", "━━━ Divider ━━━") { "━━━ ✦ $it ✦ ━━━" },
        LineArtTemplate("double_divider", "═══ Imperial ═══") { "═══❖═══ $it ═══❖═══" },
        LineArtTemplate("arrow_stream", "➤ Arrow Focus") { "»»—— $it ——««" },
        LineArtTemplate("wave_border", "〰 Wave Flow 〰") { "〰〰✦ $it ✦〰〰" },
        LineArtTemplate("sword_crest", "⚔ Gaming Tag ⚔") { "▄︻デ $it ══━一" },
        LineArtTemplate("star_orbit", "★ Starry Sky ★") { "★·.·´¯`·.·★ $it ★·.·´¯`·.·★" },
        LineArtTemplate("floral_vine", "✿ Floral Vine ✿") { "✿.｡.:* ☆ $it ☆ *.:｡.✿" },
        LineArtTemplate("dotted_grid", "░▒▓ Gradient ▓▒░") { "░▒▓█ $it █▓▒░" },
        LineArtTemplate("check_stamp", "✔ Verified Stamp") { "【✔】 $it 【✔】" }
    )

    val kaomojiAndArt: List<Pair<String, String>> = listOf(
        "Happy" to "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧",
        "Cool" to "(⌐■_■)",
        "Victory" to "٩(ˊᗜˋ*)و",
        "Love" to "(｡♥‿♥｡)",
        "Shrug" to "¯\\_(ツ)_/¯",
        "Sparkle" to "✧･ﾟ: *✧･ﾟ♡*(ᵘʷᵘ)*♡･ﾟ✧*:･ﾟ✧",
        "Thinking" to "(－‸ლ) ...hmm",
        "Rich / Money" to "[̲̅$̲̅(̲̅ιοο̲̅)̲̅$̲̅]",
        "Champion" to "🏆 ╰(°▽°)╯ 🏆",
        "Crown Banner" to "═════ ◈ ═════\n   👑 VIP 👑\n═════ ◈ ═════",
        "Separator Line" to "•·················•·················•",
        "Wave Line" to "˜”*°•.˜”*°• •°*”˜.•°*”˜"
    )
}
