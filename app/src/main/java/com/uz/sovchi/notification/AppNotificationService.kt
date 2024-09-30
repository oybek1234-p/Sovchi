package com.uz.sovchi.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.uz.sovchi.MainActivity
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.UserRepository
import com.uz.sovchi.data.chat.ChatController

class AppNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        if (token == "BLACKLISTED") return
        if (token.isEmpty()) return
        UserRepository.updatePushToken(token)
    }

    private val chanelID = "sovchi"

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val nomzodId = message.data["nomzodId"]
        if (ChatController.currentOpenedChatNomzodId == nomzodId) {
            return
        }
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        val photo = message.data["photo"]
        var builder = NotificationCompat.Builder(this, chanelID)
            .setSmallIcon(R.drawable.ic_liked)
            .setContentTitle(message.notification?.title).setContentText(message.notification?.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notify = {
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.putExtra("nomzodId", nomzodId)
            intent.putExtra("type", message.data["type"])
            intent.setAction(System.currentTimeMillis().toString())
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(pendingIntent, true)
            builder.setContentIntent(pendingIntent)
            notificationManager.notify(0, builder.build())
        }
        if (photo.isNullOrEmpty().not()) {
            Glide.with(appContext).asBitmap().load(photo).into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    builder = builder.setLargeIcon(resource)
                    notify()
                }
            })
        } else {
            notify()
        }
    }

    private fun createNotificationChannel(): Boolean {
        try {
            // Check if the API level is 26 or higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    chanelID, "Sovchi", NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Sovchi ilovasi"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                // Register the channel with the system
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(channel)
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

}