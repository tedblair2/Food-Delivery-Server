package com.example.fudserver.adapter

import android.content.Context
import android.content.Intent
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fudserver.FoodActivity
import com.example.fudserver.R
import com.example.fudserver.databinding.MenuLayoutBinding
import com.example.fudserver.model.Category
import com.squareup.picasso.Picasso

class MenuAdapter(private val menulist:ArrayList<Category>,
                  private val context: Context):RecyclerView.Adapter<MenuAdapter.ViewHolder>(){

    inner class ViewHolder(val binding: MenuLayoutBinding):RecyclerView.ViewHolder(binding.root),
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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuAdapter.ViewHolder {
        val view=MenuLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuAdapter.ViewHolder, position: Int) {
        val item=menulist[position]
        holder.binding.menuItem.text=item.name
        Picasso.get().load(item.image).placeholder(R.drawable.not_available).into(holder.binding.menuImg)
        holder.itemView.setOnClickListener {
            context.startActivity(Intent(context, FoodActivity::class.java).apply {
                putExtra("id",item.id)
                putExtra("name",item.name)
            })
        }
    }

    override fun getItemCount(): Int {
        return menulist.size
    }
    fun showItem(value:Int):String{
        val item=menulist[value]
        return item.name!!
    }
}