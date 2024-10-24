package com.travis.inshape

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MealReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationUtils.sendNotification(
            context,
            "Meal Reminder",
            "Dont forget to log your meal"
        )
    }
}