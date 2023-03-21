package com.example.fudserver

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fudserver.adapter.OrderAdapter
import com.example.fudserver.adapter.OrderDetailAdapter
import com.example.fudserver.databinding.ActivityOrderDetailsBinding
import com.example.fudserver.model.Cart
import com.example.fudserver.model.Order
import com.example.fudserver.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class OrderDetailsActivity : AppCompatActivity() {
    private lateinit var binding:ActivityOrderDetailsBinding
    private var cartlist= arrayListOf<Cart>()
    private lateinit var orderDetailAdapter: OrderDetailAdapter
    private lateinit var locationManager: LocationManager
    private var id=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title="Order Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        id= intent.getStringExtra("id")!!
        orderDetailAdapter= OrderDetailAdapter(cartlist)
        binding.recyclerDetails.setHasFixedSize(true)
        binding.recyclerDetails.layoutManager=LinearLayoutManager(this)
        binding.recyclerDetails.adapter=orderDetailAdapter
        getOrderDetails()
        getCartList()
        locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        binding.directions.setOnClickListener {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                startActivity(Intent(this,MapActivity::class.java).apply {
                    putExtra("id",id)
                })
            }else{
                Toast.makeText(this, "Please turn on Location Service", Toast.LENGTH_LONG).show()
            }
        }
        addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when(menuItem.itemId){
                    android.R.id.home-> {
                        finish()
                        true
                    }else->false
                }
            }
        })
    }
    private fun getTime(time:Long){
        val date= Date(time)
        val format= SimpleDateFormat("dd.MM.yyyy HH:mm")
        val ordertime=format.format(date)
        binding.time.text=ordertime
    }

    private fun getCartList() {
        Firebase.database.getReference("Orders").child(id).child("cartlist")
            .addValueEventListener(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children){
                        val item=child.getValue(Cart::class.java)!!
                        cartlist.add(item)
                    }
                    orderDetailAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun getOrderDetails() {
        Firebase.database.getReference("Orders").child(id).addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val item=snapshot.getValue(Order::class.java)!!
                getId(item.orderId!!)
                binding.address.text=item.address?.display_name
                getPhone(item.userid!!)
                getTime(item.time!!)
                binding.price.text="Ksh.${item.total}"
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }
    private fun getId(id:String){
        val regex=Regex("[^a-zA-Z0-9]")
        val result=regex.replace(id,"")
        binding.orderId.text="#$result"
    }

    private fun getPhone(userid: String){
        Firebase.database.getReference("Users").child(userid).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user=snapshot.getValue(User::class.java)!!
                binding.phone.text=user.number
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }
}