package com.charan.fintrack
import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit
object ReminderScheduler {
 private const val TAG="fintrack-reminder"
 fun scheduleAll(context:Context){
  WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
  val c=ReminderPrefs.config(context); if(!c.optBoolean("enabled",false)) return
  val arr=c.optJSONArray("reminders")?:return
  for(i in 0 until arr.length()){val item=arr.optJSONObject(i)?:continue;if(item.optBoolean("enabled",true))scheduleNext(context,item.optString("id"),item.optString("time"))}
 }
 fun scheduleNext(context:Context,id:String,time:String){
  if(!ReminderPrefs.isEnabled(context,id))return
  val p=time.split(":");if(p.size!=2)return
  val h=p[0].toIntOrNull()?:return;val m=p[1].toIntOrNull()?:return
  val now=Calendar.getInstance();val next=now.clone() as Calendar
  next.set(Calendar.HOUR_OF_DAY,h);next.set(Calendar.MINUTE,m);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0)
  if(!next.after(now))next.add(Calendar.DAY_OF_YEAR,1)
  if(!ReminderPrefs.config(context).optBoolean("weekends",true)){
   while(next.get(Calendar.DAY_OF_WEEK)==Calendar.SATURDAY||next.get(Calendar.DAY_OF_WEEK)==Calendar.SUNDAY)next.add(Calendar.DAY_OF_YEAR,1)
  }
  val delay=(next.timeInMillis-System.currentTimeMillis()).coerceAtLeast(1000)
  val req=OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay,TimeUnit.MILLISECONDS)
   .setInputData(workDataOf("reminderId" to id,"time" to time)).addTag(TAG).build()
  WorkManager.getInstance(context).enqueueUniqueWork("fintrack-reminder-$id",ExistingWorkPolicy.REPLACE,req)
 }
 fun scheduleSnooze(context:Context){
  val req=OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(15,TimeUnit.MINUTES)
   .setInputData(workDataOf("snooze" to true)).addTag(TAG).build()
  WorkManager.getInstance(context).enqueue(req)
 }
}