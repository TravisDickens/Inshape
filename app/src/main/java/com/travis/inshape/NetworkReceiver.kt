package com.travis.inshape

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NetworkReceiver(private val callback: ((Boolean) -> Unit)? = null) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val isConnected = NetworkUtils.isNetworkAvailable(it)
            callback?.invoke(isConnected)

            if (isConnected) {
                val title = "Network Status"
                val message = "The Database is now online"

                sendNotificationToUsers(it, title, message)
            } else {
                val title = "Network Status"
                val message = "The Database is now offline"

                sendNotificationToUsers(it, title, message)
            }
        }
    }

    private fun sendNotificationToUsers(context: Context, title: String, message: String) {

    }
}