package com.travis.inshape

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class MotivationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("MotivationReceiver", "Motivation alarm received!")

        val quotes = listOf(
            "Don’t stop now. The only bad workout is the one that didn’t happen!",
            "Stay focused, stay strong!",
            "Success starts with self-discipline."
        )
        val randomQuote = quotes.random()

        NotificationUtils.sendNotification(context, "Motivational Quote", randomQuote)

        // Reschedule the motivation alarm for 3 hours later
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val newIntent = Intent(context, MotivationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 2, newIntent, PendingIntent.FLAG_IMMUTABLE)
        val nextTriggerTime = System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR * 3

        // Check if exact alarms can be scheduled (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Prompt user to enable exact alarm permissions
                promptForExactAlarmPermission(context)
                return
            }
        }

        // Try scheduling the exact alarm
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
        } catch (e: SecurityException) {
            Log.e("MotivationReceiver", "Exact alarm permission not granted: ${e.message}")
            // Handle the lack of permission gracefully, e.g., by notifying the user
        }
    }

    private fun promptForExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                data = android.net.Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }
}
