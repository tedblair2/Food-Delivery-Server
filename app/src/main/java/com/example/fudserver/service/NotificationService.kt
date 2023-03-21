package com.example.fudserver.service

import android.Manifest
import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.fudserver.HomeActivity
import com.example.fudserver.R
import com.example.fudserver.model.Order
import com.google.firebase.database.*
import java.util.Random

private const val channel_id="FuD"
class NotificationService : Service() {
    private lateinit var databaseReference: DatabaseReference
    private lateinit var notificationManager: NotificationManager

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        databaseReference=FirebaseDatabase.getInstance().getReference("Orders")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        databaseReference.addChildEventListener(childEventListener)
        return super.onStartCommand(intent, flags, startId)
    }
    private val childEventListener=object :ChildEventListener{
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val order=snapshot.getValue(Order::class.java)
            if (order != null && order.status=="0"){
                val currenttime=System.currentTimeMillis()
                val ordertime=order.time
                if (currenttime- ordertime!! <= 60000){
                    showNotification(order)
                }
            }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
        }

        override fun onCancelled(error: DatabaseError) {
        }

    }

    private fun showNotification(order: Order) {
        createNotificationChannel()
        val intent=Intent(baseContext,HomeActivity::class.java)
        intent.putExtra("sender","service")
        val pendingIntent=PendingIntent.getActivity(baseContext,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or
        PendingIntent.FLAG_IMMUTABLE)
        val builder=NotificationCompat.Builder(baseContext, channel_id)
            .setSmallIcon(R.drawable.ic_restaurant)
            .setContentTitle("New Order")
            .setContentText("New order #${order.orderId}")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val notificicationId=Random().nextInt(9999-1)+1
        val manager=NotificationManagerCompat.from(baseContext)
        if (ActivityCompat.checkSelfPermission(this, POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        manager.notify(notificicationId,builder)
    }

    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            val channel= NotificationChannel(channel_id,"${R.string.app_name}", NotificationManager.IMPORTANCE_HIGH)
            channel.description="${R.string.app_name}"
            notificationManager=baseContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}