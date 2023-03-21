package com.example.fudserver

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.example.fudserver.databinding.ActivityOtpViewBinding
import com.example.fudserver.service.FudReceiver
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import `in`.aabhasjindal.otptextview.OTPListener
import java.util.concurrent.TimeUnit

class OtpViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOtpViewBinding
    private lateinit var dialog: ProgressDialog
    private var number=""
    private var name=""
    private var verificationId=""
    private lateinit var auth: FirebaseAuth
    private lateinit var fudReceiver: FudReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityOtpViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        fudReceiver= FudReceiver()
        registerReceiver(fudReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        dialog= ProgressDialog(this@OtpViewActivity)
        dialog.setMessage("Sending OTP....")
        dialog.setCancelable(false)
        dialog.show()
        auth= FirebaseAuth.getInstance()
        number= intent.getStringExtra("number")!!
        name=intent.getStringExtra("name")!!
        binding.title.text="Verify $number"

        val options= PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)

        binding.otpview.otpListener=object :OTPListener{
            override fun onInteractionListener() {

            }

            override fun onOTPComplete(otp: String) {
                binding.progress.visibility= View.VISIBLE
                val credentials=PhoneAuthProvider.getCredential(verificationId,otp)
                auth.signInWithCredential(credentials).addOnCompleteListener { task->
                    if (task.isSuccessful){
                        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { result ->
                            if (!task.isSuccessful) {
                                Log.d("OtpActivity", "Fetching FCM registration token failed", result.exception)
                                return@OnCompleteListener
                            }
                            val token = result.result
                            val user= hashMapOf<String,Any>()
                            user["number"]=number
                            user["name"]=name
                            user["userid"]=Firebase.auth.currentUser!!.uid
                            user["isStaff"]=true
                            user["token"]=token
                            Firebase.database.reference.child("Users")
                                .child(Firebase.auth.currentUser!!.uid).setValue(user)
                                .addOnCompleteListener {add->
                                    if (add.isSuccessful){
                                        startActivity(Intent(this@OtpViewActivity,HomeActivity::class.java).apply {
                                            flags=Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                        })
                                        binding.progress.visibility=View.GONE
                                    }else{
                                        binding.progress.visibility=View.GONE
                                        Toast.makeText(this@OtpViewActivity, add.exception?.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                        })
                    }else{
                        binding.progress.visibility=View.GONE
                        Toast.makeText(this@OtpViewActivity, task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(fudReceiver)
    }
    fun disconnected(){
        binding.card.visibility=View.GONE
        binding.top.visibility=View.GONE
        binding.noInternet.visibility=View.VISIBLE
    }
    fun connected(){
        binding.card.visibility=View.VISIBLE
        binding.top.visibility=View.VISIBLE
        binding.noInternet.visibility=View.GONE
    }
    private val callbacks=object: PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
        override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
            super.onCodeSent(p0, p1)
            dialog.dismiss()
            verificationId=p0
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
            binding.otpview.requestFocus()
        }

        override fun onVerificationCompleted(p0: PhoneAuthCredential) {
        }

        override fun onVerificationFailed(p0: FirebaseException) {
        }

    }
}