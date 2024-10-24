package com.travis.inshape

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class HydrationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Send hydration notification
        NotificationUtils.sendNotification(context, "Hydration Reminder", "Don't forget to drink water!")
    }
}
