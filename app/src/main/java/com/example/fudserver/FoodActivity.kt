package com.example.fudserver

import android.app.ProgressDialog
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.fudserver.adapter.FoodAdapter
import com.example.fudserver.databinding.ActivityFoodBinding
import com.example.fudserver.databinding.AddFoodBinding
import com.example.fudserver.model.Food
import com.example.fudserver.service.FudReceiver
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso

private const val PICK_FOOD=20
private const val UPDATE_FOOD=21
class FoodActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFoodBinding
    private lateinit var foodAdapter: FoodAdapter
    private var foodList= arrayListOf<Food>()
    private lateinit var alertDialog: AlertDialog
    private lateinit var alertBinding:AddFoodBinding
    private var imageUri:Uri?=null
    private var updateUri:Uri?=null
    private var id=""
    private lateinit var progressDialog: ProgressDialog
    private lateinit var fudReceiver: FudReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityFoodBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fudReceiver= FudReceiver()
        registerReceiver(fudReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        val name=intent.getStringExtra("name")!!
        title = name
        progressDialog= ProgressDialog(this)
        progressDialog.setMessage("Uploading...")
        progressDialog.setCancelable(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        foodAdapter= FoodAdapter(foodList,this)
        binding.recyclerFood.setHasFixedSize(true)
        binding.recyclerFood.layoutManager= GridLayoutManager(this,2)
        binding.recyclerFood.adapter=foodAdapter
        id=intent.getStringExtra("id")!!
        getFood()
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

        binding.add.setOnClickListener {
            showAlert()
        }
    }
    fun disconnected(){
        binding.recyclerFood.visibility=View.GONE
        binding.add.visibility=View.GONE
        binding.noInternet.visibility=View.VISIBLE
    }
    fun connected(){
        binding.recyclerFood.visibility=View.VISIBLE
        binding.add.visibility=View.VISIBLE
        binding.noInternet.visibility=View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(fudReceiver)
    }

    private fun showAlert() {
        val view=LayoutInflater.from(this).inflate(R.layout.add_food,null)
        alertBinding= AddFoodBinding.bind(view)
        alertDialog=AlertDialog.Builder(this@FoodActivity)
            .setView(view)
            .create()
        alertBinding.icon.setColorFilter(Color.BLACK)
        alertBinding.categoryimg.setOnClickListener {
            selectimage()
        }
        alertBinding.send.setOnClickListener {
            val name=alertBinding.foodname.text.toString()
            val description=alertBinding.foodDesc.text.toString()
            val price=alertBinding.foodprice.text.toString()
            val discount=alertBinding.foodDiscount.text.toString()
            if (TextUtils.isEmpty(name)){
                alertBinding.parentname.error="Please provide food name"
            }else if (TextUtils.isEmpty(description)){
                alertBinding.parentdesc.error="Please provide description"
            }else if (TextUtils.isEmpty(price)){
                alertBinding.parentprice.error="Please provide price"
            }else if (TextUtils.isEmpty(discount)){
                alertBinding.parentdiscount.error="Please provide a discount"
            }else if (imageUri==null){
                Toast.makeText(this, "Please provide an image", Toast.LENGTH_SHORT).show()
            }else{
                progressDialog.show()
                addData(name,description,price,discount)
            }
        }
        alertBinding.cancel.setOnClickListener {
            alertDialog.dismiss()
            imageUri=null
        }
        alertDialog.show()
    }

    private fun addData(name: String, description: String, price: String, discount: String) {
        val storage=Firebase.storage.getReference("Images/${System.currentTimeMillis()}")
        storage.putFile(imageUri!!).addOnSuccessListener {
            storage.downloadUrl.addOnCompleteListener { task->
                if (task.isSuccessful){
                    val ref=Firebase.database.getReference("Foods")
                    val foodId=ref.push().key!!
                    val food=Food(description,discount,foodId,task.result.toString(),id,name,price)
                    ref.child(foodId).setValue(food).addOnCompleteListener {
                        alertDialog.dismiss()
                        progressDialog.dismiss()
                        imageUri=null
                        Snackbar.make(binding.recyclerFood,"Successfully added $name",Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }.addOnFailureListener {
            progressDialog.dismiss()
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectimage() {
        val intent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_FOOD)
    }
    private fun updateImage(){
        val intent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, UPDATE_FOOD)
    }

    private fun getFood() {
        Firebase.database.getReference("Foods").addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                foodList.clear()
                for (child in snapshot.children){
                    val item=child.getValue(Food::class.java)!!
                    if (item.menuId==id){
                        foodList.add(item)
                    }
                }
                foodAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FoodActivity, error.message, Toast.LENGTH_SHORT).show()
            }

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode== RESULT_OK && data !=null){
            when(requestCode){
                PICK_FOOD->{
                    imageUri=data.data
                    alertBinding.categoryimg.scaleType=ImageView.ScaleType.CENTER_CROP
                    alertBinding.categoryimg.setImageURI(imageUri)
                }
                UPDATE_FOOD->{
                    updateUri=data.data
                    alertBinding.categoryimg.scaleType=ImageView.ScaleType.CENTER_CROP
                    alertBinding.categoryimg.setImageURI(updateUri)
                }
            }
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (menuInfo==null) return
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when(item.title){
            "Update"->{
                updateFood(item.order)
                true
            }
            "Delete"->{
                deleteFood(item.order)
                true
            }
            else->super.onContextItemSelected(item)
        }
    }

    private fun updateFood(position: Int) {
        val item=foodList[position]
        val ref=item.id!!
        val view=LayoutInflater.from(this).inflate(R.layout.add_food,null)
        alertBinding= AddFoodBinding.bind(view)
        alertDialog=AlertDialog.Builder(this@FoodActivity)
            .setView(view)
            .create()
        alertBinding.icon.setColorFilter(Color.BLACK)
        alertBinding.categoryimg.scaleType=ImageView.ScaleType.CENTER_CROP
        alertBinding.categorytitle.text="Update food ${item.name}"
        alertBinding.foodname.setText(item.name)
        alertBinding.foodDesc.setText(item.description)
        alertBinding.foodprice.setText(item.price)
        alertBinding.foodDiscount.setText(item.discount)
        Picasso.get().load(item.image).placeholder(R.drawable.not_available).into(alertBinding.categoryimg)
        alertBinding.categoryimg.setOnClickListener {
            updateImage()
        }
        alertBinding.send.setOnClickListener {
            val name=alertBinding.foodname.text.toString()
            val description=alertBinding.foodDesc.text.toString()
            val price=alertBinding.foodprice.text.toString()
            val discount=alertBinding.foodDiscount.text.toString()
            if (TextUtils.isEmpty(name)){
                alertBinding.parentname.error="Please provide food name"
            }else if (TextUtils.isEmpty(description)){
                alertBinding.parentdesc.error="Please provide description"
            }else if (TextUtils.isEmpty(price)){
                alertBinding.parentprice.error="Please provide price"
            }else if (TextUtils.isEmpty(discount)){
                alertBinding.parentdiscount.error="Please provide a discount"
            }else if (updateUri==null){
                val update= hashMapOf<String,Any>()
                update["description"]=description
                update["discount"]=discount
                update["price"]=price
                update["name"]=name
                Firebase.database.getReference("Foods").child(ref).updateChildren(update).addOnCompleteListener { task->
                    if (task.isSuccessful){
                        alertDialog.dismiss()
                        Toast.makeText(this, "Update successful", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }else{
                progressDialog.show()
                uploadUpdate(name,description,price,discount,ref)
            }
        }
        alertBinding.cancel.setOnClickListener {
            alertDialog.dismiss()
            updateUri=null
        }
        alertDialog.show()

    }
    private fun uploadUpdate(name: String, description: String, price: String, discount: String, ref: String) {
        val storage=Firebase.storage.getReference("Images/${System.currentTimeMillis()}")
        storage.putFile(updateUri!!).addOnSuccessListener {
            storage.downloadUrl.addOnCompleteListener { task->
                if (task.isSuccessful){
                    val database=Firebase.database.getReference("Foods")
                    val update= hashMapOf<String,Any>()
                    update["description"]=description
                    update["discount"]=discount
                    update["price"]=price
                    update["name"]=name
                    update["image"]=task.result.toString()
                    database.child(ref).updateChildren(update).addOnCompleteListener {
                        alertDialog.dismiss()
                        progressDialog.dismiss()
                        updateUri=null
                        Snackbar.make(binding.recyclerFood,"Successfully updated $name",Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }.addOnFailureListener {
            progressDialog.dismiss()
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFood(position: Int) {
        val item=foodList[position]
        val ref=item.id!!
        AlertDialog.Builder(this)
            .setTitle("Delete Food")
            .setMessage("Are you sure you want to delete this food?")
            .setNegativeButton("No"){alert,_->
                alert.dismiss()
            }
            .setPositiveButton("Yes"){_,_->
                Firebase.database.getReference("Foods").child(ref).removeValue()
                foodAdapter.notifyItemRemoved(position)
                Snackbar.make(binding.recyclerFood,"${item.name} successfully deleted",Snackbar.LENGTH_LONG)
                    .setAction("Undo"){
                        Firebase.database.getReference("Foods").child(ref).setValue(item)
                        foodAdapter.notifyItemInserted(position)
                    }
                    .setActionTextColor(Color.BLUE)
                    .show()
            }
            .show()
    }
}