package com.travis.inshape

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class HydrationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("Hydration Reminder", "Hydration alarm received!")
        // Send hydration notification
        NotificationUtils.sendNotification(context, "Hydration Reminder", "Don't forget to drink water!")
    }
}
