package com.example.fudserver.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.fudserver.HomeActivity
import com.example.fudserver.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.ktx.remoteMessage
import java.util.*

class FirebaseService:FirebaseMessagingService() {
    private val channel_id="${R.string.app_name}"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Firebase.database.getReference("Users").child(Firebase.auth.currentUser!!.uid).child("token")
            .setValue(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (message.data.isNotEmpty()){
            showNotification(message)
        }
    }

    private fun showNotification(message: RemoteMessage) {
        createNotificationChannel()
        val intent= Intent(baseContext, HomeActivity::class.java)
        intent.putExtra("sender","service")
        val pendingIntent= PendingIntent.getActivity(baseContext,0,intent, PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE)
        val builder=NotificationCompat.Builder(baseContext,channel_id)
            .setSmallIcon(R.drawable.ic_restaurant)
            .setContentTitle(message.data["title"])
            .setContentText(message.data["body"])
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificicationId= Random().nextInt(9999-1)+1
        val manager= NotificationManagerCompat.from(baseContext)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        manager.notify(notificicationId,builder)
    }
    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            val channel= NotificationChannel(channel_id,"${R.string.app_name}", NotificationManager.IMPORTANCE_HIGH)
            channel.description="${R.string.app_name}"
            val notificationManager=baseContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}