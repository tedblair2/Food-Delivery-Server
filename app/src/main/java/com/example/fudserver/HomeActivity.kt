package com.example.fudserver

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.fudserver.databinding.ActivityHomeBinding
import com.example.fudserver.fragments.CartFragment
import com.example.fudserver.fragments.MenuFragment
import com.example.fudserver.fragments.OrdersFragment
import com.example.fudserver.service.FudReceiver
import com.example.fudserver.service.NotificationService
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

private const val REQUEST_PERMISSION=123
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var fudReceiver: FudReceiver
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val permission= POST_NOTIFICATIONS
    private var isAllowed=false
    private val TAG="HomeActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotify()
        }
        val sender=intent.getStringExtra("sender")
        if (sender != null && sender=="service"){
            loadFragment(OrdersFragment(),"Orders Management")
        }else{
            loadFragment(MenuFragment(),"Menu Management")
        }
        fudReceiver= FudReceiver()
        val filter=IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(fudReceiver,filter)
//        startService(Intent(this,NotificationService::class.java))
        binding.bottomNav.setOnItemSelectedListener { item->
            when(item.itemId){
                R.id.menu->{
                    loadFragment(MenuFragment(),"Menu Management")
                }
                R.id.cart->loadFragment(CartFragment(),"Banner Management")
                R.id.orders->loadFragment(OrdersFragment(),"Orders Management")
            }

            true
        }

    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_PERMISSION->{
                if ((grantResults.isNotEmpty() && grantResults[0]== PackageManager.PERMISSION_GRANTED)){
                    isAllowed=true
                }else{
                    isAllowed=false
                    requestNotify()
                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotify(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,permission)){
            AlertDialog.Builder(this)
                .setTitle("Request Permission")
                .setMessage("Allow access to post notifications")
                .setPositiveButton("Ok"){_,_->
                    ActivityCompat.requestPermissions(baseContext as Activity, arrayOf(permission), REQUEST_PERMISSION)
                }
                .setNegativeButton("Cancel"){dialog,_->
                    Toast.makeText(baseContext, "Access denied.Cannot show notifications", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }.show()
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_PERMISSION)
        }
    }
    private fun loadFragment(fragment:Fragment,title:String){
        supportFragmentManager.beginTransaction().replace(R.id.fragmentcontainer,fragment).commit()
        setTitle(title)
    }
    fun disconnected(){
        binding.noInternet.visibility= View.VISIBLE
        binding.fragmentcontainer.visibility=View.GONE
    }
    fun connected(){
        binding.noInternet.visibility=View.GONE
        binding.fragmentcontainer.visibility=View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(fudReceiver)
    }
}