package com.tracmio.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import java.nio.charset.StandardCharsets

data class NotificationItem(
    val id: Int,
    val category: String,
    val icon: String,
    val title: String,
    val body: String
)

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        showReminderNotification(context, true)
        TracmioReminderScheduler.rescheduleFromPrefs(context)
    }

    companion object {
        const val CHANNEL_ID = "tracmio_daily"
        const val CHANNEL_NAME = "Tracmio daily reminder"
        const val NOTIFICATION_ID = 4102
        const val EXTRA_QUICK_ACTION = "tracmio_quick_action"
        private const val KEY_LAST_MEME_INDEX = "last_meme_index"
        private const val KEY_LAST_STD_INDEX = "last_std_index"
        private const val KEY_LAST_NOTIFICATION_INDEX = "last_notification_index"

        val MEME_MESSAGES = listOf(
            "Your wallet called. It wants today's transactions logged 💸 Add your income and expenses!",
            "Money moved? Tell Tracmio about it before you forget 👀 Add a transaction now.",
            "Your budget is waiting for its daily update 📊 Log today's income and expenses.",
            "UPI went 'Ting!' 5 times today 📱 Did Tracmio hear about that Swiggy order?",
            "Salary came in like a lion and left like a lamb 🦁➡️🐑 Track where it went today!",
            "Checking bank balance without logging expenses is horror movie behavior 👻 Log now!"
        )

        val STANDARD_MESSAGES = listOf(
            "Your daily money check-in is ready. Add today's income and expenses to keep your balance accurate.",
            "A one-minute update helps you stay on top of your money. Log today's transactions in Tracmio.",
            "Small daily updates make monthly reports useful. Add any income or expenses you made today."
        )

        fun parseNotificationCatalogJson(jsonText: String): List<NotificationItem> {
            return runCatching {
                val array = JSONArray(jsonText)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        add(
                            NotificationItem(
                                id = obj.optInt("id", i + 1),
                                category = obj.optString("category", "ALL"),
                                icon = obj.optString("icon", "💸"),
                                title = obj.optString("title", "Tracmio — Daily Check-In"),
                                body = obj.optString("body", "")
                            )
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }

        fun loadNotificationListFromAssets(context: Context): List<NotificationItem> {
            val jsonText = runCatching {
                context.assets.open("notifications.json")
                    .bufferedReader(StandardCharsets.UTF_8)
                    .use { it.readText() }
            }.getOrDefault("")
            val parsed = if (jsonText.isNotBlank()) parseNotificationCatalogJson(jsonText) else emptyList()
            if (parsed.isNotEmpty()) return parsed
            return MEME_MESSAGES.mapIndexed { idx, body ->
                NotificationItem(
                    id = idx + 1,
                    category = "MEME",
                    icon = "💸",
                    title = "Tracmio — Daily Check-In",
                    body = body
                )
            }
        }

        fun getNextNotification(context: Context, memeMode: Boolean = true): NotificationItem {
            val prefs = TracmioReminderScheduler.getPrefs(context)
            val categoryPref = prefs.getString(
                TracmioReminderScheduler.KEY_ALERT_CATEGORY,
                "ALL"
            ) ?: "ALL"
            val allItems = loadNotificationListFromAssets(context)
            val filtered = when (categoryPref.uppercase()) {
                "MEME" -> allItems.filter { it.category.equals("MEME", ignoreCase = true) }
                "MILESTONES" -> allItems.filter { it.category.equals("MILESTONES", ignoreCase = true) }
                "HYGIENE" -> allItems.filter { it.category.equals("HYGIENE", ignoreCase = true) }
                else -> allItems
            }.ifEmpty { allItems }

            val currentIndex = prefs.getInt(KEY_LAST_NOTIFICATION_INDEX, 0).coerceAtLeast(0)
            val selectedItem = filtered[currentIndex % filtered.size]
            prefs.edit()
                .putInt(KEY_LAST_NOTIFICATION_INDEX, (currentIndex + 1) % allItems.size.coerceAtLeast(1))
                .apply()
            return selectedItem
        }

        fun nextMessage(context: Context, memeMode: Boolean): String {
            val prefs = TracmioReminderScheduler.getPrefs(context)
            return if (memeMode) {
                val last = prefs.getInt(KEY_LAST_MEME_INDEX, -1)
                val next = (last + 1) % MEME_MESSAGES.size
                prefs.edit().putInt(KEY_LAST_MEME_INDEX, next).apply()
                MEME_MESSAGES[next]
            } else {
                val last = prefs.getInt(KEY_LAST_STD_INDEX, -1)
                val next = (last + 1) % STANDARD_MESSAGES.size
                prefs.edit().putInt(KEY_LAST_STD_INDEX, next).apply()
                STANDARD_MESSAGES[next]
            }
        }

        fun formatDailySpendSummaryLine(
            todaySpentFormatted: String,
            txCount: Int,
            safeRemainingFormatted: String
        ): String {
            return "Today: $todaySpentFormatted spent across $txCount transactions · $safeRemainingFormatted left under your Safe-to-Spend cap."
        }

        fun showReminderNotification(context: Context, memeMode: Boolean) {
            createChannel(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val item = getNextNotification(context, true)
            val userName = TracmioReminderScheduler.getUserName(context)
            val baseTitle = if (item.icon.isNotBlank()) "${item.icon} ${item.title}" else item.title
            val title = if (userName.isNotBlank() && !baseTitle.contains(userName, ignoreCase = true)) {
                "Hey $userName · $baseTitle"
            } else {
                baseTitle
            }
            val rawMessage = item.body.ifBlank { nextMessage(context, true) }
            val personalized = if (userName.isNotBlank() && !rawMessage.contains(userName, ignoreCase = true)) {
                "$userName, $rawMessage"
            } else {
                rawMessage
            }
            val summaryLine = TracmioReminderScheduler.getDailySummaryLine(context)
            val message = if (summaryLine.isNotBlank()) {
                "$summaryLine\n$personalized"
            } else {
                personalized
            }
            val immutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag

            val openIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val contentPendingIntent = PendingIntent.getActivity(context, 4100, openIntent, flags)

            val incomeIntent = Intent(context, ReminderNotificationActionReceiver::class.java).apply {
                putExtra(EXTRA_QUICK_ACTION, "income")
            }
            val incomePendingIntent = PendingIntent.getBroadcast(context, 4103, incomeIntent, flags)

            val expenseIntent = Intent(context, ReminderNotificationActionReceiver::class.java).apply {
                putExtra(EXTRA_QUICK_ACTION, "expense")
            }
            val expensePendingIntent = PendingIntent.getBroadcast(context, 4104, expenseIntent, flags)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .addAction(0, "+ Add Income", incomePendingIntent)
                .addAction(0, "+ Add Expense", expensePendingIntent)
                .build()

            runCatching {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            }
        }

        private fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Offline daily reminder to log income and expenses in Tracmio"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
