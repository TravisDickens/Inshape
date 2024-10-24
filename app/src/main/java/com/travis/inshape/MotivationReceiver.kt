package com.travis.inshape

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MotivationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val quotes = listOf(
            "Don’t stop now. The only bad workout is the one that didn’t happen!",
            "Stay focused, stay strong!",
            "Success starts with self-discipline."
        )
        val randomQuote = quotes.random()

        NotificationUtils.sendNotification(context, "Motivational Quote", randomQuote)
    }
}
