package com.example.domain

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_BUDGET = "budget_alerts"
    const val CHANNEL_REMINDERS = "finance_reminders"
    const val CHANNEL_BACKUP = "cloud_backup"
    const val CHANNEL_BROADCAST = "finplay_broadcasts"
    const val CHANNEL_TOURNAMENT_ALERT = "finplay_tournament_room_alerts"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_BUDGET,
                    "Budget Warnings",
                    NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Reminders & Overdue Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    CHANNEL_BACKUP,
                    "Google Drive Auto Backup",
                    NotificationManager.IMPORTANCE_LOW
                ),
                NotificationChannel(
                    CHANNEL_BROADCAST,
                    "FinPlay Pro Broadcasts & Announcements",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Official Admin announcements, tournament drops, and promo alerts."
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_TOURNAMENT_ALERT,
                    "Free Fire & Tournament Room Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Urgent Room ID & Password alerts before tournament matches begin."
                    enableVibration(true)
                }
            )
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    fun postBroadcastPushNotification(
        context: Context,
        broadcastId: Long,
        title: String,
        message: String,
        priority: String
    ) {
        val prefix = when (priority.uppercase()) {
            "URGENT" -> "🚨 "
            "TOURNAMENT" -> "🏆 "
            "PROMO" -> "🎁 "
            else -> "📢 "
        }
        postSystemNotification(
            context = context,
            channelId = CHANNEL_BROADCAST,
            notificationId = (40_000 + (broadcastId % 10_000)).toInt(),
            title = "$prefix$title",
            message = message
        )
    }

    fun postTournamentRoomAlert(
        context: Context,
        tournamentId: Long,
        tournamentTitle: String,
        roomId: String,
        roomPassword: String
    ) {
        val msg = if (roomId.isNotBlank()) {
            "Room ID: $roomId | Pass: ${roomPassword.ifBlank { "None" }} — দ্রুত Free Fire লগইন করে কাস্টম রুমে জয়েন করুন! (Log in and join now!)"
        } else {
            "$tournamentTitle ম্যাচ শুরু হতে যাচ্ছে! দ্রুত অ্যাপে Room ID ও Password চেক করে জয়েন করুন!"
        }
        postSystemNotification(
            context = context,
            channelId = CHANNEL_TOURNAMENT_ALERT,
            notificationId = (50_000 + (tournamentId % 10_000)).toInt(),
            title = "🔥 Match Starting Soon: $tournamentTitle",
            message = msg
        )
    }

    fun postSystemNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        actions: List<NotificationCompat.Action> = emptyList()
    ) {
        runCatching { ensureChannels(context) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = runCatching {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
            if (!granted) return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isHigh = channelId == CHANNEL_BUDGET ||
            channelId == CHANNEL_REMINDERS ||
            channelId == CHANNEL_BROADCAST ||
            channelId == CHANNEL_TOURNAMENT_ALERT

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(contentPending)
            .setPriority(if (isHigh) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(if (isHigh) NotificationCompat.DEFAULT_ALL else 0)
            .setAutoCancel(true)

        actions.take(3).forEach { builder.addAction(it) }

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }
    }
}
