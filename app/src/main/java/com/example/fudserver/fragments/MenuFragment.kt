package com.example.fudserver.fragments

import android.app.Activity.RESULT_OK
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
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fudserver.R
import com.example.fudserver.ShipperActivity
import com.example.fudserver.adapter.MenuAdapter
import com.example.fudserver.databinding.AddLayoutBinding
import com.example.fudserver.databinding.FragmentMenuBinding
import com.example.fudserver.model.Category
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso

private const val PICK_IMAGE=12
private const val UPDATE_IMAGE=15
class MenuFragment : Fragment() {
    private var _binding:FragmentMenuBinding?=null
    private val binding get() = _binding!!
    private lateinit var menuAdapter: MenuAdapter
    private val menulist= arrayListOf<Category>()
    private var imageUri:Uri?=null
    private var updateUri:Uri?=null
    private lateinit var progresssdialog:ProgressDialog
    private lateinit var dialog: AlertDialog
    private lateinit var alertbinding:AddLayoutBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding= FragmentMenuBinding.inflate(inflater,container,false)
        requireActivity().addMenuProvider(menu,viewLifecycleOwner, Lifecycle.State.RESUMED)
        menuAdapter= MenuAdapter(menulist,requireContext())
        progresssdialog= ProgressDialog(requireContext())
        progresssdialog.setMessage("Uploading...")
        progresssdialog.setCancelable(false)
        registerForContextMenu(binding.recyclerMenu)
        binding.recyclerMenu.setHasFixedSize(true)
        binding.recyclerMenu.layoutManager=LinearLayoutManager(requireContext())
        binding.recyclerMenu.adapter=menuAdapter
        getItems()

        binding.add.setOnClickListener {
            addCategory()
        }
        return binding.root
    }

    private fun addCategory() {
        val view=LayoutInflater.from(requireContext()).inflate(R.layout.add_layout,null)
        alertbinding=AddLayoutBinding.bind(view)
        dialog=AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
        alertbinding.icon.setColorFilter(Color.BLACK)
        alertbinding.categoryimg.setOnClickListener {
            selectimage()
        }
        alertbinding.send.setOnClickListener {
            val category=alertbinding.categorytext.text.toString()
            if (TextUtils.isEmpty(category)){
                alertbinding.categorytext.error="Please provide a category"
            }else if (imageUri == null){
                Toast.makeText(requireContext(), "Please provide an image", Toast.LENGTH_SHORT).show()
            }else{
                progresssdialog.show()
                uploadData(category)
            }
        }
        alertbinding.cancel.setOnClickListener {
            dialog.dismiss()
            imageUri=null
        }
        dialog.show()
    }

    private fun uploadData(category: String) {
        val storage=Firebase.storage.getReference("Category/${System.currentTimeMillis()}")
        storage.putFile(imageUri!!).addOnSuccessListener { upload->
            storage.downloadUrl.addOnCompleteListener { task->
                if (task.isSuccessful){
                    val ref=Firebase.database.getReference("Category")
                    val id=ref.push().key!!
                    val item=Category(id,task.result.toString(),category)
                    ref.child(id).setValue(item).addOnCompleteListener { add->
                        if (add.isSuccessful){
                            progresssdialog.dismiss()
                            dialog.dismiss()
                            imageUri=null
                            Snackbar.make(binding.recyclerMenu,"Successfully added Category $category"
                                ,Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }.addOnFailureListener {
            progresssdialog.dismiss()
            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
        }.addOnProgressListener { taskSnapshot->
            val progress=(100 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            progresssdialog.progress=progress
        }
    }
    private val menu=object:MenuProvider{
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.top_menu,menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when(menuItem.itemId){
                R.id.shippper->{
                    startActivity(Intent(requireContext(),ShipperActivity::class.java))
                    true
                }
                else->false
            }
        }

    }

    private fun selectimage() {
        val intent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE)
    }

    private fun getItems() {
        Firebase.database.getReference("Category").addValueEventListener(object:
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                menulist.clear()
                for (child in snapshot.children){
                    val item=child.getValue(Category::class.java)!!
                    menulist.add(item)
                }
                menuAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode==RESULT_OK && data != null){
            when(requestCode){
                PICK_IMAGE->{
                    imageUri=data.data
                    alertbinding.categoryimg.scaleType=ImageView.ScaleType.CENTER_CROP
                    alertbinding.categoryimg.setImageURI(imageUri)
                }
                UPDATE_IMAGE->{
                    updateUri=data.data
                    alertbinding.categoryimg.scaleType=ImageView.ScaleType.CENTER_CROP
                    alertbinding.categoryimg.setImageURI(updateUri)
                }
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

    private fun updateItem(order: Int) {
        val item=menulist[order]
        val ref=item.id!!
        val view=LayoutInflater.from(requireContext()).inflate(R.layout.add_layout,null)
        alertbinding=AddLayoutBinding.bind(view)
        dialog=AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
        alertbinding.categorytitle.text="Update Category ${item.name}"
        alertbinding.icon.setColorFilter(Color.BLACK)
        alertbinding.categoryimg.scaleType=ImageView.ScaleType.CENTER_CROP
        Picasso.get().load(item.image).placeholder(R.drawable.not_available).into(alertbinding.categoryimg)
        alertbinding.categoryimg.setOnClickListener {
            updateimage()
        }
        alertbinding.categorytext.setText(item.name)
        alertbinding.send.setOnClickListener {
            val category=alertbinding.categorytext.text.toString()
            if (TextUtils.isEmpty(category)){
                alertbinding.categorytext.error="Please provide a category"
            }else if (updateUri==null){
                val map= hashMapOf<String,Any>()
                map["name"]=category
                Firebase.database.getReference("Category").child(ref).updateChildren(map).addOnCompleteListener { task->
                    if (task.isSuccessful){
                        dialog.dismiss()
                        Toast.makeText(requireContext(), "Update Successful", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(requireContext(), task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }else{
                progresssdialog.show()
                updateCategory(category,ref)
            }
        }
        alertbinding.cancel.setOnClickListener {
            dialog.dismiss()
            updateUri=null
        }
        dialog.show()
    }

    private fun updateCategory(category: String, child: String){
        val storage=Firebase.storage.getReference("Category/${System.currentTimeMillis()}")
        storage.putFile(updateUri!!).addOnSuccessListener { upload->
            storage.downloadUrl.addOnCompleteListener { task->
                if (task.isSuccessful){
                    val ref=Firebase.database.getReference("Category")
                    val map= hashMapOf<String,Any>()
                    map["name"]=category
                    map["image"]=task.result.toString()
                    ref.child(child).updateChildren(map).addOnCompleteListener { add->
                        if (add.isSuccessful){
                            progresssdialog.dismiss()
                            dialog.dismiss()
                            updateUri=null
                            Snackbar.make(binding.recyclerMenu,"Successfully updated Category $category"
                                ,Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }.addOnFailureListener {
            progresssdialog.dismiss()
            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
        }.addOnProgressListener { taskSnapshot->
            val progress=(100 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            progresssdialog.progress=progress
        }
    }

    private fun updateimage() {
        val intent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, UPDATE_IMAGE)
    }

    private fun deleteItem(position:Int) {
        val item=menulist[position]
        val ref=item.id!!
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete this category?")
            .setNegativeButton("No"){alert,_->
                alert.dismiss()
            }
            .setPositiveButton("Yes"){_,_->
                Firebase.database.getReference("Category").child(ref).removeValue()
                menuAdapter.notifyItemRemoved(position)
                Snackbar.make(binding.recyclerMenu,"${item.name} successfully deleted",Snackbar.LENGTH_LONG)
                    .setAction("Undo"){
                        Firebase.database.getReference("Category").child(ref).setValue(item)
                        menuAdapter.notifyItemInserted(position)
                    }
                    .setActionTextColor(Color.BLUE)
                    .show()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }
}