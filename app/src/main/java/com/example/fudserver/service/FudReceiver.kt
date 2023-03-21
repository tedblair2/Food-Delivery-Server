package com.example.fudserver.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.example.fudserver.FoodActivity
import com.example.fudserver.HomeActivity
import com.example.fudserver.OtpViewActivity

class FudReceiver:BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        //val notConnected= intent!!.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY,false)
        when(context){
            is HomeActivity->{
                if (!isConnected(context)) context.disconnected() else context.connected()
            }
            is OtpViewActivity->{
                if (!isConnected(context)) context.disconnected() else context.connected()
            }
            is FoodActivity->{
                if (!isConnected(context)) context.disconnected() else context.connected()
            }
        }
    }
    private fun isConnected(context: Context):Boolean{
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnectedOrConnecting
    }
}