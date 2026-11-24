package com.example.ui.screens.games

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.R
import com.example.data.games.CloudTournamentDoc
import com.example.data.games.CloudTournamentEntryDoc
import com.example.data.games.TournamentHubItem
import com.example.data.games.TournamentHubMerger
import com.example.data.games.TournamentRosterMember
import com.example.data.local.ExternalTournamentEntity
import com.example.data.local.ExternalTournamentEntryEntity
import com.example.data.local.PlayWalletEntity
import com.example.domain.DateTimeUtil
import com.example.domain.games.FreeFireMapBanner
import com.example.domain.games.TournamentPrizeCodec
import com.example.domain.games.TournamentTimeFormatter
import com.example.ui.viewmodel.OnlineAuthProfile
import kotlinx.coroutines.delay

/**
 * Centralized Firestore Live Tournament Hub:
 * Streams active tournaments across Free Fire Esports and all 5 in-built Arena games
 * (including 5-Player Star Ludo) with per-second live countdown timers, real-time
 * Firestore slot sync, clickable live Joined Players Roster, and seamless Room offline fallback.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TournamentHubView(
    cloudTournaments: List<CloudTournamentDoc>,
    localTournaments: List<ExternalTournamentEntity>,
    myLocalEntries: List<ExternalTournamentEntryEntity>,
    allLocalEntries: List<ExternalTournamentEntryEntity>,
    joinedCloudEntries: Map<String, CloudTournamentEntryDoc>,
    inspectedCloudRoster: List<CloudTournamentEntryDoc>,
    onlineAuth: OnlineAuthProfile?,
    currentUserId: Long,
    wallet: PlayWalletEntity,
    isBn: Boolean,
    onSignInGoogle: () -> Unit,
    onInspectRoster: (String?) -> Unit,
    onTriggerPreMatchAlert: (ExternalTournamentEntity) -> Unit,
    onJoinHubTournament: (TournamentHubItem, String, String, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showCompleted by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var joinTarget by remember { mutableStateOf<TournamentHubItem?>(null) }
    var rosterTarget by remember { mutableStateOf<TournamentHubItem?>(null) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "hubLivePulse")
    val livePulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "liveDotAlpha"
    )

    val mergedItems = remember(cloudTournaments, localTournaments) {
        TournamentHubMerger.mergeCloudAndLocal(cloudTournaments, localTournaments)
    }
    val myLocalEntryByTournament = remember(myLocalEntries) { myLocalEntries.associateBy { it.tournamentId } }
    val localEntriesByTournament = remember(allLocalEntries) { allLocalEntries.groupBy { it.tournamentId } }

    val statusFiltered = remember(mergedItems, showCompleted) {
        if (showCompleted) mergedItems.filter { it.status == "COMPLETED" }
        else mergedItems.filter { it.status != "COMPLETED" }
    }
    val filtered = remember(statusFiltered, selectedCategory) {
        if (selectedCategory == "ALL") statusFiltered
        else statusFiltered.filter { it.gameCategory.equals(selectedCategory, ignoreCase = true) }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("tournament_hub_lazy_column")
    ) {
        // ---- 1. LIVE FIRESTORE SYNC STATUS STRIP ----
        item(key = "hub_sync_strip") {
            val isLive = onlineAuth != null
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isLive) Color(0xFF064E3B) else Color(0xFF1E293B),
                border = BorderStroke(
                    1.2.dp,
                    if (isLive) Color(0xFF34D399) else Color(0xFFF59E0B).copy(alpha = 0.7f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tournament_hub_sync_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .graphicsLayer { alpha = if (isLive) livePulseAlpha else 1f }
                                .clip(CircleShape)
                                .background(if (isLive) Color(0xFF34D399) else Color(0xFFF59E0B))
                        )
                        Icon(
                            imageVector = if (isLive) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isLive) Color(0xFF6EE7B7) else Color(0xFFFDE68A),
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = if (isLive) {
                                    if (isBn) "Firestore লাইভ টুর্নামেন্ট হাব চালু" else "Firestore Live Tournament Hub Active"
                                } else {
                                    if (isBn) "অফলাইন মোড — লাইভ সিঙ্কের জন্য সাইন-ইন করুন" else "Offline Cache — Sign in for Live Cloud Sync"
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isLive) {
                                    if (isBn) "সব ডিভাইসে রিয়েল-টাইম কাউন্টডাউন, স্লট ও রোস্টার আপডেট হচ্ছে (${cloudTournaments.size} ক্লাউড ম্যাচ)"
                                    else "Real-time countdowns, live slots & rosters synced (${cloudTournaments.size} cloud matches)"
                                } else {
                                    if (isBn) "Google সাইন-ইন করলে Admin-এর নতুন টুর্নামেন্ট ও রুম অ্যালার্ট লাইভ আসবে"
                                    else "Connect Google to stream live Admin tournaments & instant room updates"
                                },
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp
                            )
                        }
                    }
                    if (!isLive) {
                        Button(
                            onClick = onSignInGoogle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("tournament_hub_signin_btn")
                        ) {
                            Text(if (isBn) "সাইন-ইন" else "Sync", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // ---- 2. HERO BANNER ----
        item(key = "hub_hero_banner") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.75f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(166.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_esports_battle_banner),
                        contentDescription = "Centralized Tournament Hub",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF090D16).copy(alpha = 0.30f),
                                        Color(0xFF090D16).copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = Color(0xFFEF4444)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("CENTRALIZED LIVE TOURNAMENT HUB", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Text(
                            text = if (isBn) "Free Fire ও ৫-প্লেয়ার লুডু লাইভ টুর্নামেন্ট" else "Free Fire, 5P Ludo & Arena Cups",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (isBn) "রিয়েল-টাইম কাউন্টডাউন, লাইভ প্লেয়ার রোস্টার ও সরাসরি Winning Balance প্রাইজ!"
                            else "Real-time countdowns, live joined rosters & instant withdrawable Winning Coins!",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFDE68A)
                        )
                    }
                }
            }
        }

        // ---- 3. ANTI-CHEAT DIRECTIVE ----
        item(key = "hub_anticheat_directive") {
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
                    Icon(Icons.Default.GppBad, contentDescription = "Anti-Cheat Policy", tint = Color(0xFFFCA5A5), modifier = Modifier.size(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isBn) "⚠️ ফেয়ার-প্লে ও অ্যান্টি-হ্যাক নীতিমালা" else "⚠️ Anti-Cheat & Fair Play Policy",
                            color = Color(0xFFFECACA),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = if (isBn) "• Hack, Panel, Config, Teaming বা Emulator Bypass করলে অ্যাকাউন্ট স্থায়ীভাবে SUSPEND হবে। সঠিক গেম আইডি দিয়ে জয়েন করুন।"
                            else "• Hacks, Panels, Configs, Teaming, or Emulator Bypass = PERMANENT SUSPENSION. Enter accurate IGN/UID to unlock Room ID & Pass.",
                            color = Color(0xFFFEE2E2),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // ---- 4. MULTI-GAME CATEGORY FILTER CHIPS ----
        item(key = "hub_category_filters") {
            val categories = listOf(
                "ALL" to (if (isBn) "সব গেম" else "All Games"),
                "FREE_FIRE" to "Free Fire",
                "LUDO" to (if (isBn) "লুডু ২–৫P" else "Ludo 2–5P"),
                "CHESS" to (if (isBn) "দাবা" else "Chess"),
                "AETHER_CROWN" to "Aether Crown",
                "REVERSI" to "Reversi",
                "CONNECT_FOUR" to "Connect 4"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { (code, label) ->
                    val count = if (code == "ALL") statusFiltered.size
                    else statusFiltered.count { it.gameCategory.equals(code, ignoreCase = true) }
                    FilterChip(
                        selected = selectedCategory == code,
                        onClick = { selectedCategory = code },
                        label = {
                            Text(
                                text = if (count > 0 && code != "ALL") "$label ($count)" else label,
                                fontSize = 11.sp,
                                fontWeight = if (selectedCategory == code) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("hub_cat_chip_${code.lowercase()}")
                    )
                }
            }
        }

        // ---- 5. UPCOMING/LIVE vs COMPLETED SWITCH ----
        item(key = "hub_status_switch") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = !showCompleted,
                    onClick = { showCompleted = false },
                    label = {
                        val activeCnt = mergedItems.count { it.status != "COMPLETED" }
                        Text(if (isBn) "🔥 লাইভ / আসন্ন ($activeCnt)" else "🔥 Live / Upcoming ($activeCnt)", fontWeight = FontWeight.Bold)
                    },
                    modifier = Modifier.weight(1f).testTag("hub_filter_live")
                )
                FilterChip(
                    selected = showCompleted,
                    onClick = { showCompleted = true },
                    label = {
                        val compCnt = mergedItems.count { it.status == "COMPLETED" }
                        Text(if (isBn) "🏆 সম্পন্ন ($compCnt)" else "🏆 Completed ($compCnt)", fontWeight = FontWeight.Bold)
                    },
                    modifier = Modifier.weight(1f).testTag("hub_filter_completed")
                )
            }
        }

        if (filtered.isEmpty()) {
            item(key = "hub_empty_state") {
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
                                if (isBn) "কোনো সম্পন্ন টুর্নামেন্ট হিস্ট্রি নেই।" else "No completed tournaments in this category."
                            } else {
                                if (isBn) "এই ক্যাটাগরিতে এখনো কোনো লাইভ টুর্নামেন্ট নেই — Admin ম্যাচ লঞ্চ করলে সাথে সাথে এখানে দেখাবে!"
                                else "No active tournaments in this category yet — newly launched matches stream here live!"
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ---- 6. REAL-TIME TOURNAMENT CARDS ----
        items(filtered, key = { it.key }) { item ->
            val myLocal = item.localId?.let { myLocalEntryByTournament[it] }
            val myCloud = item.cloudDocId?.let { joinedCloudEntries[it] }
            val inPlayerUids = onlineAuth != null && onlineAuth.uid in item.playerUids
            val isJoined = myLocal != null || myCloud != null || inPlayerUids
            val joinedIgn = myCloud?.inGameName ?: myLocal?.inGameName ?: onlineAuth?.displayName.orEmpty()
            val joinedUid = myCloud?.inGameUid ?: myLocal?.inGameUid ?: onlineAuth?.uid?.take(10).orEmpty()

            val localEntries = item.localId?.let { localEntriesByTournament[it] }.orEmpty()
            val effectiveJoinedCount = maxOf(item.joinedCount, item.playerUids.size, localEntries.size)
            val isFull = effectiveJoinedCount >= item.maxSlots
            val fillRatio = (effectiveJoinedCount.toFloat() / item.maxSlots.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            val mapBanner = remember(item.bannerKey, item.mapName) {
                FreeFireMapBanner.fromKeyOrMap(item.bannerKey, item.mapName)
            }
            val rankPrizes = remember(item.prizeRanksJson) {
                TournamentPrizeCodec.parseRanks(item.prizeRanksJson)
            }
            val remainingMs = if (item.matchStartTimeMillis > 0L) item.matchStartTimeMillis - nowMillis else 0L
            val isPreMatchAlertWindow = isJoined &&
                item.status != "COMPLETED" &&
                item.matchStartTimeMillis > 0L &&
                remainingMs in 1L..(item.preMatchAlertMinutes.coerceAtLeast(1) * 60_000L)

            LaunchedEffect(item.key, isPreMatchAlertWindow) {
                if (isPreMatchAlertWindow) {
                    onTriggerPreMatchAlert(item.toLocalEntitySnapshot())
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(
                    1.4.dp,
                    when {
                        item.status == "LIVE" -> Color(0xFFEF4444)
                        isJoined -> Color(0xFF10B981)
                        item.isLiveCloudSynced -> Color(0xFF38BDF8).copy(alpha = 0.75f)
                        else -> Color(0xFFF59E0B).copy(alpha = 0.65f)
                    }
                ),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hub_tournament_card_${item.key}")
            ) {
                Column(
                    modifier = Modifier.background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF172554))
                        )
                    )
                ) {
                    // Banner Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(144.dp)
                    ) {
                        Image(
                            painter = painterResource(id = mapBanner.bannerRes),
                            contentDescription = "${item.title} Banner",
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
                                            Color(0xFF0F172A).copy(alpha = 0.93f)
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
                                    color = Color(0xFF090D16).copy(alpha = 0.82f),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.7f))
                                ) {
                                    Text(
                                        "${item.gameCategory.replace("_", " ")} • ${item.modeLabel}",
                                        color = Color(0xFFFDE68A),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                if (item.isLiveCloudSynced) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF065F46).copy(alpha = 0.92f)
                                    ) {
                                        Text(
                                            "LIVE SYNC",
                                            color = Color(0xFFA7F3D0),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = when (item.status) {
                                    "LIVE" -> Color(0xFFEF4444)
                                    "COMPLETED" -> Color(0xFF475569)
                                    else -> Color(0xFF059669)
                                }
                            ) {
                                Text(
                                    item.status,
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
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            val scheduleCaption = item.startTimeLabel.ifBlank {
                                TournamentTimeFormatter.formatScheduleLabel(item.matchStartTimeMillis)
                            }
                            Text(
                                text = "🕒 $scheduleCaption  •  ${mapBanner.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }

                    // Card Body
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Real-Time Countdown Bar
                        if (item.status != "COMPLETED" && item.matchStartTimeMillis > 0L) {
                            val isUrgent = remainingMs <= item.preMatchAlertMinutes * 60_000L && remainingMs > 0L
                            val countdownText = TournamentTimeFormatter.formatCountdown(remainingMs, isBn)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isUrgent) Color(0xFF991B1B) else Color(0xFF0F766E).copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, if (isUrgent) Color(0xFFFCA5A5) else Color(0xFF2DD4BF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFFDE68A), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (isBn) "লাইভ কাউন্টডাউন" else "Live Match Countdown",
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

                        // Pre-match Alert Banner
                        if (isPreMatchAlertWindow || (isJoined && item.roomAlertSent && item.status != "COMPLETED")) {
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
                                        text = if (isBn) "🔥 অ্যালার্ট: Room ID ও Password দেওয়া হয়েছে — দ্রুত লগইন করে জয়েন হন!"
                                        else "🔥 ROOM ALERT: Room ID & Password released — Join the match lobby now!",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HubStatTile("PRIZE POOL", "৳${item.prizePool.toInt()}", Color(0xFFFBBF24), Modifier.weight(1f))
                            if (item.perKillReward > 0.0) {
                                HubStatTile("PER KILL", "৳${item.perKillReward.toInt()}", Color(0xFF34D399), Modifier.weight(1f))
                            } else {
                                HubStatTile("MODE", item.modeLabel.take(10), Color(0xFF34D399), Modifier.weight(1f))
                            }
                            HubStatTile("ENTRY", if (item.entryFee <= 0) "FREE" else "৳${item.entryFee.toInt()}", Color(0xFF38BDF8), Modifier.weight(1f))
                        }

                        // Multi-Rank Prizes
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

                        if (item.rulesNotice.isNotBlank()) {
                            Text("📋 ${item.rulesNotice}", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelSmall)
                        }

                        // Live Slot Occupancy + Clickable Live Roster Chip
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isFull) {
                                        if (isBn) "সব সিট পূর্ণ" else "All Seats Full"
                                    } else {
                                        if (isBn) "${(item.maxSlots - effectiveJoinedCount).coerceAtLeast(0)} টি সিট খালি"
                                        else "${(item.maxSlots - effectiveJoinedCount).coerceAtLeast(0)} Spots Left"
                                    },
                                    color = if (isFull) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF38BDF8).copy(alpha = 0.22f),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            rosterTarget = item
                                            onInspectRoster(item.cloudDocId)
                                        }
                                        .testTag("hub_roster_chip_${item.key}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Groups, contentDescription = "View Live Roster", tint = Color(0xFF7DD3FC), modifier = Modifier.size(14.dp))
                                        Text(
                                            text = if (isBn) "$effectiveJoinedCount/${item.maxSlots} প্লেয়ার দেখুন"
                                            else "$effectiveJoinedCount/${item.maxSlots} Roster",
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

                        // Joined Vault OR Join Action OR Winner Badge
                        if (item.status == "COMPLETED") {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (item.winnerName.isNotBlank()) "🏆 Champion: ${item.winnerName}" else "🏁 Tournament Completed",
                                    color = Color(0xFFFDE68A),
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else if (isJoined) {
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
                                            "JOINED AS $joinedIgn (ID: $joinedUid)",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (item.roomIdCode.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Room ID: ${item.roomIdCode}  •  Pass: ${item.roomPassword}",
                                                color = Color(0xFFFDE68A),
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp
                                            )
                                            IconButton(onClick = {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                cm?.setPrimaryClip(ClipData.newPlainText("Room", "ID: ${item.roomIdCode} Pass: ${item.roomPassword}"))
                                                Toast.makeText(context, "Room ID & Password Copied!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Room", tint = Color.White, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    } else {
                                        Text(
                                            if (isBn) "ম্যাচ শুরুর ${item.preMatchAlertMinutes} মিনিট আগে Admin এখানে Room ID ও Password দেবেন।"
                                            else "Admin will push Room ID & Password here ${item.preMatchAlertMinutes} mins before match.",
                                            color = Color(0xFFA7F3D0),
                                            fontSize = 11.sp
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            rosterTarget = item
                                            onInspectRoster(item.cloudDocId)
                                        },
                                        border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("hub_view_roster_btn_${item.key}")
                                    ) {
                                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(17.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (isBn) "লাইভ প্লেয়ার রোস্টার দেখুন ($effectiveJoinedCount/${item.maxSlots})"
                                            else "View Live Joined Roster ($effectiveJoinedCount/${item.maxSlots})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = { joinTarget = item },
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
                                    .testTag("hub_join_btn_${item.key}")
                            ) {
                                Text(
                                    text = when {
                                        isFull -> if (isBn) "সিট পূর্ণ — পরের ম্যাচে চেষ্টা করুন" else "MATCH FULL — WAIT FOR NEXT"
                                        item.entryFee <= 0 -> if (isBn) "ফ্রি জয়েন করুন" else "JOIN FREE MATCH"
                                        else -> if (isBn) "জয়েন করুন (৳${item.entryFee.toInt()})" else "JOIN TOURNAMENT (৳${item.entryFee.toInt()})"
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

    // ---- JOIN TOURNAMENT DIALOG ----
    joinTarget?.let { item ->
        val isEsports = item.gameCategory in setOf("FREE_FIRE", "PUBG", "EFOOTBALL", "ESPORTS")
        var ign by remember(item.key) { mutableStateOf(onlineAuth?.displayName.orEmpty().takeIf { !isEsports }.orEmpty()) }
        var uid by remember(item.key) { mutableStateOf(if (!isEsports) "FINPLAY-${currentUserId.coerceAtLeast(1L)}" else "") }
        val isValid = ign.trim().length >= 2 && uid.trim().length >= 4

        AlertDialog(
            onDismissRequest = { joinTarget = null },
            title = { Text("Join ${item.title}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Entry Fee: ৳${item.entryFee.toInt()} • Playable Balance: ৳${(wallet.tournamentBalance + wallet.winningBalance).toInt()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = ign,
                        onValueChange = { ign = it },
                        label = {
                            Text(
                                if (isEsports) {
                                    if (isBn) "গেম আইডি নাম (IGN) *" else "In-Game Name (IGN) *"
                                } else {
                                    if (isBn) "আপনার প্লেয়ার নাম *" else "Player Display Name *"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ff_join_ign_input")
                    )
                    OutlinedTextField(
                        value = uid,
                        onValueChange = { uid = if (isEsports) it.filter { ch -> ch.isDigit() || ch == '-' } else it },
                        label = {
                            Text(
                                if (isEsports) {
                                    if (isBn) "গেম UID নম্বর *" else "In-Game UID Number *"
                                } else {
                                    if (isBn) "প্লেয়ার ট্যাগ / হ্যান্ডেল *" else "Player Tag / Handle *"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ff_join_uid_input")
                    )
                    Text(
                        text = if (isBn) "⚠️ সতর্কতা: হ্যাক বা চিটিং করলে আইডি স্থায়ীভাবে সাসপেন্ড করা হবে।"
                        else "⚠️ Fair Play Pledge: Cheating or hacks lead to permanent suspension.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = isValid,
                    onClick = {
                        onJoinHubTournament(item, ign.trim(), uid.trim()) { joinTarget = null }
                    },
                    modifier = Modifier.testTag("ff_join_confirm_btn")
                ) { Text(if (isBn) "কনফার্ম ও জয়েন" else "Confirm & Join") }
            },
            dismissButton = { TextButton(onClick = { joinTarget = null }) { Text(if (isBn) "বাতিল" else "Cancel") } }
        )
    }

    // ---- LIVE JOINED PLAYERS ROSTER DIALOG ----
    rosterTarget?.let { item ->
        val localEntries = item.localId?.let { localEntriesByTournament[it] }.orEmpty()
        val cloudEntries = if (item.cloudDocId != null) inspectedCloudRoster else emptyList()
        val roster = remember(cloudEntries, localEntries, onlineAuth?.uid, currentUserId) {
            TournamentHubMerger.mergeRosterMembers(
                cloudEntries = cloudEntries,
                localEntries = localEntries,
                currentCloudUid = onlineAuth?.uid,
                currentLocalUserId = currentUserId
            )
        }
        LiveTournamentRosterDialog(
            item = item,
            roster = roster,
            isBn = isBn,
            onDismiss = {
                rosterTarget = null
                onInspectRoster(null)
            }
        )
    }
}

@Composable
private fun LiveTournamentRosterDialog(
    item: TournamentHubItem,
    roster: List<TournamentRosterMember>,
    isBn: Boolean,
    onDismiss: () -> Unit
) {
    var searchQuery by remember(item.key) { mutableStateOf("") }
    val filteredRoster = remember(roster, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) roster
        else roster.filter {
            it.inGameName.lowercase().contains(q) ||
                it.inGameUid.lowercase().contains(q) ||
                it.usernameSnapshot.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (isBn) "লাইভ জয়েন প্লেয়ার রোস্টার" else "Live Joined Players Roster",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = "${item.title}  •  ${maxOf(roster.size, item.joinedCount)}/${item.maxSlots} ${if (isBn) "জন জয়েন" else "Joined"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (roster.size > 4) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        placeholder = { Text(if (isBn) "IGN বা UID খুঁজুন..." else "Search IGN or UID...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("joined_players_search_input")
                    )
                }
                if (filteredRoster.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (roster.isEmpty()) {
                                if (isBn) "এখনো কেউ জয়েন করেনি — প্রথম প্লেয়ার হিসেবে স্লট #১ দখল করুন!"
                                else "No players joined yet — be the first to claim Slot #1!"
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
                        items(filteredRoster.size, key = { idx -> filteredRoster[idx].key }) { idx ->
                            val member = filteredRoster[idx]
                            val slotNumber = roster.indexOfFirst { it.key == member.key }.takeIf { it >= 0 }?.plus(1) ?: (idx + 1)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (member.isCurrentUser) Color(0xFF065F46).copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    1.dp,
                                    if (member.isCurrentUser) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant
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
                                            .background(if (member.isCurrentUser) Color(0xFF059669) else Color(0xFF1E293B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("#$slotNumber", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(member.inGameName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold)
                                            if (member.isCurrentUser) {
                                                Surface(shape = RoundedCornerShape(50), color = Color(0xFFF59E0B)) {
                                                    Text(
                                                        if (isBn) "আপনি" else "YOU",
                                                        color = Color.Black,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            if (member.isCloudVerified) {
                                                Surface(shape = RoundedCornerShape(50), color = Color(0xFF0EA5E9).copy(alpha = 0.2f)) {
                                                    Text(
                                                        "LIVE",
                                                        color = Color(0xFF0284C7),
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "ID: ${member.inGameUid}  •  @${member.usernameSnapshot.ifBlank { "player" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${if (isBn) "জয়েন:" else "Joined:"} ${DateTimeUtil.formatDateTime(member.joinedAtMillis)}",
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
            Button(onClick = onDismiss, modifier = Modifier.testTag("joined_players_close_btn")) {
                Text(if (isBn) "বন্ধ করুন" else "Close")
            }
        }
    )
}

@Composable
private fun HubStatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
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
            Text(value, fontSize = 14.sp, color = accent, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}
