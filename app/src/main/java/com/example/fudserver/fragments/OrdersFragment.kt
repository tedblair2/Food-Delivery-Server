package com.example.fudserver.fragments

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fudserver.R
import com.example.fudserver.ShipperActivity
import com.example.fudserver.adapter.OrderAdapter
import com.example.fudserver.databinding.FragmentOrdersBinding
import com.example.fudserver.databinding.UpdateOrderBinding
import com.example.fudserver.model.Order
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class OrdersFragment : Fragment() {
    private var _binding:FragmentOrdersBinding?=null
    private val binding get() = _binding!!
    private var orderlist= arrayListOf<Order>()
    private lateinit var orderAdapter: OrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding= FragmentOrdersBinding.inflate(inflater,container,false)
        requireActivity().addMenuProvider(menu,viewLifecycleOwner,Lifecycle.State.RESUMED)
        orderAdapter= OrderAdapter(orderlist,requireContext())
        binding.recyclerOrders.setHasFixedSize(true)
        val layoutManager=LinearLayoutManager(requireContext())
        layoutManager.reverseLayout=true
        layoutManager.stackFromEnd=true
        binding.recyclerOrders.layoutManager=layoutManager
        binding.recyclerOrders.adapter=orderAdapter
        getOrders()


        return binding.root
    }

    private fun getOrders() {
        Firebase.database.getReference("Orders").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                orderlist.clear()
                for (child in snapshot.children){
                    val item=child.getValue(Order::class.java)!!
                    orderlist.add(item)
                }
                orderAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo==null) return
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when(item.title){
            "Update"-> {
                updateOrder(item.order)
                true
            }
            "Delete"->{
                deleteOrder(item.order)
                true
            }
            else->super.onContextItemSelected(item)
        }
    }
    private val menu=object: MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.top_menu,menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when(menuItem.itemId){
                R.id.shippper->{
                    startActivity(Intent(requireContext(), ShipperActivity::class.java))
                    true
                }
                else->false
            }
        }

    }
    private fun updateOrder(position: Int){
        val item=orderlist[position]
        val ref=item.orderId!!
        val view=LayoutInflater.from(requireContext()).inflate(R.layout.update_order,null)
        val alertBinding=UpdateOrderBinding.bind(view)
        val options= listOf("Placed","On the Way","Shipped")
        val autoAdapter=ArrayAdapter(requireContext(),R.layout.drop_down_items,options)
        alertBinding.status.setText(getStatus(item.status!!))
        (alertBinding.status as? AutoCompleteTextView)?.setAdapter(autoAdapter)
        AlertDialog.Builder(requireContext())
            .setTitle("Update Status")
            .setMessage("Please select status")
            .setView(view)
            .setPositiveButton("Update"){dialog,_->
                binding.loading.visibility=View.VISIBLE
                val status=returnStatus(alertBinding.status.text.toString())
                if (TextUtils.isEmpty(status)){
                    alertBinding.parentStatus.error="Status cannot be empty"
                }else{
                    val update= hashMapOf<String,Any>()
                    update["status"]=status
                    Firebase.database.getReference("Orders").child(ref).updateChildren(update).addOnCompleteListener {
                        if (it.isSuccessful){
                            dialog.dismiss()
                            binding.loading.visibility=View.GONE
                            Toast.makeText(requireContext(), "Update successful", Toast.LENGTH_SHORT).show()
                        }else{
                            binding.loading.visibility=View.GONE
                            Toast.makeText(requireContext(), it.exception?.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel"){dialog,_->
                dialog.dismiss()
            }
            .create()
            .show()

    }
    private fun returnStatus(status:String):String{
        return when(status){
            "Placed"->"0"
            "On the Way"->"1"
            else->"2"
        }
    }
    private fun getStatus(status:String): String {
        return when (status) {
            "0" -> {
                "Placed"
            }
            "1" -> {
                "On the Way"
            }
            else -> {
                "Shipped"
            }
        }
    }

    private fun deleteOrder(position:Int){
        val item=orderlist[position]
        val ref=item.orderId!!
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Order #$ref")
            .setMessage("Are you sure you want to delete this order?")
            .setPositiveButton("Yes"){_,_->
                Firebase.database.getReference("Orders").child(ref).removeValue().addOnCompleteListener { task->
                    if (task.isSuccessful){
                        Toast.makeText(requireContext(), "Successfully deleted Order #$ref", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(requireContext(), task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("No"){dialog,_->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }
}