package com.travis.inshape

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class HydrationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("Hydration Reminder", "Hydration alarm received!")
        // Send hydration notification
        NotificationUtils.sendNotification(context, "Hydration Reminder", "Don't forget to drink water!")

        // Reschedule the hydration reminder
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminderIntent = Intent(context, HydrationReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 1, reminderIntent, PendingIntent.FLAG_IMMUTABLE)
        val nextTriggerTime = System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR * 2

        // Schedule the next alarm
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
        } catch (e: SecurityException) {
            Log.e("HydrationReminder", "Exact alarm permission not granted: ${e.message}")
        }
    }
}
