package com.travis.inshape

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MealReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MealReminderReceiver", "Alarm received!")
        NotificationUtils.sendNotification(
            context,
            "Meal Reminder",
            "Dont forget to log your meal"
        )
    }
}