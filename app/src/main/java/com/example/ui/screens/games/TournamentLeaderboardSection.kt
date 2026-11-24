package com.example.ui.screens.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.games.TournamentEarningsEstimate
import com.example.data.games.TournamentLeaderboardEntry
import com.example.domain.games.PlayArmoryCatalog
import com.example.ui.viewmodel.OnlineAuthProfile
import kotlin.math.roundToInt

/**
 * Real-time Top 10 Tournament Leaderboard powered by Firestore `orderBy("matchPoints", DESC).limit(10)`.
 * Features an esports Gold/Silver/Bronze Top-3 Podium and ranked #4-#10 rows with live sync status.
 */
@Composable
fun TournamentLeaderboardSection(
    entries: List<TournamentLeaderboardEntry>,
    onlineAuth: OnlineAuthProfile?,
    isBn: Boolean,
    onSignInGoogle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.3.dp, Color(0xFFF59E0B).copy(alpha = 0.72f)),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("tournament_leaderboard_section")
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF090F1E), Color(0xFF171F3A), Color(0xFF0D1B2A))
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = "Tournament Leaderboard",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isBn) "শীর্ষ ১০ টুর্নামেন্ট লিডারবোর্ড" else "Top 10 Tournament Leaderboard",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isBn) "Firestore রিয়েল-টাইম ম্যাচ পয়েন্ট র‍্যাঙ্কিং"
                            else "Real-time Firestore ranking by match points",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (onlineAuth != null) Color(0xFF065F46) else Color(0xFF334155)
                ) {
                    Text(
                        text = if (onlineAuth != null) "● LIVE TOP 10" else "SIGN IN",
                        color = if (onlineAuth != null) Color(0xFF6EE7B7) else Color(0xFFFDE68A),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            if (onlineAuth == null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = if (isBn) "লাইভ শীর্ষ ১০ লিডারবোর্ড দেখতে এবং আপনার পয়েন্ট যোগ করতে Google সাইন-ইন করুন।"
                            else "Sign in with Google to view live Top 10 rankings and sync your tournament match points.",
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onSignInGoogle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("leaderboard_signin_btn")
                        ) {
                            Text(
                                text = if (isBn) "Google দিয়ে সিঙ্ক করুন" else "Sign in for Live Rankings",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            } else if (entries.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = if (isBn) "এখনো কেউ র‍্যাঙ্কড ম্যাচ খেলেনি — প্রথম চ্যাম্পিয়ন হোন!"
                            else "No ranked scores recorded yet — finish a match to claim #1!",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Top 3 Podium Row
                val podium = entries.take(3)
                if (podium.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        podium.forEachIndexed { idx, entry ->
                            PodiumPlayerTile(
                                rank = idx + 1,
                                entry = entry,
                                isMe = entry.userId == onlineAuth.uid,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Ranked List #1 - #10 (shows #4-#10 if podium shown, or all compact rows if desired)
                val remaining = if (entries.size > 3) entries.drop(3).take(7) else emptyList()
                if (remaining.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        remaining.forEachIndexed { offset, entry ->
                            val rank = offset + 4
                            LeaderboardRankedRow(
                                rank = rank,
                                entry = entry,
                                isMe = entry.userId == onlineAuth.uid,
                                isBn = isBn
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumPlayerTile(
    rank: Int,
    entry: TournamentLeaderboardEntry,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = when (rank) {
        1 -> Color(0xFFFBBF24)
        2 -> Color(0xFFCBD5E1)
        else -> Color(0xFFFB923C)
    }
    val medal = when (rank) {
        1 -> "👑 #1"
        2 -> "🥈 #2"
        else -> "🥉 #3"
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = if (rank == 1) 0.18f else 0.11f),
        border = BorderStroke(if (isMe || rank == 1) 1.5.dp else 1.dp, accent.copy(alpha = 0.75f)),
        modifier = modifier.testTag("leaderboard_podium_rank_$rank")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(medal, color = accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                text = PlayArmoryCatalog.avatarGlyph(entry.avatarId),
                fontSize = 22.sp
            )
            Text(
                text = entry.displayName,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${entry.matchPoints} PTS",
                color = accent,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "${entry.wins}W • ${entry.winRatePercent}%",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun LeaderboardRankedRow(
    rank: Int,
    entry: TournamentLeaderboardEntry,
    isMe: Boolean,
    isBn: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isMe) Color(0xFF065F46).copy(alpha = 0.42f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(
            1.dp,
            if (isMe) Color(0xFF34D399) else Color.White.copy(alpha = 0.12f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_rank_$rank")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isMe) Color(0xFF10B981) else Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = PlayArmoryCatalog.avatarGlyph(entry.avatarId),
                fontSize = 18.sp
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = entry.displayName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isMe) {
                        Surface(shape = RoundedCornerShape(50), color = Color(0xFFF59E0B)) {
                            Text(
                                text = if (isBn) "আপনি" else "YOU",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${entry.favoriteGame.replace("_", " ")} • ${entry.wins}W/${entry.matchesPlayed}M • ৳${entry.totalCoinsEarned.toInt()}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.matchPoints}",
                    color = Color(0xFF38BDF8),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "PTS",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Compact Entry Fee vs. Potential Earnings & ROI analyzer banner powered by
 * `TournamentEarningsCalculator.calculateEntryVsPotentialEarnings(...)`.
 */
@Composable
fun TournamentEarningsBreakdownBanner(
    estimate: TournamentEarningsEstimate,
    isBn: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = if (estimate.isPositiveReturn) Color(0xFF34D399) else Color(0xFFFBBF24)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF064E3B).copy(alpha = 0.34f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("tournament_earnings_breakdown")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Potential Earnings",
                        tint = accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isBn) "এন্ট্রি বনাম সম্ভাব্য আয়" else "Entry vs. Potential Earnings",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accent.copy(alpha = 0.22f)
                ) {
                    val roiTag = if (estimate.isFreeEntry) {
                        if (isBn) "ফ্রি এন্ট্রি" else "FREE ROI"
                    } else {
                        String.format("%.1fx • +%d%% ROI", estimate.returnMultiplier, estimate.roiPercent.roundToInt())
                    }
                    Text(
                        text = roiTag,
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isBn) "১ম পুরস্কার: ৳${estimate.firstPlacePrize.roundToInt()}"
                    else "1st Prize: ৳${estimate.firstPlacePrize.roundToInt()}",
                    color = Color(0xFFFDE68A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isBn) "সম্ভাব্য নিট লাভ: +৳${estimate.potentialNetProfit.roundToInt().coerceAtLeast(0)}"
                    else "Net Profit: +৳${estimate.potentialNetProfit.roundToInt().coerceAtLeast(0)}",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (estimate.breakEvenKills > 0) {
                Text(
                    text = if (isBn) "🎯 মাত্র ${estimate.breakEvenKills} কিল করলেই এন্ট্রি ফি (৳${estimate.entryFee.roundToInt()}) উঠে আসবে!"
                    else "🎯 Just ${estimate.breakEvenKills} kills cover your ৳${estimate.entryFee.roundToInt()} entry fee!",
                    color = Color(0xFFA7F3D0),
                    fontSize = 10.sp
                )
            }
        }
    }
}
