package com.tracemeo.app

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray

class DailyReminderReceiver : BroadcastReceiver() {
    data class NotificationCatalogItem(
        val id: String,
        val category: String,
        val title: String,
        val body: String
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        TracmioReminderScheduler.ensureChannel(context)
        val item = pickNextNotificationItem(context)
        val userName = intent.getStringExtra("user_name").orEmpty().ifBlank { "Friend" }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context,
            100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val addIncomeIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_QUICK_ACTION, "income")
        }
        val addIncomePi = PendingIntent.getActivity(
            context,
            101,
            addIncomeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val addExpenseIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_QUICK_ACTION, "expense")
        }
        val addExpensePi = PendingIntent.getActivity(
            context,
            102,
            addExpenseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, TracmioReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("${item.title} · $userName")
            .setContentText(item.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .addAction(0, "+ Add income", addIncomePi)
            .addAction(0, "+ Add expense", addExpensePi)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.notify(1613, notification)
    }

    private fun pickNextNotificationItem(context: Context): NotificationCatalogItem {
        val raw = runCatching {
            context.assets.open("notifications.json").bufferedReader().use { it.readText() }
        }.getOrNull().orEmpty()
        val items = parseNotificationCatalogJson(raw)
        if (items.isEmpty()) {
            return NotificationCatalogItem(
                id = "1",
                category = "HYGIENE",
                title = "9 PM Expense Check-In",
                body = "Log today's income and expenses in Tracmio."
            )
        }
        val prefs = context.getSharedPreferences("tracmio_notif_rotation", Context.MODE_PRIVATE)
        val idx = prefs.getInt("last_idx", 0) % items.size
        prefs.edit().putInt("last_idx", (idx + 1) % items.size).apply()
        return items[idx]
    }

    companion object {
        const val EXTRA_QUICK_ACTION = "tracmio_quick_action"

        fun parseNotificationCatalogJson(jsonText: String): List<NotificationCatalogItem> {
            return runCatching {
                val arr = JSONArray(jsonText)
                val list = mutableListOf<NotificationCatalogItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        NotificationCatalogItem(
                            id = obj.optString("id", "${i + 1}"),
                            category = obj.optString("category", "HYGIENE"),
                            title = obj.optString("title", "Tracmio Reminder"),
                            body = obj.optString("body", "Log today's income and expenses.")
                        )
                    )
                }
                list
            }.getOrDefault(emptyList())
        }
    }
}
