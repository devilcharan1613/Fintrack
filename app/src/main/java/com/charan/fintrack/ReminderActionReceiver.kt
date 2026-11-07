package com.charan.fintrack

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderActionReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            "com.charan.fintrack.REMIND_LATER" -> ReminderScheduler.scheduleSnooze(context)
            "com.charan.fintrack.ADD_EXPENSE", "com.charan.fintrack.ADD_INCOME" -> {
                val type=if(intent.action=="com.charan.fintrack.ADD_INCOME") "income" else "expense"
                val launch=Intent(context,MainActivity::class.java).apply {
                    flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("fintrack_open_transaction",type)
                }
                context.startActivity(launch)
            }
        }
    }
}