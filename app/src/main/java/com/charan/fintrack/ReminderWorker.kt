package com.charan.fintrack
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar
class ReminderWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){
 override suspend fun doWork():Result{
  val id=inputData.getString("reminderId");val snooze=inputData.getBoolean("snooze",false)
  if(snooze){
   if(ReminderPrefs.config(applicationContext).optBoolean("enabled",false))ReminderNotifications.show(applicationContext,"snooze","night")
   return Result.success()
  }
  if(id.isNullOrBlank()||!ReminderPrefs.isEnabled(applicationContext,id))return Result.success()
  val day=Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
  val cfg=ReminderPrefs.config(applicationContext)
  if(!cfg.optBoolean("weekends",true)&&(day==Calendar.SATURDAY||day==Calendar.SUNDAY)){
   reschedule(id);return Result.success()
  }
  ReminderNotifications.show(applicationContext,id,id);reschedule(id);return Result.success()
 }
 private fun reschedule(id:String){
  val arr=ReminderPrefs.reminders(applicationContext)
  for(i in 0 until arr.length()){val item=arr.optJSONObject(i)?:continue;if(item.optString("id")==id)ReminderScheduler.scheduleNext(applicationContext,id,item.optString("time"))}
 }
}