package com.example.notifications

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
import com.example.data.model.AppNotification

object ClutchXNotificationManager {

    const val CHANNEL_APPROVALS = "clutchx_approvals"
    const val CHANNEL_REMINDERS = "clutchx_reminders"
    const val CHANNEL_GENERAL = "clutchx_general"
    const val EXTRA_OPEN_ROUTE = "clutchx_open_route"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val approvalsChannel = NotificationChannel(
            CHANNEL_APPROVALS,
            "Registration Approvals",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Real-time alerts when tournament registrations are confirmed or approved."
            enableVibration(true)
        }

        val remindersChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Match Reminders & Room Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Upcoming Nepal Standard Time match reminders and room credential releases."
            enableVibration(true)
        }

        val generalChannel = NotificationChannel(
            CHANNEL_GENERAL,
            "Results & Rewards",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Match results, leaderboard changes, and NPR reward notices."
        }

        manager.createNotificationChannels(listOf(approvalsChannel, remindersChannel, generalChannel))
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showNotification(context: Context, notification: AppNotification) {
        ensureChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channelId = when (notification.type.uppercase()) {
            "REGISTRATION", "REGISTRATION_APPROVAL" -> CHANNEL_APPROVALS
            "MATCH_REMINDER", "ROOM_CREDENTIALS" -> CHANNEL_REMINDERS
            else -> CHANNEL_GENERAL
        }

        val targetRoute = when (notification.type.uppercase()) {
            "MATCH_REMINDER", "ROOM_CREDENTIALS" -> "my_matches"
            else -> "notifications"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_ROUTE, targetRoute)
        }

        val requestCode = notification.id.ifBlank { notification.title }.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_clutchx_notification)
            .setContentTitle(notification.title.ifBlank { "ClutchX Alert" })
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setPriority(
                if (channelId == CHANNEL_GENERAL) NotificationCompat.PRIORITY_DEFAULT
                else NotificationCompat.PRIORITY_HIGH
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(requestCode, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked concurrently
        }
    }
}
