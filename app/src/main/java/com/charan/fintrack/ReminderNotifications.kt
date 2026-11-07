package com.charan.fintrack

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlin.random.Random

object ReminderNotifications {
    private const val CHANNEL="fintrack_money_reminders"
    private val expense=arrayOf(
        "Bro, your money didn't disappear. You just forgot to track it. 💀",
        "Your wallet after shopping: I'm tired, boss. 😭",
        "POV: You said you wouldn't spend today. 😂",
        "That ₹50 expense also deserves documentation, bro!",
        "Your bank balance wants to talk to you. 💸"
    )
    private val income=arrayOf(
        "Money entered the chat! 🤑 Don't forget to record your income.",
        "Finally, your hard work paid off. Log that income!",
        "Bro is earning! Update FinTrack like a responsible legend. 🔥"
    )
    private val night=arrayOf(
        "Before sleeping, tell FinTrack where today's money went. 😂",
        "Your expenses are hiding. Time to investigate! 🕵️",
        "Tomorrow you will forget today's expenses. Update them now!",
        "Good night! Your wallet needs a financial report. 🌙"
    )
    private val standard=mapOf(
        "morning" to "Good morning! ☀️ New day, new expenses. Don't forget to track your spending!",
        "afternoon" to "Hey! 👀 Did you spend money on lunch? Update your FinTrack before you forget!",
        "evening" to "Your money has a story today. 📊 Don't leave the expenses undocumented!",
        "night" to "Daily financial check! 🌙 Where did your money go today? Let's update FinTrack!"
    )
    fun createChannel(context: Context) {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            val channel=NotificationChannel(CHANNEL,"FinTrack reminders",NotificationManager.IMPORTANCE_DEFAULT)
            channel.description="Scheduled reminders to record income and expenses"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    private fun action(context: Context, action: String, request: Int): PendingIntent {
        val i=Intent(context,ReminderActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(context,request,i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    private fun content(context: Context, type: String?=null): PendingIntent {
        val i=Intent(context,MainActivity::class.java).apply {
            flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if(type!=null) putExtra("fintrack_open_transaction",type)
        }
        return PendingIntent.getActivity(context,if(type=="income") 81 else 80,i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    fun show(context: Context, id: String, category: String) {
        createChannel(context)
        if(!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return
        val cfg=ReminderPrefs.config(context)
        val meme=cfg.optBoolean("memeMode",true)
        val messages=when(category) { "night" -> night; "morning","afternoon","evening" -> expense; else -> expense }
        val title=when(category) { "morning"->"Morning money check";"afternoon"->"Afternoon money check";"evening"->"Evening money check";else->"FinTrack daily review" }
        val message=if(!meme) standard[category] ?: standard["night"]!! else {
            val prefs=ReminderPrefs.prefs(context)
            val key="last_message_$category"
            val previous=prefs.getInt(key,-1)
            val available=messages.indices.filter{it!=previous}
            val selected=available.randomOrNull() ?: Random.nextInt(messages.size)
            prefs.edit().putInt(key,selected).apply()
            messages[selected]
        }
        val builder=NotificationCompat.Builder(context,CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title).setContentText(message).setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true)
            .setContentIntent(content(context))
            .addAction(0,"Add Expense",action(context,"com.charan.fintrack.ADD_EXPENSE",101))
            .addAction(0,"Add Income",action(context,"com.charan.fintrack.ADD_INCOME",102))
            .addAction(0,"Remind me later",action(context,"com.charan.fintrack.REMIND_LATER",103))
            .setSilent(!cfg.optBoolean("sound",true))
        NotificationManagerCompat.from(context).notify(id.hashCode(),builder.build())
    }
}