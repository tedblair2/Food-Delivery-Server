package com.example.fudserver.adapter

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fudserver.R
import com.example.fudserver.databinding.MenuLayoutBinding
import com.example.fudserver.model.Banner
import com.squareup.picasso.Picasso

class BannerAdapter(private val bannerlist:ArrayList<Banner>):RecyclerView.Adapter<BannerAdapter.ViewHolder>() {

    class ViewHolder(val binding:MenuLayoutBinding):RecyclerView.ViewHolder(binding.root),
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
            p0?.add(0,0,adapterPosition,"Update")
            p0?.add(0,1,adapterPosition,"Delete")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view=MenuLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return bannerlist.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item=bannerlist[position]
        holder.binding.menuItem.text=item.name
        Picasso.get().load(item.image).placeholder(R.drawable.not_available).into(holder.binding.menuImg)
    }
}