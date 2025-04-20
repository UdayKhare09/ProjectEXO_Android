package dev.uday.projectexo_android.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.uday.projectexo_android.MainActivity
import dev.uday.projectexo_android.R

object NotificationHelper {
    private const val CHANNEL_ID = "exo_chat_channel"
    private const val NOTIFICATION_GROUP = "exo_notifications"
    private var notificationId = 1000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Chat Messages"
            val descriptionText = "Notifications for incoming chat messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 100, 50, 100)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showMessageNotification(context: Context, sender: String, message: String, isPrivate: Boolean) {
        // Create an intent to open the app and navigate to the specific chat
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("CHAT_DESTINATION", if (isPrivate) sender else "general")
        }

        val pendingIntentFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, pendingIntentFlags
        )

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (isPrivate) "Private message from $sender" else "New message in chat")
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setGroup(NOTIFICATION_GROUP)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId++, notificationBuilder.build())
            } catch (e: SecurityException) {
                // Handle the case where notification permission is not granted
                e.printStackTrace()
            }
        }
    }
}