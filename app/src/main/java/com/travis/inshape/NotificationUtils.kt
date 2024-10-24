package com.travis.inshape

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationUtils {

    fun sendNotification(context: Context, title: String, message: String) {
        // Check if notification permission is granted (Android 13 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // If permission is not granted, show a message or request the permission
                Toast.makeText(context, "Please enable notification permissions for this app.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Proceed with sending notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "inshape_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Inshape Notifications", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.logocropped) // Add your app icon
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            // Use NotificationManagerCompat to display the notification
            NotificationManagerCompat.from(context).notify(1, notification)
        } catch (e: SecurityException) {
            Toast.makeText(context, "Notification permission is required to display notifications.", Toast.LENGTH_SHORT).show()
        }
    }
}
