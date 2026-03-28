package com.iot.android.smartlamp.model

data class Lamp(
    val id: Int = 0,
    val publicId : String,
    val name : String,
    val model : String,
    val state : Boolean = false,
    val colour : String,
    val brightness : Int = 0,
    val txKey : String,
    val rxKey : String,
    val createdAt : Long = System.currentTimeMillis(),
    val modifiedAt : Long = System.currentTimeMillis()
)
