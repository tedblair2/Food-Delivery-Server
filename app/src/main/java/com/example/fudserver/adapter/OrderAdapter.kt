package com.example.fudserver.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.fudserver.MapActivity
import com.example.fudserver.OrderDetailsActivity
import com.example.fudserver.databinding.OrderLayoutBinding
import com.example.fudserver.model.Order
import com.example.fudserver.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class OrderAdapter(private val orderlist:ArrayList<Order>,private val context: Context):RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    class ViewHolder(val binding:OrderLayoutBinding):RecyclerView.ViewHolder(binding.root),
        View.OnCreateContextMenuListener{
        init {
            binding.root.setOnCreateContextMenuListener(this)
        }
        override fun onCreateContextMenu(
            p0: ContextMenu?,
            p1: View?,
            p2: ContextMenu.ContextMenuInfo?
        ) {
            p0?.setHeaderTitle("Select Action")
            p0?.add(0,1,adapterPosition,"Update")
            p0?.add(0,2,adapterPosition,"Delete")
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view=OrderLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return orderlist.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item=orderlist[position]
        getId(item.orderId!!,holder)
        holder.binding.address.visibility=View.GONE
        holder.binding.ship.text=getStatus(item.status!!)
        getPhone(item.userid!!,holder)
        getTime(item.time!!,holder)

        val locationManager=context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        holder.itemView.setOnClickListener {
            context.startActivity(Intent(context,OrderDetailsActivity::class.java).apply {
                putExtra("id",item.orderId)
            })
        }

        holder.binding.directions.setOnClickListener {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                context.startActivity(Intent(context,MapActivity::class.java).apply {
                    putExtra("id",item.orderId)
                })
            }else{
                Toast.makeText(context, "Please turn on Location Service", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun getId(id:String,holder: ViewHolder){
        val regex=Regex("[^a-zA-Z0-9]")
        val result=regex.replace(id,"")
        holder.binding.orderId.text="#$result"
    }
    @SuppressLint("SimpleDateFormat")
    private fun getTime(time: Long, holder: OrderAdapter.ViewHolder) {
        val date= Date(time)
        val format= SimpleDateFormat("dd.MM.yyyy HH:mm")
        val ordertime=format.format(date)
        holder.binding.time.text=ordertime
    }
    private fun getPhone(userid: String, holder: ViewHolder) {
        Firebase.database.getReference("Users").child(userid).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user=snapshot.getValue(User::class.java)!!
                holder.binding.phone.text=user.number
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun getStatus(number:String):String{
        return when (number) {
            "0" -> {
                "PLACED"
            }
            "1" -> {
                "ON THE WAY"
            }
            else -> {
                "SHIPPED"
            }
        }
    }
}