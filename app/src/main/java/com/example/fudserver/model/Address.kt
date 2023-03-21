package com.example.fudserver.model

data class Address(
    val display_name: String?=null,
    val lat: String?=null,
    val lon: String?=null,
){
    override fun toString(): String {
        return display_name?:""
    }
}
