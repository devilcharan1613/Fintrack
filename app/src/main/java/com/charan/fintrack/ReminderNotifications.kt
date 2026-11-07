package com.charan.fintrack

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

object ReminderNotifications {
    private const val CHANNEL_SOUND = "fintrack_money_reminders_sound"
    private const val CHANNEL_SILENT = "fintrack_money_reminders_silent"
    private val expense = arrayOf(
        "Bro, your money didn't disappear. You just forgot to track it. 💀",
        "Your wallet after shopping: I'm tired, boss. 😭",
        "POV: You said you wouldn't spend today. 😂",
        "That ₹50 expense also deserves documentation, bro!",
        "Your bank balance wants to talk to you. 💸"
    )
    private val night = arrayOf(
        "Before sleeping, tell FinTrack where today's money went. 😂",
        "Your expenses are hiding. Time to investigate! 🕵️",
        "Tomorrow you will forget today's expenses. Update them now!",
        "Good night! Your wallet needs a financial report. 🌙"
    )
    private val standard = mapOf(
        "morning" to "Good morning! ☀️ New day, new expenses. Don't forget to track your spending!",
        "afternoon" to "Hey! 👀 Did you spend money on lunch? Update your FinTrack before you forget!",
        "evening" to "Your money has a story today. 📊 Don't leave the expenses undocumented!",
        "night" to "Daily financial check! 🌙 Where did your money go today? Let's update FinTrack!",
        "custom" to "Quick money check-in: record any income or expenses you've made."
    )

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val sound = NotificationChannel(CHANNEL_SOUND, "FinTrack reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Scheduled reminders to record income and expenses"
        }
        val silent = NotificationChannel(CHANNEL_SILENT, "FinTrack silent reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Scheduled reminders without sound"
            setSound(null, null)
        }
        manager.createNotificationChannel(sound)
        manager.createNotificationChannel(silent)
    }

    fun canPost(context: Context, soundEnabled: Boolean = true): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = if (soundEnabled) CHANNEL_SOUND else CHANNEL_SILENT
            val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(channelId)
            if (channel == null || channel.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }

    private fun reminderAction(context: Context): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).setAction("com.charan.fintrack.REMIND_LATER")
        return PendingIntent.getBroadcast(context, 103, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun openApp(context: Context, transactionType: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (transactionType != null) putExtra("fintrack_open_transaction", transactionType)
        }
        val requestCode = when (transactionType) { "income" -> 81; "expense" -> 82; else -> 80 }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun show(context: Context, id: String, category: String): Boolean {
        createChannel(context)
        val config = ReminderPrefs.config(context)
        val soundEnabled = config.optBoolean("sound", true)
        if (!canPost(context, soundEnabled)) return false
        val custom = config.optJSONArray("memeMessages")
        val customMessages = if (custom == null) emptyList() else {
            (0 until custom.length()).mapNotNull { index -> custom.optString(index).trim().takeIf { it.isNotEmpty() } }
        }
        val categoryMessages = if (category == "night") night.toList() else expense.toList()
        val messages = if (customMessages.isNotEmpty()) customMessages else categoryMessages
        val title = when (category) {
            "morning" -> "Morning money check"
            "afternoon" -> "Afternoon money check"
            "evening" -> "Evening money check"
            "night" -> "FinTrack daily review"
            else -> "FinTrack reminder"
        }
        val message = if (!config.optBoolean("memeMode", true)) {
            standard[category] ?: standard.getValue("custom")
        } else {
            val prefs = ReminderPrefs.prefs(context)
            val key = "last_message_$category"
            val previous = prefs.getInt(key, -1)
            val choices = messages.indices.filter { it != previous }
            val chosen = choices.randomOrNull() ?: 0
            prefs.edit().putInt(key, chosen).apply()
            messages[chosen]
        }
        val channelId = if (soundEnabled) CHANNEL_SOUND else CHANNEL_SILENT
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_fintrack)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .addAction(android.R.drawable.ic_menu_edit, "Add Expense", openApp(context, "expense"))
            .addAction(android.R.drawable.ic_menu_edit, "Add Income", openApp(context, "income"))
            .addAction(android.R.drawable.ic_popup_reminder, "Remind me later", reminderAction(context))
            .addAction(android.R.drawable.ic_menu_view, "Open FinTrack", openApp(context))
            .build()
        NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
        return true
    }
}
