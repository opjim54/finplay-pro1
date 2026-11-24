package com.example.ui.screens.games

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import com.example.R
import com.example.data.games.FirestoreMultiplayerRepository
import com.example.data.local.BroadcastNotificationEntity
import com.example.data.local.ExternalTournamentEntity
import com.example.data.local.ExternalTournamentEntryEntity
import com.example.data.local.GameBotConfigEntity
import com.example.data.local.PlayWalletEntity
import com.example.data.local.TournamentTierConfigEntity
import com.example.domain.DateTimeUtil
import com.example.domain.games.FreeFireMapBanner
import com.example.domain.games.TournamentPrizeCodec
import com.example.domain.games.TournamentTimeFormatter
import com.example.ui.viewmodel.MatchmakingScanState
import com.example.ui.viewmodel.OnlineAuthProfile
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Premium Free Fire / Esports Tournament Tab with Hero Banner, Live vs Completed filter,
 * live ticking countdown, clickable Joined Players roster, animated slot fill bars,
 * Full-Match gate, and unlocked Room ID & Password copy box.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EsportsTournamentsTabContent(
    tournaments: List<ExternalTournamentEntity>,
    myEntries: List<ExternalTournamentEntryEntity>,
    allEntries: List<ExternalTournamentEntryEntity> = emptyList(),
    currentUserId: Long = 0L,
    wallet: PlayWalletEntity,
    isBn: Boolean,
    onTriggerPreMatchAlert: (ExternalTournamentEntity) -> Unit = {},
    onJoinTournament: (Long, String, String, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showCompleted by remember { mutableStateOf(false) }
    var joinTarget by remember { mutableStateOf<ExternalTournamentEntity?>(null) }
    var viewPlayersTarget by remember { mutableStateOf<ExternalTournamentEntity?>(null) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val myEntryMap = remember(myEntries) { myEntries.associateBy { it.tournamentId } }
    val entriesByTournament = remember(allEntries) { allEntries.groupBy { it.tournamentId } }
    val filtered = remember(tournaments, showCompleted) {
        if (showCompleted) tournaments.filter { it.status == "COMPLETED" }
        else tournaments.filter { it.status != "COMPLETED" }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // ---- HERO BANNER ----
        item(key = "esports_hero_banner") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.75f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(178.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_esports_battle_banner),
                        contentDescription = "Esports Tournament Arena",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF090D16).copy(alpha = 0.35f),
                                        Color(0xFF090D16).copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFEF4444)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("FREE FIRE & ESPORTS ARENA", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Text(
                            text = if (isBn) "কাস্টম রুম টুর্নামেন্ট ও উইনিং প্রাইজ" else "Pro Custom Room Tournaments",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (isBn) "জয়েন করলেই Room ID ও Password আনলক • বিজয়ীর টাকা সরাসরি Winning Balance-এ!"
                            else "Join to unlock Room ID & Pass • Win prizes directly into withdrawable Winning Coins!",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFDE68A)
                        )
                    }
                }
            }
        }

        // ---- ANTI-CHEAT & FAIR PLAY DIRECTIVE CARD ----
        item(key = "esports_anticheat_directive") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF450A0A),
                border = BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("esports_anticheat_card")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.GppBad,
                        contentDescription = "Anti-Cheat Warning",
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(22.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = if (isBn) "⚠️ টুর্নামেন্ট নির্দেশনা ও অ্যান্টি-হ্যাক নীতিমালা" else "⚠️ Anti-Cheat & Fair Play Policy",
                            color = Color(0xFFFECACA),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = if (isBn) "• কোনো ধরনের Hack / Panel / Headshot Script / Config / Teaming / Emulator Bypass ব্যবহার করলে অ্যাকাউন্ট স্থায়ীভাবে SUSPEND হবে এবং কোনো প্রাইজ দেওয়া হবে না।\n• সঠিক Free Fire Game Name (IGN) ও UID দিয়ে জয়েন করুন। ম্যাচ শুরুর আগে Room ID ও Password অ্যালার্ট পাঠানো হবে।"
                            else "• Any Hack, Headshot Panel, Config File, Teaming, or Emulator Bypass = PERMANENT ACCOUNT SUSPENSION & prize forfeiture.\n• Enter your exact Free Fire IGN & UID when joining. You will receive an instant Room ID & Password alert before match start.",
                            color = Color(0xFFFEE2E2),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // ---- UPCOMING/LIVE vs COMPLETED SWITCH ----
        item(key = "esports_filter_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = !showCompleted,
                    onClick = { showCompleted = false },
                    label = {
                        val activeCnt = tournaments.count { it.status != "COMPLETED" }
                        Text(if (isBn) "🔥 লাইভ / আসন্ন ($activeCnt)" else "🔥 Live / Upcoming ($activeCnt)", fontWeight = FontWeight.Bold)
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = showCompleted,
                    onClick = { showCompleted = true },
                    label = {
                        val compCnt = tournaments.count { it.status == "COMPLETED" }
                        Text(if (isBn) "🏆 সম্পন্ন ($compCnt)" else "🏆 Completed ($compCnt)", fontWeight = FontWeight.Bold)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (filtered.isEmpty()) {
            item(key = "esports_empty") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(40.dp))
                        Text(
                            text = if (showCompleted) {
                                if (isBn) "কোনো Completed হিস্ট্রি নেই (নতুন ম্যাচ এলে পুরোনোটা অটো ডিলিট হয়)" else "No completed history (auto-cleared when a new match launches)"
                            } else {
                                if (isBn) "এখনো কোনো Free Fire টুর্নামেন্ট লাইভ নেই — Admin নতুন ম্যাচ অ্যাড করলে এখানে দেখাবে!" else "No active Free Fire tournaments yet — check back soon!"
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(filtered, key = { "ff_card_${it.id}" }) { t ->
            val myEntry = myEntryMap[t.id]
            val tournamentEntries = entriesByTournament[t.id].orEmpty()
            val effectiveJoinedCount = maxOf(t.joinedCount, tournamentEntries.size)
            val isFull = effectiveJoinedCount >= t.maxSlots
            val fillRatio = (effectiveJoinedCount.toFloat() / t.maxSlots.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            val mapBanner = remember(t.bannerKey, t.mapName) {
                FreeFireMapBanner.fromKeyOrMap(t.bannerKey, t.mapName)
            }
            val rankPrizes = remember(t.prizeRanksJson) {
                TournamentPrizeCodec.parseRanks(t.prizeRanksJson)
            }
            val remainingMs = if (t.matchStartTimeMillis > 0L) t.matchStartTimeMillis - nowMillis else 0L
            val isPreMatchAlertWindow = myEntry != null &&
                t.status != "COMPLETED" &&
                t.matchStartTimeMillis > 0L &&
                remainingMs in 1L..(t.preMatchAlertMinutes.coerceAtLeast(1) * 60_000L)

            LaunchedEffect(t.id, isPreMatchAlertWindow) {
                if (isPreMatchAlertWindow) {
                    onTriggerPreMatchAlert(t)
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(
                    1.3.dp,
                    when {
                        t.status == "LIVE" -> Color(0xFFEF4444)
                        myEntry != null -> Color(0xFF10B981)
                        else -> Color(0xFFF59E0B).copy(alpha = 0.65f)
                    }
                ),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF172554))
                        )
                    )
                ) {
                    // ---- REAL GENERATED MAP BANNER ----
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                    ) {
                        Image(
                            painter = painterResource(id = mapBanner.bannerRes),
                            contentDescription = "${mapBanner.displayName} Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF090D16).copy(alpha = 0.25f),
                                            Color(0xFF0F172A).copy(alpha = 0.92f)
                                        )
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF090D16).copy(alpha = 0.78f),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.7f))
                                ) {
                                    Text(
                                        "${t.modeLabel} • ${mapBanner.displayName}",
                                        color = Color(0xFFFDE68A),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                if (t.versionLabel.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF090D16).copy(alpha = 0.72f)
                                    ) {
                                        Text(
                                            t.versionLabel,
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = when (t.status) {
                                    "LIVE" -> Color(0xFFEF4444)
                                    "COMPLETED" -> Color(0xFF475569)
                                    else -> Color(0xFF059669)
                                }
                            ) {
                                Text(
                                    t.status,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = t.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            val scheduleCaption = t.startTimeLabel.ifBlank {
                                TournamentTimeFormatter.formatScheduleLabel(t.matchStartTimeMillis)
                            }
                            Text(
                                text = "🕒 $scheduleCaption  •  ${t.gameCategory.replace("_", " ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }

                    // ---- CARD BODY ----
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ---- LIVE TICKING COUNTDOWN TIMER ----
                        if (t.status != "COMPLETED" && t.matchStartTimeMillis > 0L) {
                            val countdownText = formatCountdownLabel(remainingMs, isBn)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (remainingMs <= t.preMatchAlertMinutes * 60_000L && remainingMs > 0L) {
                                    Color(0xFF991B1B)
                                } else {
                                    Color(0xFF0F766E).copy(alpha = 0.35f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (remainingMs <= t.preMatchAlertMinutes * 60_000L && remainingMs > 0L) Color(0xFFFCA5A5) else Color(0xFF2DD4BF)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = Color(0xFFFDE68A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (isBn) "ম্যাচ শুরুর কাউন্টডাউন" else "Live Match Countdown",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = countdownText,
                                        color = Color(0xFFFDE68A),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // ---- PRE-MATCH ROOM ALERT BANNER FOR JOINED PLAYERS ----
                        if (isPreMatchAlertWindow || (myEntry != null && t.roomAlertSent && t.status != "COMPLETED")) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFB45309),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = if (isBn) "🔥 অ্যালার্ট: Room ID ও Password দেওয়া হয়েছে — দ্রুত Free Fire লগইন করে কাস্টম রুমে জয়েন হন!"
                                        else "🔥 ROOM ALERT: Room ID & Password released — Log into Free Fire and join now!",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // Prize / Per Kill / Entry Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EsportsStatBox("PRIZE POOL", "৳${t.prizePool.toInt()}", Color(0xFFFBBF24), Modifier.weight(1f))
                            EsportsStatBox("PER KILL", "৳${t.perKillReward.toInt()}", Color(0xFF34D399), Modifier.weight(1f))
                            EsportsStatBox("ENTRY FEE", if (t.entryFee <= 0) "FREE" else "৳${t.entryFee.toInt()}", Color(0xFF38BDF8), Modifier.weight(1f))
                        }

                        // ---- DYNAMIC MULTI-RANK PRIZE TIERS ----
                        if (rankPrizes.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rankPrizes.forEach { tier ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.45f))
                                    ) {
                                        Text(
                                            text = "🏆 ${tier.rankLabel}: ৳${tier.amount.toInt()}",
                                            color = Color(0xFFFDE68A),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (t.rulesNotice.isNotBlank()) {
                            Text(
                                text = "📋 ${t.rulesNotice}",
                                color = Color(0xFFCBD5E1),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                    // Slot Progress Bar (Clickable to open Joined Players List)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isFull) {
                                    if (isBn) "সব সিট পূর্ণ — পরবর্তী ম্যাচের জন্য অপেক্ষা করুন" else "All Seats Full — Wait for Next Match"
                                } else {
                                    if (isBn) "${(t.maxSlots - effectiveJoinedCount).coerceAtLeast(0)} টি সিট খালি আছে"
                                    else "${(t.maxSlots - effectiveJoinedCount).coerceAtLeast(0)} Spots Left"
                                },
                                color = if (isFull) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { viewPlayersTarget = t }
                                    .testTag("view_joined_players_chip_${t.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Groups,
                                        contentDescription = if (isBn) "জয়েন হওয়া প্লেয়ার দেখুন" else "View Joined Players",
                                        tint = Color(0xFF7DD3FC),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isBn) "$effectiveJoinedCount/${t.maxSlots} প্লেয়ার দেখুন"
                                        else "$effectiveJoinedCount/${t.maxSlots} Players",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                        LinearProgressIndicator(
                            progress = { fillRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = if (isFull) Color(0xFFEF4444) else Color(0xFFF59E0B),
                            trackColor = Color.White.copy(alpha = 0.16f)
                        )
                    }

                    // Joined Room ID/Pass OR Join Button OR Winner Badge
                    if (t.status == "COMPLETED") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (t.winnerName.isNotBlank()) "🏆 Champion: ${t.winnerName}" else "🏁 Tournament Completed",
                                color = Color(0xFFFDE68A),
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else if (myEntry != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF064E3B),
                            border = BorderStroke(1.dp, Color(0xFF34D399)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "JOINED AS ${myEntry.inGameName} (UID: ${myEntry.inGameUid})",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                if (t.roomIdCode.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Room ID: ${t.roomIdCode}  •  Pass: ${t.roomPassword}",
                                            color = Color(0xFFFDE68A),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp
                                        )
                                        IconButton(onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cm?.setPrimaryClip(ClipData.newPlainText("Room", "ID: ${t.roomIdCode} Pass: ${t.roomPassword}"))
                                            Toast.makeText(context, "Room ID & Password Copied!", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Room", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                } else {
                                    Text(
                                        if (isBn) "ম্যাচ শুরুর ${t.preMatchAlertMinutes} মিনিট আগে Admin এখানে Room ID ও Password দেবেন।"
                                        else "Admin will publish Room ID & Password here ${t.preMatchAlertMinutes} mins before match.",
                                        color = Color(0xFFA7F3D0),
                                        fontSize = 11.sp
                                    )
                                }

                                // Dedicated Clickable Joined Players List Button
                                OutlinedButton(
                                    onClick = { viewPlayersTarget = t },
                                    border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .testTag("joined_players_list_btn_${t.id}")
                                ) {
                                    Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isBn) "কারা কারা জয়েন হয়েছে দেখুন ($effectiveJoinedCount/${t.maxSlots})"
                                        else "View All Joined Players ($effectiveJoinedCount/${t.maxSlots})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { joinTarget = t },
                            enabled = !isFull,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color.Black,
                                disabledContainerColor = Color(0xFF475569),
                                disabledContentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("join_esports_btn_${t.id}")
                        ) {
                            Text(
                                text = when {
                                    isFull -> if (isBn) "সিট পূর্ণ — পরের ম্যাচে চেষ্টা করুন" else "MATCH FULL — WAIT FOR NEXT"
                                    t.entryFee <= 0 -> if (isBn) "ফ্রি জয়েন করুন" else "JOIN FREE MATCH"
                                    else -> if (isBn) "জয়েন করুন (৳${t.entryFee.toInt()})" else "JOIN TOURNAMENT (৳${t.entryFee.toInt()})"
                                },
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    }
                }
            }
        }
    }

    joinTarget?.let { t ->
        var ign by remember(t.id) { mutableStateOf("") }
        var uid by remember(t.id) { mutableStateOf("") }
        val isValid = ign.trim().length >= 2 && uid.trim().length >= 5
        AlertDialog(
            onDismissRequest = { joinTarget = null },
            title = { Text("Join ${t.title}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Entry Fee: ৳${t.entryFee.toInt()} • Your Playable Balance: ৳${(wallet.tournamentBalance + wallet.winningBalance).toInt()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = ign,
                        onValueChange = { ign = it },
                        label = { Text(if (isBn) "Free Fire গেম আইডি নাম (IGN) *" else "Free Fire In-Game Name (IGN) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ff_join_ign_input")
                    )
                    OutlinedTextField(
                        value = uid,
                        onValueChange = { uid = it.filter { ch -> ch.isDigit() || ch == '-' } },
                        label = { Text(if (isBn) "Free Fire UID নম্বর *" else "Free Fire UID Number *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ff_join_uid_input")
                    )
                    Text(
                        text = if (isBn) "⚠️ সতর্কতা: হ্যাক বা প্যানেল ব্যবহার করলে আইডি স্থায়ীভাবে সাসপেন্ড করা হবে। ম্যাচের ${t.preMatchAlertMinutes} মিনিট আগে Room ID ও Password অ্যালার্ট পাবেন।"
                        else "⚠️ Fair Play Pledge: Using hacks/panels results in permanent suspension. Room ID & Pass alert arrives ${t.preMatchAlertMinutes} mins before start.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = isValid,
                    onClick = {
                        onJoinTournament(t.id, ign.trim(), uid.trim()) { joinTarget = null }
                    },
                    modifier = Modifier.testTag("ff_join_confirm_btn")
                ) { Text(if (isBn) "কনফার্ম ও জয়েন" else "Confirm & Join") }
            },
            dismissButton = { TextButton(onClick = { joinTarget = null }) { Text(if (isBn) "বাতিল" else "Cancel") } }
        )
    }

    viewPlayersTarget?.let { t ->
        val joinedList = entriesByTournament[t.id].orEmpty()
        JoinedPlayersListDialog(
            tournament = t,
            entries = joinedList,
            currentUserId = currentUserId,
            isBn = isBn,
            onDismiss = { viewPlayersTarget = null }
        )
    }
}

@Composable
private fun JoinedPlayersListDialog(
    tournament: ExternalTournamentEntity,
    entries: List<ExternalTournamentEntryEntity>,
    currentUserId: Long,
    isBn: Boolean,
    onDismiss: () -> Unit
) {
    var searchQuery by remember(tournament.id) { mutableStateOf("") }
    val filteredEntries = remember(entries, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) entries
        else entries.filter {
            it.inGameName.lowercase().contains(q) ||
                it.inGameUid.lowercase().contains(q) ||
                it.usernameSnapshot.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isBn) "জয়েন হওয়া প্লেয়ারদের তালিকা" else "Joined Players Roster",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = "${tournament.title}  •  ${entries.size}/${tournament.maxSlots} ${if (isBn) "জন জয়েন" else "Joined"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (entries.size > 4) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        placeholder = { Text(if (isBn) "IGN বা UID খুঁজুন..." else "Search IGN or UID...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("joined_players_search_input")
                    )
                }

                if (filteredEntries.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (entries.isEmpty()) {
                                if (isBn) "এখনো কেউ এই টুর্নামেন্টে জয়েন করেনি। প্রথম প্লেয়ার হিসেবে জয়েন করুন!"
                                else "No players have joined this match yet. Be the first to claim Slot #1!"
                            } else {
                                if (isBn) "এই নামে কোনো প্লেয়ার পাওয়া যায়নি।" else "No matching player found."
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .testTag("joined_players_lazy_list")
                    ) {
                        items(filteredEntries.size, key = { idx -> "jp_${filteredEntries[idx].id}" }) { idx ->
                            val entry = filteredEntries[idx]
                            val slotNumber = entries.indexOfFirst { it.id == entry.id }.takeIf { it >= 0 }?.plus(1) ?: (idx + 1)
                            val isMe = currentUserId > 0L && entry.ownerUserId == currentUserId

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isMe) Color(0xFF065F46).copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isMe) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isMe) Color(0xFF059669) else Color(0xFF1E293B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#$slotNumber",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = entry.inGameName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            if (isMe) {
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = Color(0xFFF59E0B)
                                                ) {
                                                    Text(
                                                        text = if (isBn) "আপনি" else "YOU",
                                                        color = Color.Black,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "UID: ${entry.inGameUid}  •  @${entry.usernameSnapshot}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${if (isBn) "জয়েন:" else "Joined:"} ${DateTimeUtil.formatDateTime(entry.joinedAt)}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("joined_players_close_btn")
            ) {
                Text(if (isBn) "বন্ধ করুন" else "Close")
            }
        }
    )
}

private fun formatCountdownLabel(remainingMs: Long, isBn: Boolean): String =
    TournamentTimeFormatter.formatCountdown(remainingMs, isBn)

@Composable
private fun EsportsStatBox(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
            Text(value, fontSize = 15.sp, color = accent, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/**
 * Per-Game Tournament Entry Tier Picker Dialog:
 * Shows Admin-configured Entry Fees -> Win Prizes, Live Multiplayer Seat Availability,
 * real waiting player names, and lets the player Scan Real Players (with Bot Fallback) or Play Local.
 */
@Composable
fun GameEntryTierPickerDialog(
    gameTitle: String,
    gameId: String,
    tiers: List<TournamentTierConfigEntity>,
    botConfig: GameBotConfigEntity,
    seatStatus: FirestoreMultiplayerRepository.LiveGameSeatStatus?,
    wallet: PlayWalletEntity,
    isBn: Boolean,
    onDismiss: () -> Unit,
    onScanAndPlayOnlineOrBot: (TournamentTierConfigEntity) -> Unit,
    onPlayInstantLocalBot: (TournamentTierConfigEntity, Int) -> Unit
) {
    val activeTiers = remember(tiers) { tiers.filter { it.isEnabled }.ifEmpty {
        listOf(
            TournamentTierConfigEntity(gameId = gameId, tierLabel = "Practice", entryFee = 0.0, winPrize = 0.0),
            TournamentTierConfigEntity(gameId = gameId, tierLabel = "Bronze", entryFee = 20.0, winPrize = 36.0),
            TournamentTierConfigEntity(gameId = gameId, tierLabel = "Silver", entryFee = 50.0, winPrize = 90.0),
            TournamentTierConfigEntity(gameId = gameId, tierLabel = "Gold", entryFee = 100.0, winPrize = 180.0)
        )
    } }
    var selectedTier by remember(activeTiers) { mutableStateOf(activeTiers.first()) }
    val isBusy = seatStatus?.isServerBusy == true
    val openSeats = seatStatus?.openSeats ?: botConfig.maxConcurrentSeats

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("🏆 $gameTitle", fontWeight = FontWeight.ExtraBold)
                Text(
                    text = if (isBn) "এন্ট্রি ফি ও উইনিং প্রাইজ সিলেক্ট করুন" else "Select Entry Fee & Win Prize",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Live Seat Capacity Status Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isBusy) Color(0xFF7F1D1D) else Color(0xFF064E3B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = if (isBusy) {
                                if (isBn) "🔴 সার্ভার ব্যস্ত! সব সিট পূর্ণ — একটি ম্যাচ শেষ হওয়া পর্যন্ত অপেক্ষা করুন"
                                else "🔴 Server Busy! All seats full — wait for a match to finish"
                            } else {
                                if (isBn) "🟢 অনলাইন মাল্টিপ্লেয়ার চালু • $openSeats/${botConfig.maxConcurrentSeats} সিট খালি"
                                else "🟢 Multiplayer Open • $openSeats/${botConfig.maxConcurrentSeats} Seats Free"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        val waitingNames = seatStatus?.waitingPlayerNames.orEmpty()
                        if (waitingNames.isNotEmpty()) {
                            Text(
                                text = "👤 Waiting Online: ${waitingNames.take(3).joinToString(", ")}",
                                color = Color(0xFFFDE68A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = if (isBn) "রিয়েল প্লেয়ার না থাকলে অটো বট (${botConfig.botAccuracyPercent}% অ্যাকুরেসি) খেলবে"
                                else "Auto-scans real players first; Bot (${botConfig.botAccuracyPercent}% Acc) joins if none waiting",
                                color = Color(0xFFA7F3D0),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Playable: ৳${(wallet.tournamentBalance + wallet.winningBalance).toInt()} • 100% Loss Cashback → Game Wallet",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                // Tier Cards List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(210.dp)
                ) {
                    items(activeTiers, key = { "${it.id}_${it.entryFee}" }) { tier ->
                        val isSel = selectedTier == tier
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTier = tier }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(tier.tierLabel, fontWeight = FontWeight.ExtraBold)
                                    Text(
                                        text = if (tier.entryFee <= 0.0) {
                                            if (isBn) "ফ্রি প্র্যাকটিস ম্যাচ" else "Free Practice Match"
                                        } else {
                                            if (isBn) "হারলে ৳${tier.entryFee.toInt()} গেম ওয়ালেটে ক্যাশব্যাক"
                                            else "Loss = ৳${tier.entryFee.toInt()} Cashback to Game Wallet"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (tier.entryFee <= 0.0) "FREE" else "Entry ৳${tier.entryFee.toInt()}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (tier.winPrize > 0.0) {
                                        Text(
                                            text = "🏆 Win ৳${tier.winPrize.toInt()}",
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF059669),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onScanAndPlayOnlineOrBot(selectedTier) },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_smart_match_btn")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isBn) "অনলাইন প্লেয়ার স্ক্যান করে খেলুন" else "Scan Real Players & Play",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                OutlinedButton(
                    onClick = { onPlayInstantLocalBot(selectedTier, botConfig.botAccuracyPercent) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isBn) "সরাসরি খেলুন (Bot ${botConfig.botAccuracyPercent}%)" else "Instant Play (Bot ${botConfig.botAccuracyPercent}%)"
                    )
                }
            }
        }
    )
}

/**
 * Animated Real-Player Radar Scanner & Server Busy Modal Dialog.
 */
@Composable
fun MatchmakingScanDialog(
    scanState: MatchmakingScanState,
    isBn: Boolean,
    onlineAuth: OnlineAuthProfile? = null,
    avatarGlyph: String = "👑",
    onCancel: () -> Unit
) {
    when (scanState) {
        MatchmakingScanState.Idle -> Unit
        is MatchmakingScanState.ServerBusy -> {
            AlertDialog(
                onDismissRequest = onCancel,
                containerColor = Color(0xFF101829),
                titleContentColor = Color(0xFFF8FAFC),
                textContentColor = Color(0xFFCBD5E1),
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(36.dp)) },
                title = { Text(if (isBn) "সার্ভার ব্যস্ত! সিট পূর্ণ" else "Server Busy — Seats Full", fontWeight = FontWeight.ExtraBold) },
                text = {
                    Text(
                        "${scanState.message}\n\n" +
                            if (isBn) "বর্তমানে একটি ম্যাচ শেষ হলে সিট খালি হবে, তখন আবার চেষ্টা করুন।"
                            else "A seat opens as soon as an active match finishes. Please try again shortly."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5C451), contentColor = Color(0xFF090E1A))
                    ) { Text(if (isBn) "ঠিক আছে" else "OK, I'll Wait", fontWeight = FontWeight.ExtraBold) }
                }
            )
        }
        is MatchmakingScanState.Scanning -> {
            val pulse by rememberInfiniteTransition(label = "radar").animateFloat(
                initialValue = 0.90f,
                targetValue = 1.12f,
                animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "radarScale"
            )
            val foundOpponent = scanState.detectedPlayerNames.firstOrNull {
                !it.equals(onlineAuth?.displayName.orEmpty(), ignoreCase = true)
            }
            AlertDialog(
                onDismissRequest = onCancel,
                containerColor = Color(0xFF101829),
                titleContentColor = Color(0xFFF5C451),
                textContentColor = Color(0xFFF8FAFC),
                title = {
                    Text(
                        text = if (isBn) "✦ অনলাইন VS ম্যাচমেকিং ✦" else "✦ ONLINE VS MATCHMAKING ✦",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Dual Side-by-Side VS Showcase Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF141F35),
                            border = BorderStroke(1.2.dp, Color(0xFFF5C451).copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Left Slot: Local Player (Name + Google Profile Photo)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF042F2E))
                                            .border(2.dp, Color(0xFFF5C451), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!onlineAuth?.photoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = onlineAuth?.photoUrl,
                                                contentDescription = onlineAuth?.displayName ?: "You",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(avatarGlyph, fontSize = 28.sp)
                                        }
                                    }
                                    Text(
                                        text = onlineAuth?.displayName ?: (if (isBn) "আপনি" else "You"),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFF8FAFC),
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                    Surface(shape = RoundedCornerShape(50), color = Color(0xFF10B981).copy(alpha = 0.22f)) {
                                        Text(
                                            text = if (isBn) "প্রস্তুত" else "READY",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF6EE7B7),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Center VS Medallion
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(44.dp)
                                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFFF5C451), Color(0xFFD49B27))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "VS",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = Color(0xFF090E1A)
                                    )
                                }

                                // Right Slot: Live Online Opponent Scanner
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .graphicsLayer {
                                                if (foundOpponent == null) {
                                                    scaleX = pulse
                                                    scaleY = pulse
                                                }
                                            }
                                            .clip(CircleShape)
                                            .background(Color(0xFF042F2E))
                                            .border(
                                                2.dp,
                                                if (foundOpponent != null) Color(0xFF10B981) else Color(0xFF6EE7B7).copy(alpha = 0.55f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (foundOpponent != null) {
                                            Text("⚔️", fontSize = 26.sp)
                                        } else {
                                            Icon(
                                                Icons.Default.Radar,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = foundOpponent ?: (if (isBn) "খোঁজা হচ্ছে..." else "Searching..."),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = if (foundOpponent != null) Color(0xFF6EE7B7) else Color(0xFF94A3B8),
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFF5C451).copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "${scanState.secondsRemaining}s",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFF5C451),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            "${scanState.gameId.replace("_", " ")} • ${scanState.tierLabel} (৳${scanState.entryFee.toInt()} → Win ৳${scanState.winPrize.toInt()})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFF5C451),
                            textAlign = TextAlign.Center
                        )

                        if (scanState.detectedPlayerNames.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF042F2E),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Online Players: ${scanState.detectedPlayerNames.take(3).joinToString(", ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF8FAFC)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isBn) "কোনো রিয়েল প্লেয়ার না পাওয়া গেলে স্বয়ংক্রিয়ভাবে Bot (${scanState.botAccuracyFallback}% অ্যাকুরেসি) ম্যাচ শুরু হবে!"
                            else "If no live challenger joins in time, Arena Bot (${scanState.botAccuracyFallback}% Accuracy) steps in automatically!",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    OutlinedButton(
                        onClick = onCancel,
                        border = BorderStroke(1.dp, Color(0xFFF5C451).copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF5C451))
                    ) { Text(if (isBn) "বাতিল" else "Cancel Scan") }
                }
            )
        }
    }
}

/**
 * Glowing Admin Broadcast Notification Banner shown inside Play & Earn Hub.
 */
@Composable
fun LatestBroadcastBanner(
    broadcast: BroadcastNotificationEntity,
    modifier: Modifier = Modifier
) {
    val colors = when (broadcast.priority) {
        "URGENT" -> listOf(Color(0xFF991B1B), Color(0xFFDC2626))
        "TOURNAMENT" -> listOf(Color(0xFF78350F), Color(0xFFD97706))
        "PROMO" -> listOf(Color(0xFF4C1D95), Color(0xFF7C3AED))
        else -> listOf(Color(0xFF064E3B), Color(0xFF047857))
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .background(Brush.horizontalGradient(colors))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFFDE68A), modifier = Modifier.size(26.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "[${broadcast.priority}] ${broadcast.title}",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    broadcast.message,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
        }
    }
}
