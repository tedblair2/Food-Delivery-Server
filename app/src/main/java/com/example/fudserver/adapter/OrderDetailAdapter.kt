package com.example.fudserver.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fudserver.databinding.OrderLayoutBinding
import com.example.fudserver.model.Cart

class OrderDetailAdapter(private val cartlist:ArrayList<Cart>):RecyclerView.Adapter<OrderDetailAdapter.ViewHolder>() {

    class ViewHolder(val binding:OrderLayoutBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view=OrderLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return cartlist.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item=cartlist[position]
        holder.binding.time.visibility= View.GONE
        holder.binding.directions.visibility=View.GONE
        holder.binding.orderId.text="Name: ${item.productName}"
        holder.binding.ship.text="Quantity: ${item.quantity}"
        holder.binding.phone.text="Price: Ksh.${item.price}"
        holder.binding.address.text="Discount: Ksh.${item.discount}"
    }
}