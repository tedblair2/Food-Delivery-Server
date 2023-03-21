package com.example.fudserver.fragments

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fudserver.R
import com.example.fudserver.ShipperActivity
import com.example.fudserver.adapter.BannerAdapter
import com.example.fudserver.databinding.AddFoodBinding
import com.example.fudserver.databinding.FragmentCartBinding
import com.example.fudserver.model.Banner
import com.example.fudserver.model.Food
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso

private const val SELECT=30
private const val UPDATE=31
class CartFragment : Fragment() {
    private var _binding:FragmentCartBinding?=null
    private val binding get() = _binding!!
    private val bannerlist= arrayListOf<Banner>()
    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var alertbinding:AddFoodBinding
    private lateinit var progressDialog:ProgressDialog
    private lateinit var dialog: AlertDialog
    private var imageUri: Uri?=null
    private var updateUri:Uri?=null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding= FragmentCartBinding.inflate(inflater,container,false)
        requireActivity().addMenuProvider(menu,viewLifecycleOwner, Lifecycle.State.RESUMED)
        bannerAdapter= BannerAdapter(bannerlist)
        binding.recyclerBanner.setHasFixedSize(true)
        progressDialog= ProgressDialog(requireContext())
        progressDialog.setMessage("Uploading...")
        progressDialog.setCancelable(false)
        val layoutManager=LinearLayoutManager(requireContext())
        layoutManager.reverseLayout=true
        layoutManager.stackFromEnd=true
        binding.recyclerBanner.layoutManager=layoutManager
        binding.recyclerBanner.adapter=bannerAdapter
        getBanner()
        binding.add.setOnClickListener {
            addDialog()
        }
        return binding.root
    }

    private fun addDialog() {
        val view=LayoutInflater.from(requireContext()).inflate(R.layout.add_food,null)
        alertbinding= AddFoodBinding.bind(view)
        dialog=AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
        alertbinding.icon.setColorFilter(Color.BLACK)
        alertbinding.categoryimg.setOnClickListener {
            selectimage()
        }
        alertbinding.send.setOnClickListener {
            val name=alertbinding.foodname.text.toString()
            val description=alertbinding.foodDesc.text.toString()
            val price=alertbinding.foodprice.text.toString()
            val discount=alertbinding.foodDiscount.text.toString()
            if (TextUtils.isEmpty(name)){
                alertbinding.parentname.error="Please provide food name"
            }else if (TextUtils.isEmpty(description)){
                alertbinding.parentdesc.error="Please provide description"
            }else if (TextUtils.isEmpty(price)){
                alertbinding.parentprice.error="Please provide price"
            }else if (TextUtils.isEmpty(discount)){
                alertbinding.parentdiscount.error="Please provide a discount"
            }else if (imageUri==null){
                Toast.makeText(requireContext(), "Please provide an image", Toast.LENGTH_SHORT).show()
            }else{
                progressDialog.show()
                addData(name,description,price,discount)
            }
        }
        alertbinding.cancel.setOnClickListener {
            imageUri=null
            dialog.dismiss()
        }
        dialog.show()

    }

    private fun addData(name: String, description: String, price: String, discount: String) {
        val storage=Firebase.storage.getReference("Banner/${System.currentTimeMillis()}")
        storage.putFile(imageUri!!).addOnSuccessListener {
            storage.downloadUrl.addOnCompleteListener { task->
                if (task.isSuccessful){
                    val ban=Firebase.database.getReference("Banner")
                    val id=ban.push().key!!
                    val ref=Firebase.database.getReference("Foods")
                    val foodId=ref.push().key!!
                    val banner=Banner(id,task.result.toString(),name,foodId)
                    val food= Food(description,discount,foodId,task.result.toString(),"00",name,price)
                    ban.child(id).setValue(banner).addOnSuccessListener {
                        ref.child(foodId).setValue(food).addOnCompleteListener {
                            progressDialog.dismiss()
                            dialog.dismiss()
                            imageUri=null
                            Snackbar.make(binding.recyclerBanner,"Successfully added $name", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }.addOnFailureListener {
            progressDialog.dismiss()
            Toast.makeText(requireContext(), "Failed!!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectimage() {
        val intent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, SELECT)
    }

    private fun getBanner() {
        Firebase.database.getReference("Banner").addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                bannerlist.clear()
                for (child in snapshot.children){
                    val item=child.getValue(Banner::class.java)!!
                    bannerlist.add(item)
                }
                bannerAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
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
            "Update"->{
                updateItem(item.order)
                true
            }
            "Delete"->{
                deleteItem(item.order)
                true
            }
            else->super.onContextItemSelected(item)
        }
    }
    private fun deleteItem(position: Int){
        val item=bannerlist[position]
        val ref=item.id!!
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Food")
            .setMessage("Are you sure you want to delete this banner?")
            .setNegativeButton("No"){alert,_->
                alert.dismiss()
            }
            .setPositiveButton("Yes"){_,_->
                Firebase.database.getReference("Banner").child(ref).removeValue()
                Firebase.database.getReference("Foods").child(item.foodId!!).removeValue()
                bannerAdapter.notifyItemRemoved(position)
                Snackbar.make(binding.recyclerBanner,"${item.name} successfully deleted",Snackbar.LENGTH_LONG).show()
            }
            .show()
    }
    private fun updateItem(position:Int){
        val item=bannerlist[position]
        val ref=item.id!!
        val view=LayoutInflater.from(requireContext()).inflate(R.layout.add_food,null)
        alertbinding= AddFoodBinding.bind(view)
        dialog= AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
        alertbinding.icon.setColorFilter(Color.BLACK)
        alertbinding.categoryimg.scaleType=ImageView.ScaleType.CENTER_CROP
        alertbinding.categorytitle.text="Update Banner ${item.name}"
        alertbinding.foodname.setText(item.name)
        Firebase.database.getReference("Foods").child(item.foodId!!).addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val food=snapshot.getValue(Food::class.java)
                alertbinding.foodDesc.setText(food?.description)
                alertbinding.foodprice.setText(food?.price)
                alertbinding.foodDiscount.setText(food?.discount)
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
        Picasso.get().load(item.image).placeholder(R.drawable.not_available).into(alertbinding.categoryimg)
        alertbinding.categoryimg.setOnClickListener {
            updateImage()
        }
        alertbinding.send.setOnClickListener {
            val name=alertbinding.foodname.text.toString()
            val description=alertbinding.foodDesc.text.toString()
            val price=alertbinding.foodprice.text.toString()
            val discount=alertbinding.foodDiscount.text.toString()
            if (TextUtils.isEmpty(name)){
                alertbinding.parentname.error="Please provide a name"
            }else if (TextUtils.isEmpty(description)){
                alertbinding.parentdesc.error="Please provide description"
            }else if (TextUtils.isEmpty(price)){
                alertbinding.parentprice.error="Please provide price"
            }else if (TextUtils.isEmpty(discount)){
                alertbinding.parentdiscount.error="Please provide a discount"
            }else if (updateUri==null){
                val banner= hashMapOf<String,Any>()
                banner["name"]=name
                val food= hashMapOf<String,Any>()
                food["description"]=description
                food["discount"]=discount
                food["price"]=price
                food["name"]=name
                Firebase.database.getReference("Banner").child(ref).updateChildren(banner).addOnSuccessListener {
                    Firebase.database.getReference("Foods").child(item.foodId).updateChildren(food).addOnCompleteListener { task->
                        if (task.isSuccessful){
                            dialog.dismiss()
                            Toast.makeText(requireContext(), "Update successful", Toast.LENGTH_SHORT).show()
                        }else{
                            Toast.makeText(requireContext(), task.exception?.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }else{
                progressDialog.show()
                uploadUpdate(name,description,price,discount,item)
            }
        }
        alertbinding.cancel.setOnClickListener {
            dialog.dismiss()
            updateUri=null
        }
        dialog.show()
    }

    private fun updateImage() {
        val intent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, UPDATE)
    }

    private fun uploadUpdate(name: String, description: String, price: String, discount: String, item: Banner) {
        val storage=Firebase.storage.getReference("Banner/${System.currentTimeMillis()}")
        storage.putFile(updateUri!!).addOnSuccessListener {
            storage.downloadUrl.addOnCompleteListener { task->
                if (task.isSuccessful){
                    val banner= hashMapOf<String,Any>()
                    banner["name"]=name
                    banner["image"]=task.result.toString()
                    val database=Firebase.database.getReference("Foods")
                    val food= hashMapOf<String,Any>()
                    food["description"]=description
                    food["discount"]=discount
                    food["price"]=price
                    food["name"]=name
                    food["image"]=task.result.toString()
                    Firebase.database.getReference("Banner").child(item.id!!).updateChildren(banner).addOnSuccessListener {
                        database.child(item.foodId!!).updateChildren(food).addOnCompleteListener {
                            dialog.dismiss()
                            progressDialog.dismiss()
                            updateUri=null
                            Snackbar.make(binding.recyclerBanner,"Successfully updated $name",Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }.addOnFailureListener {
            progressDialog.dismiss()
            Toast.makeText(requireContext(), "Failed!!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode==RESULT_OK && data != null){
            when(requestCode){
                SELECT->{
                    imageUri=data.data
                    alertbinding.categoryimg.scaleType=ImageView.ScaleType.CENTER_CROP
                    alertbinding.categoryimg.setImageURI(imageUri)
                }
                UPDATE->{
                    updateUri=data.data
                    alertbinding.categoryimg.scaleType=ImageView.ScaleType.CENTER_CROP
                    alertbinding.categoryimg.setImageURI(updateUri)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }

}