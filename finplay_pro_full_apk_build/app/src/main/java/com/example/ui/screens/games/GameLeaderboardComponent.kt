package com.example.ui.screens.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.GameLeaderboardEntity
import com.example.data.local.winRatePercent
import com.example.domain.games.GameSoundEffect
import com.example.domain.games.GameSoundEngine
import com.example.i18n.LanguageManager
import com.example.i18n.LocalStrings
import com.example.ui.theme.LocalFinanceColors
import kotlin.math.roundToInt

@Composable
fun GameLeaderboardOverviewCard(
    entries: List<GameLeaderboardEntity>,
    onOpenFullSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gStrings by LanguageManager.gameStrings.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current
    var expanded by remember { mutableStateOf(false) }

    val totalMatches = remember(entries) { entries.sumOf { it.gamesPlayed } }
    val totalWins = remember(entries) { entries.sumOf { it.wins } }
    val overallWinRate = remember(totalMatches, totalWins) {
        if (totalMatches <= 0) 0f else (totalWins * 100f) / totalMatches.toFloat()
    }
    val bestStreak = remember(entries) { entries.maxOfOrNull { it.bestWinStreak } ?: 0 }
    val topHighScore = remember(entries) { entries.maxOfOrNull { it.highScore } ?: 0 }

    val rankedGames = remember(entries) {
        val map = entries.associateBy { it.gameId }
        ArenaGameId.entries.map { game ->
            game to (map[game.name] ?: GameLeaderboardEntity(gameId = game.name))
        }.sortedWith(
            compareByDescending<Pair<ArenaGameId, GameLeaderboardEntity>> { it.second.highScore }
                .thenByDescending { it.second.winRatePercent() }
                .thenByDescending { it.second.wins }
        )
    }

    val animatedOverallProgress by animateFloatAsState(
        targetValue = (overallWinRate / 100f).coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "leaderboard_overall_winrate"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.2.dp, financeColors.champagneGold.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("games_leaderboard_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        GameSoundEngine.play(GameSoundEffect.TAP)
                        expanded = !expanded
                    }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFD97706), Color(0xFFF59E0B))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = gStrings.leaderboardTitle,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = gStrings.leaderboardTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = gStrings.leaderboardSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = financeColors.champagneGold
                )
            }

            // Summary KPI Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LeaderboardStatPill(
                    label = gStrings.totalMatchesLabel,
                    value = "$totalMatches",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                LeaderboardStatPill(
                    label = gStrings.winRateLabel,
                    value = "${overallWinRate.roundToInt()}%",
                    accent = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                LeaderboardStatPill(
                    label = gStrings.highScoreLabel,
                    value = "$topHighScore",
                    accent = financeColors.champagneGold,
                    modifier = Modifier.weight(1f)
                )
                LeaderboardStatPill(
                    label = gStrings.bestStreakLabel,
                    value = "🔥$bestStreak",
                    accent = Color(0xFFF97316),
                    modifier = Modifier.weight(1f)
                )
            }

            LinearProgressIndicator(
                progress = { animatedOverallProgress },
                color = financeColors.champagneGold,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (totalMatches == 0) {
                        Text(
                            text = gStrings.noMatchesYet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        rankedGames.forEachIndexed { idx, (game, stat) ->
                            LeaderboardGameRow(
                                rank = idx + 1,
                                game = game,
                                entry = stat,
                                gStrings = gStrings
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            GameSoundEngine.play(GameSoundEffect.TAP)
                            onOpenFullSheet()
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("games_open_full_leaderboard_btn")
                    ) {
                        Icon(Icons.Default.MilitaryTech, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(gStrings.leaderboardTitle)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLeaderboardModalSheet(
    entries: List<GameLeaderboardEntity>,
    onResetLeaderboard: () -> Unit,
    onDismiss: () -> Unit
) {
    val gStrings by LanguageManager.gameStrings.collectAsStateWithLifecycle()
    val appStrings = LocalStrings.current
    val financeColors = LocalFinanceColors.current
    var showResetConfirm by remember { mutableStateOf(false) }

    val totalMatches = remember(entries) { entries.sumOf { it.gamesPlayed } }
    val totalWins = remember(entries) { entries.sumOf { it.wins } }
    val overallWinRate = if (totalMatches <= 0) 0f else (totalWins * 100f) / totalMatches.toFloat()

    val rankedGames = remember(entries) {
        val map = entries.associateBy { it.gameId }
        ArenaGameId.entries.map { game ->
            game to (map[game.name] ?: GameLeaderboardEntity(gameId = game.name))
        }.sortedWith(
            compareByDescending<Pair<ArenaGameId, GameLeaderboardEntity>> { it.second.highScore }
                .thenByDescending { it.second.winRatePercent() }
                .thenByDescending { it.second.wins }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(gStrings.resetLeaderboard, fontWeight = FontWeight.Bold) },
            text = { Text(gStrings.resetLeaderboardConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        GameSoundEngine.play(GameSoundEffect.TAP)
                        onResetLeaderboard()
                        showResetConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_reset_leaderboard_btn")
                ) {
                    Text(gStrings.resetLeaderboard)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(appStrings.cancel)
                }
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🏆 ${gStrings.leaderboardTitle}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${gStrings.totalMatchesLabel}: $totalMatches • ${gStrings.winRateLabel}: ${overallWinRate.roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = financeColors.champagneGold,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (totalMatches > 0) {
                    OutlinedButton(
                        onClick = {
                            GameSoundEngine.play(GameSoundEffect.TAP)
                            showResetConfirm = true
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("leaderboard_reset_btn")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = gStrings.resetLeaderboard, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(gStrings.resetLeaderboard, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (totalMatches == 0) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = gStrings.noMatchesYet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            rankedGames.forEachIndexed { idx, (game, stat) ->
                LeaderboardGameRow(
                    rank = idx + 1,
                    game = game,
                    entry = stat,
                    gStrings = gStrings
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LeaderboardStatPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LeaderboardGameRow(
    rank: Int,
    game: ArenaGameId,
    entry: GameLeaderboardEntity,
    gStrings: GameUiStrings
) {
    val financeColors = LocalFinanceColors.current
    val winRate = entry.winRatePercent()
    val animatedWinRate by animateFloatAsState(
        targetValue = (winRate / 100f).coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "row_winrate_${game.name}"
    )

    val medal = when {
        entry.gamesPlayed == 0 -> "•"
        rank == 1 -> "🥇"
        rank == 2 -> "🥈"
        rank == 3 -> "🥉"
        else -> "#$rank"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, financeColors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_${game.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = medal,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Column {
                        Text(
                            text = gStrings.gameTitle(game),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "${entry.wins}${gStrings.winsShort} • ${entry.losses}${gStrings.lossesShort} • ${entry.draws}${gStrings.drawsShort} (${entry.gamesPlayed} ${gStrings.totalMatchesLabel})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = financeColors.champagneGold.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${entry.highScore} pts",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = financeColors.champagneGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { animatedWinRate },
                    color = Color(0xFF10B981),
                    trackColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
                Text(
                    text = "${winRate.roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
                if (entry.bestWinStreak > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${entry.bestWinStreak}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF97316)
                        )
                    }
                }
                if (entry.highestBotAccuracyBeaten > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${entry.highestBotAccuracyBeaten}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Spring-animated victory/finish card shown inside any game screen once a match concludes,
 * highlighting the score persisted into the local Room Leaderboard.
 */
@Composable
fun GameMatchResultBanner(
    visible: Boolean,
    headline: String,
    scoreEarned: Int,
    onPlayAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gStrings by LanguageManager.gameStrings.collectAsStateWithLifecycle()
    val financeColors = LocalFinanceColors.current

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = financeColors.champagneGold.copy(alpha = 0.14f),
            border = BorderStroke(1.5.dp, financeColors.champagneGold),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("game_match_result_banner")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF10B981).copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "🏆 ${gStrings.matchScoreRecorded}: +$scoreEarned pts",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Button(
                    onClick = {
                        GameSoundEngine.play(GameSoundEffect.TAP)
                        onPlayAgain()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("game_play_again_btn")
                ) {
                    Text(gStrings.playAnotherMatch)
                }
            }
        }
    }
}
