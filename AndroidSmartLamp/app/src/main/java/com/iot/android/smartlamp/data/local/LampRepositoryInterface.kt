package com.iot.android.smartlamp.data.local

import com.iot.android.smartlamp.model.Lamp

interface LampRepositoryInterface {
    fun getAllLamps() : List<Lamp>
    fun getLampById(id : Int) : Lamp?
    fun insertLamp(lamp : Lamp) : String
    fun updateLamp(lamp : Lamp) : String
    fun updateLampState(id : Int, state : Boolean)
    fun deleteLamp(id : Int) : Unit
}
