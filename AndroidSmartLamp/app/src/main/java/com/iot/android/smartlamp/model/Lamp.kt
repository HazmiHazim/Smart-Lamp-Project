package com.iot.android.smartlamp.model

import com.google.gson.annotations.SerializedName

data class Lamp(
    val id: Int = 0,
    @SerializedName("public_id")
    val publicId : String,
    val name : String,
    val model : String,
    val state : Boolean = false,
    val colour : String,
    val brightness : Int = 0,
    @SerializedName("tx_key")
    val txKey : String,
    @SerializedName("rx_key")
    val rxKey : String,
    val createdAt : Long = System.currentTimeMillis(),
    val modifiedAt : Long = System.currentTimeMillis()
)