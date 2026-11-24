package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.data.games.PlayWalletRepository
import com.example.data.local.BroadcastNotificationEntity
import com.example.data.local.ExternalTournamentEntity
import com.example.data.local.GameBotConfigEntity
import com.example.data.local.PlayWalletEntity
import com.example.data.local.TournamentTierConfigEntity
import com.example.data.local.UserEntity
import com.example.domain.DateTimeUtil
import com.example.domain.games.FreeFireMapBanner
import com.example.domain.games.RankPrizeTier
import com.example.domain.games.TournamentPrizeCodec
import com.example.domain.games.TournamentTimeFormatter
import com.example.ui.components.LedgerCard
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Enhanced Admin Users Tab with live Username Search + Per-User Coin/Balance Editor
 * (Tournament Deposit Balance, Winning Coins Balance, and Game Wallet Coins).
 */
@Composable
fun AdminUsersWithCoinsTab(
    users: List<UserEntity>,
    wallets: List<PlayWalletEntity>,
    onAddUser: (String, String, String) -> Unit,
    onToggleLockUser: (UserEntity) -> Unit,
    onResetUserPassword: (UserEntity, String) -> Unit,
    onDeleteUser: (Long) -> Unit,
    onAdjustUserWallet: (Long, Double, Double, Double, String) -> Unit
) {
    val walletByUser = remember(wallets) { wallets.associateBy { it.ownerUserId } }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var resetTarget by remember { mutableStateOf<UserEntity?>(null) }
    var coinEditTarget by remember { mutableStateOf<UserEntity?>(null) }

    val filteredUsers = remember(users, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) users
        else users.filter {
            it.username.lowercase().contains(q) || it.displayName.lowercase().contains(q)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "admin_user_search_bar") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search user by username or name") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_user_search_input")
                )
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add New User Account")
                }
            }
        }

        if (filteredUsers.isEmpty()) {
            item(key = "empty_users_match") {
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No users match \"$searchQuery\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(filteredUsers, key = { it.id }) { user ->
            val wallet = walletByUser[user.id] ?: PlayWalletEntity(ownerUserId = user.id)
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${user.displayName} (@${user.username})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (user.isLocked) "Status: LOCKED" else "Status: ACTIVE • ID #${user.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (user.isLocked) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                        )
                    }
                    Row {
                        IconButton(onClick = { onToggleLockUser(user) }) {
                            Icon(
                                if (user.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = "Toggle Lock"
                            )
                        }
                        IconButton(onClick = { resetTarget = user }) {
                            Icon(Icons.Default.Key, contentDescription = "Reset Password")
                        }
                        if (users.size > 1) {
                            IconButton(onClick = { onDeleteUser(user.id) }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AdminMiniBalancePill("Deposit", "৳${wallet.tournamentBalance.toInt()}", Color(0xFF059669), Modifier.weight(1f))
                    AdminMiniBalancePill("Winning", "৳${wallet.winningBalance.toInt()}", Color(0xFFD97706), Modifier.weight(1f))
                    AdminMiniBalancePill("Game", "৳${wallet.gameBalance.toInt()}", Color(0xFF7C3AED), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { coinEditTarget = user },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_edit_coins_${user.username}"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add / Modify User Coins", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    coinEditTarget?.let { target ->
        val w = walletByUser[target.id] ?: PlayWalletEntity(ownerUserId = target.id)
        var tourneyDelta by remember(target.id) { mutableStateOf("") }
        var winningDelta by remember(target.id) { mutableStateOf("") }
        var gameDelta by remember(target.id) { mutableStateOf("") }
        var note by remember(target.id) { mutableStateOf("Admin Bonus / Balance Update") }

        AlertDialog(
            onDismissRequest = { coinEditTarget = null },
            title = { Text("Modify Coins • @${target.username}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Current: Deposit ৳${w.tournamentBalance.toInt()} | Winning ৳${w.winningBalance.toInt()} | Game ৳${w.gameBalance.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Use positive numbers to ADD (e.g. 100) or negative to DEDUCT (e.g. -50).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = tourneyDelta,
                        onValueChange = { tourneyDelta = it.filter { ch -> ch.isDigit() || ch == '-' || ch == '.' } },
                        label = { Text("Tournament Deposit Delta (+/-)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = winningDelta,
                        onValueChange = { winningDelta = it.filter { ch -> ch.isDigit() || ch == '-' || ch == '.' } },
                        label = { Text("Winning Coins Delta (+/-) [Withdrawable]") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gameDelta,
                        onValueChange = { gameDelta = it.filter { ch -> ch.isDigit() || ch == '-' || ch == '.' } },
                        label = { Text("Game Wallet Delta (+/-)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Audit Reason / Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val td = tourneyDelta.toDoubleOrNull() ?: 0.0
                    val wd = winningDelta.toDoubleOrNull() ?: 0.0
                    val gd = gameDelta.toDoubleOrNull() ?: 0.0
                    if (td != 0.0 || wd != 0.0 || gd != 0.0) {
                        onAdjustUserWallet(target.id, td, wd, gd, note)
                    }
                    coinEditTarget = null
                }) { Text("Apply Balance") }
            },
            dismissButton = {
                TextButton(onClick = { coinEditTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddDialog) {
        var u by remember { mutableStateOf("") }
        var p by remember { mutableStateOf("") }
        var d by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New User") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = d, onValueChange = { d = it }, label = { Text("Display Name") }, singleLine = true)
                    OutlinedTextField(value = u, onValueChange = { u = it }, label = { Text("Username") }, singleLine = true)
                    OutlinedTextField(value = p, onValueChange = { p = it }, label = { Text("Password") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (u.isNotBlank() && p.isNotBlank()) {
                        onAddUser(u.trim(), p, d.trim().ifEmpty { u.trim() })
                        showAddDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }

    resetTarget?.let { target ->
        var newPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { resetTarget = null },
            title = { Text("Reset Password • @${target.username}") },
            text = {
                OutlinedTextField(value = newPass, onValueChange = { newPass = it }, label = { Text("New Password") }, singleLine = true)
            },
            confirmButton = {
                Button(onClick = {
                    if (newPass.isNotBlank()) {
                        onResetUserPassword(target, newPass)
                        resetTarget = null
                    }
                }) { Text("Update") }
            },
            dismissButton = { TextButton(onClick = { resetTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminMiniBalancePill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/**
 * All-In-One Admin Tournaments, Free Fire Esports & Per-Game Bot Accuracy Control Tab.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminTournamentsAndBotsTab(
    tiers: List<TournamentTierConfigEntity>,
    botConfigs: List<GameBotConfigEntity>,
    externalTournaments: List<ExternalTournamentEntity>,
    onSaveTier: (TournamentTierConfigEntity) -> Unit,
    onDeleteTier: (Long) -> Unit,
    onSaveBotConfig: (GameBotConfigEntity) -> Unit,
    onCreateExternalTournament: (ExternalTournamentEntity) -> Unit,
    onUpdateExternalTournament: (ExternalTournamentEntity) -> Unit,
    onSendRoomAlert: (Long) -> Unit = {},
    onCompleteExternalTournament: (Long, String, Double) -> Unit,
    onDeleteExternalTournament: (Long) -> Unit
) {
    var selectedGameId by remember { mutableStateOf("LUDO") }
    var showAddTierDialog by remember { mutableStateOf(false) }
    var showCreateEsportsDialog by remember { mutableStateOf(false) }
    var completeTarget by remember { mutableStateOf<ExternalTournamentEntity?>(null) }
    var roomEditTarget by remember { mutableStateOf<ExternalTournamentEntity?>(null) }
    var scheduleEditTarget by remember { mutableStateOf<ExternalTournamentEntity?>(null) }

    val botMap = remember(botConfigs) { botConfigs.associateBy { it.gameId } }
    val selectedGameTiers = remember(tiers, selectedGameId) { tiers.filter { it.gameId == selectedGameId } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---- SECTION 1: FREE FIRE / EXTERNAL ESPORTS TOURNAMENTS ----
        item(key = "admin_esports_header") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF7C2D12))))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFBBF24))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Free Fire & Esports Tournaments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Text(
                        "Create Free Fire / PUBG / Custom Room matches. Launching a NEW tournament automatically deletes previous COMPLETED history for that game!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Button(
                        onClick = { showCreateEsportsDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_create_freefire_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Launch New Free Fire / Esports Match", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        items(externalTournaments, key = { "ext_${it.id}" }) { t ->
            val mapBanner = remember(t.bannerKey, t.mapName) {
                FreeFireMapBanner.fromKeyOrMap(t.bannerKey, t.mapName)
            }
            val ranks = remember(t.prizeRanksJson) { TournamentPrizeCodec.parseRanks(t.prizeRanksJson) }
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = mapBanner.bannerRes),
                    contentDescription = "${mapBanner.displayName} Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${t.gameCategory} • ${t.title}", fontWeight = FontWeight.ExtraBold)
                        Text(
                            "${t.modeLabel} | ${mapBanner.displayName} | ${t.startTimeLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (t.status) {
                            "LIVE" -> Color(0xFFEF4444)
                            "COMPLETED" -> Color(0xFF64748B)
                            else -> Color(0xFF059669)
                        }
                    ) {
                        Text(
                            t.status,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Entry: ৳${t.entryFee.toInt()} • Prize Pool: ৳${t.prizePool.toInt()} • Per Kill: ৳${t.perKillReward.toInt()} • Comm: ${t.adminCommissionPercent.toInt()}% • Slots: ${t.joinedCount}/${t.maxSlots}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (ranks.isNotEmpty()) {
                    Text(
                        ranks.joinToString(" • ") { "${it.rankLabel}: ৳${it.amount.toInt()}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD97706),
                        fontWeight = FontWeight.Bold
                    )
                }
                if (t.roomIdCode.isNotBlank()) {
                    Text(
                        "Room ID: ${t.roomIdCode} | Pass: ${t.roomPassword}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (t.status == "COMPLETED" && t.winnerName.isNotBlank()) {
                    Text("🏆 Winner: ${t.winnerName}", color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (t.status != "COMPLETED") {
                        OutlinedButton(onClick = { roomEditTarget = t }) {
                            Text("Set Room ID/Pass")
                        }
                        OutlinedButton(
                            onClick = { scheduleEditTarget = t },
                            modifier = Modifier.testTag("admin_reschedule_ff_${t.id}")
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Set Time")
                        }
                        Button(
                            onClick = { onSendRoomAlert(t.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706), contentColor = Color.White),
                            modifier = Modifier.testTag("admin_send_room_alert_${t.id}")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Send Room Alert")
                        }
                        if (t.status == "UPCOMING") {
                            Button(
                                onClick = { onUpdateExternalTournament(t.copy(status = "LIVE")) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                            ) { Text("Go LIVE") }
                        }
                        Button(
                            onClick = { completeTarget = t },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Finish & Pay Winner")
                        }
                    }
                    OutlinedButton(onClick = { onDeleteExternalTournament(t.id) }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // ---- SECTION 2: IN-BUILT GAMES ENTRY FEES & WIN PRIZES ----
        item(key = "admin_tiers_section") {
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("In-Built Game Entry Fees & Win Prizes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlayWalletRepository.ALL_INBUILT_GAME_IDS.forEach { gid ->
                        FilterChip(
                            selected = selectedGameId == gid,
                            onClick = { selectedGameId = gid },
                            label = { Text(gid.replace("_", " ")) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                selectedGameTiers.forEach { tier ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${tier.tierLabel}: Entry ৳${tier.entryFee.toInt()} → Win ৳${tier.winPrize.toInt()}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (tier.isEnabled) "Active on Player Menu" else "Disabled",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (tier.isEnabled) Color(0xFF059669) else MaterialTheme.colorScheme.error
                            )
                        }
                        Switch(
                            checked = tier.isEnabled,
                            onCheckedChange = { onSaveTier(tier.copy(isEnabled = it)) }
                        )
                        IconButton(onClick = { onDeleteTier(tier.id) }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Delete Tier", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showAddTierDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Entry Fee Tier for ${selectedGameId.replace("_", " ")}")
                }
            }
        }

        // ---- SECTION 3: PER-GAME BOT ACCURACY & SEAT LIMIT ----
        item(key = "admin_bot_accuracy_header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Per-Game Bot Accuracy & Online Seat Limits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        items(PlayWalletRepository.ALL_INBUILT_GAME_IDS, key = { "bot_cfg_$it" }) { gid ->
            val cfg = botMap[gid] ?: GameBotConfigEntity(gameId = gid)
            var accSlider by remember(cfg.botAccuracyPercent) { mutableFloatStateOf(cfg.botAccuracyPercent.toFloat()) }
            var seatsInput by remember(cfg.maxConcurrentSeats) { mutableStateOf(cfg.maxConcurrentSeats.toString()) }
            var mpEnabled by remember(cfg.multiplayerEnabled) { mutableStateOf(cfg.multiplayerEnabled) }

            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(gid.replace("_", " "), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (mpEnabled) "Online Open" else "Paused", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.width(6.dp))
                        Switch(checked = mpEnabled, onCheckedChange = { mpEnabled = it })
                    }
                }
                Text(
                    "Fallback Bot Accuracy: ${accSlider.toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = accSlider,
                    onValueChange = { accSlider = it },
                    valueRange = 10f..100f,
                    steps = 17
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = seatsInput,
                        onValueChange = { seatsInput = it.filter(Char::isDigit) },
                        label = { Text("Max Seats") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            onSaveBotConfig(
                                cfg.copy(
                                    botAccuracyPercent = accSlider.toInt(),
                                    maxConcurrentSeats = (seatsInput.toIntOrNull() ?: 10).coerceIn(1, 500),
                                    multiplayerEnabled = mpEnabled
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save Game Config") }
                }
            }
        }
    }

    if (showAddTierDialog) {
        var label by remember { mutableStateOf("Pro Match") }
        var entry by remember { mutableStateOf("150") }
        var prize by remember { mutableStateOf("270") }
        AlertDialog(
            onDismissRequest = { showAddTierDialog = false },
            title = { Text("Add Tier • ${selectedGameId.replace("_", " ")}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Tier Name (e.g. Master)") }, singleLine = true)
                    OutlinedTextField(value = entry, onValueChange = { entry = it.filter(Char::isDigit) }, label = { Text("Entry Fee (৳)") }, singleLine = true)
                    OutlinedTextField(value = prize, onValueChange = { prize = it.filter(Char::isDigit) }, label = { Text("Win Prize (৳)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val feeVal = entry.toDoubleOrNull() ?: 20.0
                    val prizeVal = prize.toDoubleOrNull() ?: (feeVal * 1.8)
                    onSaveTier(
                        TournamentTierConfigEntity(
                            gameId = selectedGameId,
                            tierLabel = label.trim().ifBlank { "৳${feeVal.toInt()} Tier" },
                            entryFee = feeVal,
                            winPrize = prizeVal,
                            isEnabled = true,
                            sortOrder = selectedGameTiers.size + 1
                        )
                    )
                    showAddTierDialog = false
                }) { Text("Save Tier") }
            },
            dismissButton = { TextButton(onClick = { showAddTierDialog = false }) { Text("Cancel") } }
        )
    }

    if (showCreateEsportsDialog) {
        var gameCat by remember { mutableStateOf("FREE_FIRE") }
        var selectedBanner by remember { mutableStateOf(FreeFireMapBanner.BERMUDA) }
        var title by remember { mutableStateOf("Free Fire Bermuda Squad Cup") }
        var mode by remember { mutableStateOf(FreeFireMapBanner.BERMUDA.defaultMode) }
        var entryFee by remember { mutableStateOf("30") }
        var prizePool by remember { mutableStateOf("1000") }
        var perKill by remember { mutableStateOf("15") }
        var slots by remember { mutableStateOf("48") }
        var selectedStartEpochMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 60 * 60_000L) }
        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }
        var nowTicker by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var alertLeadMinutes by remember { mutableStateOf("10") }
        var commissionPct by remember { mutableStateOf("20") }
        var versionLabel by remember { mutableStateOf("Mobile Only • No Emulator") }
        var rulesNotice by remember { mutableStateOf("No Hack / Panel / Headshot Config / Teaming. Violators permanently suspended.") }
        var startTimeLabel by remember { mutableStateOf(TournamentTimeFormatter.formatScheduleLabel(selectedStartEpochMillis)) }
        var roomId by remember { mutableStateOf("") }
        var roomPass by remember { mutableStateOf("") }
        var rankPrizes by remember {
            mutableStateOf(
                listOf(
                    RankPrizeTier("1st Prize", 500.0),
                    RankPrizeTier("2nd Prize", 300.0),
                    RankPrizeTier("3rd Prize", 200.0)
                )
            )
        }
        var newRankLabel by remember { mutableStateOf("") }
        var newRankAmount by remember { mutableStateOf("") }

        LaunchedEffect(showCreateEsportsDialog) {
            while (true) {
                delay(1000L)
                nowTicker = System.currentTimeMillis()
            }
        }

        val remainingPreviewMs = selectedStartEpochMillis - nowTicker
        val isFutureTime = remainingPreviewMs > 0L

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedStartEpochMillis)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    Button(onClick = {
                        datePickerState.selectedDateMillis?.let { utcDate ->
                            val cur = Calendar.getInstance().apply { timeInMillis = selectedStartEpochMillis }
                            val updated = TournamentTimeFormatter.combineUtcDateWithLocalTime(
                                utcDateMillis = utcDate,
                                hourOfDay = cur.get(Calendar.HOUR_OF_DAY),
                                minute = cur.get(Calendar.MINUTE)
                            )
                            selectedStartEpochMillis = updated
                            startTimeLabel = TournamentTimeFormatter.formatScheduleLabel(updated)
                        }
                        showDatePicker = false
                    }) { Text("Set Date") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showTimePicker) {
            val cur = Calendar.getInstance().apply { timeInMillis = selectedStartEpochMillis }
            val timePickerState = rememberTimePickerState(
                initialHour = cur.get(Calendar.HOUR_OF_DAY),
                initialMinute = cur.get(Calendar.MINUTE),
                is24Hour = false
            )
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Select Exact Match Time", fontWeight = FontWeight.Bold) },
                text = { TimePicker(state = timePickerState) },
                confirmButton = {
                    Button(onClick = {
                        val updatedCal = Calendar.getInstance().apply {
                            timeInMillis = selectedStartEpochMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        selectedStartEpochMillis = updatedCal.timeInMillis
                        startTimeLabel = TournamentTimeFormatter.formatScheduleLabel(updatedCal.timeInMillis)
                        showTimePicker = false
                    }) { Text("Set Time") }
                },
                dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
            )
        }

        AlertDialog(
            onDismissRequest = { showCreateEsportsDialog = false },
            title = { Text("Centralized Live Tournament Hub Creator", fontWeight = FontWeight.ExtraBold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(450.dp)) {
                    // ---- GAME CATEGORY SELECTOR (FREE FIRE + ALL 5 IN-BUILT GAMES) ----
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Select Game Category (Syncs Live to Firestore Hub)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    "FREE_FIRE" to "Free Fire",
                                    "LUDO" to "Ludo (2–5P)",
                                    "CHESS" to "Chess",
                                    "AETHER_CROWN" to "Aether Crown",
                                    "REVERSI" to "Reversi",
                                    "CONNECT_FOUR" to "Connect 4",
                                    "CARROM" to "Carrom 3D",
                                    "SNAKES_LADDERS" to "Snakes & Ladders",
                                    "FRUIT_SLASH" to "Fruit Slash 3D",
                                    "PUBG" to "PUBG"
                                ).forEach { (catCode, label) ->
                                    FilterChip(
                                        selected = gameCat == catCode,
                                        onClick = {
                                            gameCat = catCode
                                            when (catCode) {
                                                "FREE_FIRE" -> {
                                                    mode = selectedBanner.defaultMode
                                                    title = "Free Fire ${selectedBanner.displayName} Cup"
                                                    slots = "48"
                                                }
                                                "LUDO" -> {
                                                    mode = "5P STAR LUDO"
                                                    title = "Royal 5-Player Ludo Grand Cup"
                                                    slots = "20"
                                                    perKill = "0"
                                                }
                                                "CHESS" -> {
                                                    mode = "BLITZ 1v1"
                                                    title = "Grandmaster Chess Championship"
                                                    slots = "16"
                                                    perKill = "0"
                                                }
                                                "AETHER_CROWN" -> {
                                                    mode = "FLUX 1v1"
                                                    title = "Aether Crown Orbital Cup"
                                                    slots = "16"
                                                    perKill = "0"
                                                }
                                                "REVERSI" -> {
                                                    mode = "OTHELLO 1v1"
                                                    title = "Imperial Reversi Masters"
                                                    slots = "16"
                                                    perKill = "0"
                                                }
                                                "CONNECT_FOUR" -> {
                                                    mode = "KNOCKOUT"
                                                    title = "Crown Connect 4 Clash"
                                                    slots = "16"
                                                    perKill = "0"
                                                }
                                                "CARROM" -> {
                                                    mode = "CARROM 1v1"
                                                    title = "FinPlay Pro Carrom 3D Grand Masters"
                                                    slots = "16"
                                                    perKill = "0"
                                                }
                                                "SNAKES_LADDERS" -> {
                                                    mode = "CLASSIC 100"
                                                    title = "FinPlay Pro Snakes & Ladders Masters"
                                                    slots = "16"
                                                    perKill = "0"
                                                }
                                                "FRUIT_SLASH" -> {
                                                    mode = "SLASH BLITZ"
                                                    title = "Fruit Slash 3D Blade Tournament"
                                                    slots = "24"
                                                    perKill = "0"
                                                }
                                                else -> {
                                                    mode = "SQUAD"
                                                    title = "$label Pro Tournament"
                                                    slots = "48"
                                                }
                                            }
                                        },
                                        label = { Text(label, fontSize = 11.sp) },
                                        modifier = Modifier.testTag("admin_hub_cat_${catCode.lowercase()}")
                                    )
                                }
                            }
                        }
                    }

                    // ---- MAP BANNER SELECTOR + LIVE PREVIEW ----
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Select Map & Real Banner", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Image(
                                painter = painterResource(id = selectedBanner.bannerRes),
                                contentDescription = selectedBanner.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FreeFireMapBanner.entries.forEach { banner ->
                                    FilterChip(
                                        selected = selectedBanner == banner,
                                        onClick = {
                                            selectedBanner = banner
                                            if (gameCat == "FREE_FIRE") {
                                                mode = banner.defaultMode
                                                if (title.startsWith("Free Fire ")) {
                                                    title = "Free Fire ${banner.displayName} Cup"
                                                }
                                            }
                                        },
                                        label = { Text(banner.displayName, fontSize = 11.sp) },
                                        modifier = Modifier.testTag("ff_map_chip_${banner.key.lowercase()}")
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tournament Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = gameCat, onValueChange = { gameCat = it }, label = { Text("Game") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = mode, onValueChange = { mode = it }, label = { Text("Mode") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = entryFee, onValueChange = { entryFee = it.filter(Char::isDigit) }, label = { Text("Entry ৳") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = prizePool, onValueChange = { prizePool = it.filter(Char::isDigit) }, label = { Text("Total Prize ৳") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = perKill, onValueChange = { perKill = it.filter(Char::isDigit) }, label = { Text("Per Kill ৳") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = slots, onValueChange = { slots = it.filter(Char::isDigit) }, label = { Text("Max Slots") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = commissionPct, onValueChange = { commissionPct = it.filter(Char::isDigit) }, label = { Text("Comm %") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                    }

                    // ---- DYNAMIC UNLIMITED RANK PRIZES BUILDER ----
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Dynamic Rank Prizes (1st, 2nd, 3rd, MVP...)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                                rankPrizes.forEachIndexed { idx, tier ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("🏆 ${tier.rankLabel}: ৳${tier.amount.toInt()}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                        IconButton(
                                            onClick = { rankPrizes = rankPrizes.filterIndexed { i, _ -> i != idx } },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteForever, contentDescription = "Remove rank", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = newRankLabel,
                                        onValueChange = { newRankLabel = it },
                                        label = { Text("Rank (e.g. 4th / MVP)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    OutlinedTextField(
                                        value = newRankAmount,
                                        onValueChange = { newRankAmount = it.filter(Char::isDigit) },
                                        label = { Text("৳") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.7f)
                                    )
                                    Button(
                                        onClick = {
                                            val amt = newRankAmount.toDoubleOrNull() ?: 0.0
                                            if (newRankLabel.isNotBlank() && amt > 0.0) {
                                                val updated = rankPrizes + RankPrizeTier(newRankLabel.trim(), amt)
                                                rankPrizes = updated
                                                prizePool = updated.sumOf { it.amount }.toInt().toString()
                                                newRankLabel = ""
                                                newRankAmount = ""
                                            }
                                        },
                                        modifier = Modifier.testTag("admin_add_rank_prize_btn")
                                    ) { Text("+Add") }
                                }
                            }
                        }
                    }

                    // ---- CUSTOM ANY-TIME DATE & TIME SCHEDULER + LIVE AUTO COUNTDOWN ----
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            border = BorderStroke(
                                1.dp,
                                if (isFutureTime) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Match Start Date & Time (Auto User Countdown)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showDatePicker = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("admin_ff_pick_date_btn")
                                    ) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Pick Date", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { showTimePicker = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("admin_ff_pick_time_btn")
                                    ) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Pick Time", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Quick Preset Chips (Anytime shortcuts)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(
                                        "+15m" to { System.currentTimeMillis() + 15 * 60_000L },
                                        "+30m" to { System.currentTimeMillis() + 30 * 60_000L },
                                        "+1h" to { System.currentTimeMillis() + 60 * 60_000L },
                                        "+3h" to { System.currentTimeMillis() + 180 * 60_000L },
                                        "9:00 PM" to { TournamentTimeFormatter.nextOccurrenceOfTime(21, 0) },
                                        "Tomorrow 8 PM" to { TournamentTimeFormatter.nextOccurrenceOfTime(20, 0, addDays = 1) }
                                    ).forEach { (lbl, computeTime) ->
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                val next = computeTime()
                                                selectedStartEpochMillis = next
                                                startTimeLabel = TournamentTimeFormatter.formatScheduleLabel(next)
                                            },
                                            label = { Text(lbl, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                // Live Auto-Calculated Countdown Preview Box
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isFutureTime) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = "📅 Scheduled: ${TournamentTimeFormatter.formatScheduleLabel(selectedStartEpochMillis)}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isFutureTime) {
                                            Text(
                                                text = "⏳ Live User Countdown: ${TournamentTimeFormatter.formatCountdown(remainingPreviewMs, isBn = false)}",
                                                color = Color(0xFFFDE68A),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "⚠️ Time is in the past — pick a future time!",
                                                    color = Color(0xFFFECACA),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                TextButton(onClick = {
                                                    val bumped = selectedStartEpochMillis + 24L * 60L * 60L * 1000L
                                                    selectedStartEpochMillis = bumped
                                                    startTimeLabel = TournamentTimeFormatter.formatScheduleLabel(bumped)
                                                }) {
                                                    Text("+1 Day", color = Color(0xFFFDE68A), fontWeight = FontWeight.ExtraBold)
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = startTimeLabel,
                                        onValueChange = { startTimeLabel = it },
                                        label = { Text("Display Schedule Label") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.4f)
                                    )
                                    OutlinedTextField(
                                        value = alertLeadMinutes,
                                        onValueChange = { alertLeadMinutes = it.filter(Char::isDigit) },
                                        label = { Text("Alert (min)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(value = versionLabel, onValueChange = { versionLabel = it }, label = { Text("Device / Version Rule") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = rulesNotice, onValueChange = { rulesNotice = it }, label = { Text("Anti-Cheat & Tournament Rules Notice") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = roomId, onValueChange = { roomId = it }, label = { Text("Room ID (Opt)") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = roomPass, onValueChange = { roomPass = it }, label = { Text("Room Pass") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = title.isNotBlank() && isFutureTime,
                    onClick = {
                        if (title.isNotBlank() && isFutureTime) {
                            val sumRanks = rankPrizes.sumOf { it.amount }
                            val finalPrizePool = prizePool.toDoubleOrNull()?.takeIf { it > 0.0 } ?: sumRanks.coerceAtLeast(100.0)
                            onCreateExternalTournament(
                                ExternalTournamentEntity(
                                    gameCategory = gameCat.trim().uppercase().ifBlank { "FREE_FIRE" },
                                    title = title.trim(),
                                    modeLabel = mode.trim().ifBlank { selectedBanner.defaultMode },
                                    mapName = selectedBanner.displayName,
                                    bannerKey = selectedBanner.key,
                                    entryFee = entryFee.toDoubleOrNull() ?: 20.0,
                                    prizePool = finalPrizePool,
                                    perKillReward = perKill.toDoubleOrNull() ?: 10.0,
                                    prizeRanksJson = TournamentPrizeCodec.serializeRanks(rankPrizes),
                                    maxSlots = (slots.toIntOrNull() ?: 48).coerceAtLeast(2),
                                    startTimeLabel = startTimeLabel.trim().ifBlank {
                                        TournamentTimeFormatter.formatScheduleLabel(selectedStartEpochMillis)
                                    },
                                    matchStartTimeMillis = selectedStartEpochMillis,
                                    preMatchAlertMinutes = (alertLeadMinutes.toIntOrNull() ?: 10).coerceIn(1, 60),
                                    versionLabel = versionLabel.trim(),
                                    rulesNotice = rulesNotice.trim(),
                                    adminCommissionPercent = (commissionPct.toDoubleOrNull() ?: 20.0).coerceIn(0.0, 90.0),
                                    roomIdCode = roomId.trim(),
                                    roomPassword = roomPass.trim(),
                                    status = "UPCOMING"
                                )
                            )
                            showCreateEsportsDialog = false
                        }
                    },
                    modifier = Modifier.testTag("admin_launch_ff_confirm_btn")
                ) { Text("Launch Tournament") }
            },
            dismissButton = { TextButton(onClick = { showCreateEsportsDialog = false }) { Text("Cancel") } }
        )
    }

    scheduleEditTarget?.let { target ->
        var editedEpochMillis by remember(target.id) {
            mutableLongStateOf(
                target.matchStartTimeMillis.takeIf { it > System.currentTimeMillis() }
                    ?: (System.currentTimeMillis() + 60 * 60_000L)
            )
        }
        var showEditDate by remember(target.id) { mutableStateOf(false) }
        var showEditTime by remember(target.id) { mutableStateOf(false) }
        var editNowTicker by remember(target.id) { mutableLongStateOf(System.currentTimeMillis()) }

        LaunchedEffect(target.id) {
            while (true) {
                delay(1000L)
                editNowTicker = System.currentTimeMillis()
            }
        }
        val editRemainingMs = editedEpochMillis - editNowTicker

        if (showEditDate) {
            val dpState = rememberDatePickerState(initialSelectedDateMillis = editedEpochMillis)
            DatePickerDialog(
                onDismissRequest = { showEditDate = false },
                confirmButton = {
                    Button(onClick = {
                        dpState.selectedDateMillis?.let { utcDate ->
                            val cur = Calendar.getInstance().apply { timeInMillis = editedEpochMillis }
                            editedEpochMillis = TournamentTimeFormatter.combineUtcDateWithLocalTime(
                                utcDateMillis = utcDate,
                                hourOfDay = cur.get(Calendar.HOUR_OF_DAY),
                                minute = cur.get(Calendar.MINUTE)
                            )
                        }
                        showEditDate = false
                    }) { Text("Set Date") }
                },
                dismissButton = { TextButton(onClick = { showEditDate = false }) { Text("Cancel") } }
            ) { DatePicker(state = dpState) }
        }

        if (showEditTime) {
            val cur = Calendar.getInstance().apply { timeInMillis = editedEpochMillis }
            val tpState = rememberTimePickerState(
                initialHour = cur.get(Calendar.HOUR_OF_DAY),
                initialMinute = cur.get(Calendar.MINUTE),
                is24Hour = false
            )
            AlertDialog(
                onDismissRequest = { showEditTime = false },
                title = { Text("Pick Match Time") },
                text = { TimePicker(state = tpState) },
                confirmButton = {
                    Button(onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = editedEpochMillis
                            set(Calendar.HOUR_OF_DAY, tpState.hour)
                            set(Calendar.MINUTE, tpState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        editedEpochMillis = cal.timeInMillis
                        showEditTime = false
                    }) { Text("Set Time") }
                },
                dismissButton = { TextButton(onClick = { showEditTime = false }) { Text("Cancel") } }
            )
        }

        AlertDialog(
            onDismissRequest = { scheduleEditTarget = null },
            title = { Text("Reschedule: ${target.title}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showEditDate = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Date")
                        }
                        OutlinedButton(onClick = { showEditTime = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Time")
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (editRemainingMs > 0L) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "📅 ${TournamentTimeFormatter.formatScheduleLabel(editedEpochMillis)}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                if (editRemainingMs > 0L) "⏳ Countdown: ${TournamentTimeFormatter.formatCountdown(editRemainingMs, false)}"
                                else "⚠️ Choose a future date/time",
                                color = Color(0xFFFDE68A),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = editRemainingMs > 0L,
                    onClick = {
                        onUpdateExternalTournament(
                            target.copy(
                                matchStartTimeMillis = editedEpochMillis,
                                startTimeLabel = TournamentTimeFormatter.formatScheduleLabel(editedEpochMillis),
                                roomAlertSent = false
                            )
                        )
                        scheduleEditTarget = null
                    }
                ) { Text("Update Countdown") }
            },
            dismissButton = { TextButton(onClick = { scheduleEditTarget = null }) { Text("Cancel") } }
        )
    }

    roomEditTarget?.let { target ->
        var rId by remember(target.id) { mutableStateOf(target.roomIdCode) }
        var rPass by remember(target.id) { mutableStateOf(target.roomPassword) }
        AlertDialog(
            onDismissRequest = { roomEditTarget = null },
            title = { Text("Custom Room Credentials") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = rId, onValueChange = { rId = it }, label = { Text("Room ID") }, singleLine = true)
                    OutlinedTextField(value = rPass, onValueChange = { rPass = it }, label = { Text("Room Password") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateExternalTournament(target.copy(roomIdCode = rId.trim(), roomPassword = rPass.trim()))
                    roomEditTarget = null
                }) { Text("Save Room Info") }
            },
            dismissButton = { TextButton(onClick = { roomEditTarget = null }) { Text("Cancel") } }
        )
    }

    completeTarget?.let { target ->
        var winnerUser by remember(target.id) { mutableStateOf("") }
        var prizeAmt by remember(target.id) { mutableStateOf(target.prizePool.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { completeTarget = null },
            title = { Text("Finish Tournament & Credit Winner") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter winner's FinPlay username to auto-credit Winning Coins (withdrawable). This tournament will move to COMPLETED until a new ${target.gameCategory} match is launched.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(value = winnerUser, onValueChange = { winnerUser = it }, label = { Text("Winner Username") }, singleLine = true)
                    OutlinedTextField(value = prizeAmt, onValueChange = { prizeAmt = it.filter(Char::isDigit) }, label = { Text("Prize to Winning Balance (৳)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCompleteExternalTournament(target.id, winnerUser.trim(), prizeAmt.toDoubleOrNull() ?: 0.0)
                    completeTarget = null
                }) { Text("Mark Completed") }
            },
            dismissButton = { TextButton(onClick = { completeTarget = null }) { Text("Cancel") } }
        )
    }
}

/**
 * Admin Broadcast Notification Center — pushes alerts to ALL users at once.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminBroadcastNotificationsTab(
    broadcasts: List<BroadcastNotificationEntity>,
    onSendBroadcast: (String, String, String) -> Unit,
    onDeleteBroadcast: (Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("TOURNAMENT") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "compose_broadcast") {
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Send Push Notification to ALL Users", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Delivers instantly via Android Heads-Up Push + All Users Inbox + Real-Time Firestore Cloud Sync.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF059669),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("INFO", "TOURNAMENT", "PROMO", "URGENT").forEach { tag ->
                        FilterChip(selected = priority == tag, onClick = { priority = tag }, label = { Text(tag) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notification Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_broadcast_title_input")
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Announcement Message") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_broadcast_msg_input")
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank() && message.isNotBlank()) {
                            onSendBroadcast(title.trim(), message.trim(), priority)
                            title = ""
                            message = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_send_broadcast_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Broadcast to All Users", fontWeight = FontWeight.Bold)
                }
            }
        }

        items(broadcasts, key = { it.id }) { b ->
            LedgerCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("[${b.priority}] ${b.title}", fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { onDeleteBroadcast(b.id) }) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
                Text(b.message, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    DateTimeUtil.formatDate(b.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AdminSecretLoginConfigCard(
    onUpdateSecretCredentials: (String, String) -> Unit,
    onUpdateAdminPortalKey: (String) -> Unit = {}
) {
    var secUser by remember { mutableStateOf("") }
    var secPass by remember { mutableStateOf("") }
    var newPortalKey by remember { mutableStateOf("") }
    LedgerCard(modifier = Modifier.fillMaxWidth()) {
        Text("Same-APK Secret Admin Login Credentials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Sign in with these credentials on the regular Login Screen to open this Admin Panel directly.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = secUser,
            onValueChange = { secUser = it },
            label = { Text("New Secret Admin Username (min 4 chars)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = secPass,
            onValueChange = { secPass = it },
            label = { Text("New Secret Admin Password (min 8 chars)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                if (secUser.trim().length >= 4 && secPass.trim().length >= 8) {
                    onUpdateSecretCredentials(secUser.trim(), secPass.trim())
                    secUser = ""
                    secPass = ""
                }
            },
            enabled = secUser.trim().length >= 4 && secPass.trim().length >= 8,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Rotate Secret Admin Login")
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        Text("Long-Press Logo Admin Portal Key", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            "Rotate the encrypted Admin Portal Key used in the long-press logo Admin dialog.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = newPortalKey,
            onValueChange = { newPortalKey = it },
            label = { Text("New Admin Portal Key (min 6 chars)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                if (newPortalKey.trim().length >= 6) {
                    onUpdateAdminPortalKey(newPortalKey.trim())
                    newPortalKey = ""
                }
            },
            enabled = newPortalKey.trim().length >= 6,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Rotate Admin Portal Key")
        }
    }
}
