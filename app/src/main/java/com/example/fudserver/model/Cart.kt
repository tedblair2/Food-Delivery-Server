package com.example.fudserver.model

data class Cart(
    val id:Int?=null,
    val productId:String?=null,
    val productName:String?=null,
    val quantity:Int?=null,
    val discount:Int?=null,
    val price:Int?=null
)
