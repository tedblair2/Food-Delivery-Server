package com.example.fudserver.adapter

import android.content.Context
import android.content.Intent
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fudserver.R
import com.example.fudserver.databinding.FoodLayoutBinding
import com.example.fudserver.model.Food
import com.squareup.picasso.Picasso

class FoodAdapter(private var foodlist:ArrayList<Food>,private val context: Context):RecyclerView.Adapter<FoodAdapter.ViewHolder>() {

    class ViewHolder(val binding: FoodLayoutBinding):RecyclerView.ViewHolder(binding.root),
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
        val view=FoodLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return foodlist.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val food=foodlist[position]
        holder.binding.menuItem.text=food.name
        Picasso.get().load(food.image).placeholder(R.drawable.not_available).into(holder.binding.menuImg)

    }
    fun search(filteredList:ArrayList<Food>){
        foodlist=filteredList
        notifyDataSetChanged()
    }
}