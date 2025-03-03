package com.uz.sovchi.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.uz.sovchi.MainActivity
import com.uz.sovchi.R
import com.uz.sovchi.appContext
import com.uz.sovchi.data.filter.MyFilter
import com.uz.sovchi.showToast

class AppNotificationService : FirebaseMessagingService() {
    
    override fun onNewToken(token: String) {
        if (token == "BLACKLISTED") return
     //   MyFilter.update()
    }

    private val chanelID = "sovchi"

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        val photo = message.data["photo"]
        var builder = NotificationCompat.Builder(this, chanelID).setSmallIcon(R.drawable.ic_liked)
            .setContentTitle(message.notification?.title).setContentText(message.notification?.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        val notify = {
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.putExtra("nomzodId", message.data["nomzodId"])
            intent.putExtra("type", message.data["type"])
            intent.setAction(System.currentTimeMillis().toString())
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0,
                intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingIntent)
            notificationManager.notify(0, builder.build())
        }
        if (photo.isNullOrEmpty().not()) {
            Glide.with(appContext).asBitmap().load(photo).into(object : SimpleTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    builder = builder.setLargeIcon(resource)
                    notify()
                }
            })
        } else {
            notify()
        }
    }

    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val chanel = NotificationChannel(chanelID, "sovchi", importance)
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(chanel)
    }

}