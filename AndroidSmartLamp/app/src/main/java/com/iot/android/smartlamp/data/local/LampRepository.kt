package com.iot.android.smartlamp.data.local

import android.content.ContentValues
import android.util.Log
import com.iot.android.smartlamp.model.Lamp

class LampRepository(private val db : AppDatabase) : LampRepositoryInterface  {

    override fun getAllLamps() : List<Lamp> {
        val dbReader = db.readableDatabase
        val lamps = mutableListOf<Lamp>()

        val cursor = dbReader.query(
            "lamps",
            null,
            null,
            null,
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val lamp = Lamp(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    publicId = it.getString(it.getColumnIndexOrThrow("public_id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    model = it.getString(it.getColumnIndexOrThrow("model")),
                    state = it.getInt(it.getColumnIndexOrThrow("state")) == 1,
                    colour = it.getString(it.getColumnIndexOrThrow("colour")) ?: "",
                    brightness = it.getInt(it.getColumnIndexOrThrow("brightness")),
                    txKey = it.getString(it.getColumnIndexOrThrow("tx_key")),
                    rxKey = it.getString(it.getColumnIndexOrThrow("rx_key")),
                    createdAt = it.getLong(it.getColumnIndexOrThrow("modified_at")),
                    modifiedAt = it.getLong(it.getColumnIndexOrThrow("modified_at"))
                )
                lamps.add(lamp)
            }
        }

        return lamps
    }

    override fun getLampById(id : Int) : Lamp? {
        val dbReader = db.readableDatabase
        val cursor = dbReader.query(
            "lamps",
            null,
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        var lamp : Lamp? = null

        cursor.use {
            if (it.moveToFirst()) {
                lamp = Lamp(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    publicId = it.getString(it.getColumnIndexOrThrow("public_id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    model = it.getString(it.getColumnIndexOrThrow("model")),
                    state = it.getInt(it.getColumnIndexOrThrow("state")) == 1,
                    colour = it.getString(it.getColumnIndexOrThrow("colour")) ?: "",
                    brightness = it.getInt(it.getColumnIndexOrThrow("brightness")),
                    txKey = it.getString(it.getColumnIndexOrThrow("tx_key")),
                    rxKey = it.getString(it.getColumnIndexOrThrow("rx_key")),
                    modifiedAt = it.getLong(it.getColumnIndexOrThrow("modified_at"))
                )
            }
        }

        return lamp
    }

    override fun insertLamp(lamp : Lamp) : String {
        Log.d("DatabaseLayer", "TX: " + lamp.txKey)
        Log.d("DatabaseLayer", "RX: " + lamp.rxKey)
        val values = ContentValues().apply {
            put("public_id", lamp.publicId)
            put("name", lamp.name)
            put("model", lamp.model)
            put("state", lamp.state)
            put("colour", lamp.colour)
            put("brightness", lamp.brightness)
            put("tx_key", lamp.txKey)
            put("rx_key", lamp.rxKey)
            put("created_at", System.currentTimeMillis())
            put("modified_at", System.currentTimeMillis())
        }
        db.writableDatabase.insert("lamps", null, values)
        return lamp.publicId
    }

    override fun updateLamp(lamp : Lamp) : String {
        val values = ContentValues().apply {
            put("name", lamp.name)
            put("model", lamp.model)
            put("state", lamp.state)
            put("colour", lamp.colour)
            put("brightness", lamp.brightness)
            put("modified_at", System.currentTimeMillis())
        }

        db.writableDatabase.update("lamps", values, "id = ?", arrayOf(lamp.id.toString()))
        return lamp.publicId
    }

    override fun updateLampState(id : Int, state : Boolean) {
        val values = ContentValues().apply {
            put("state", if (state) 1 else 0)
            put("modified_at", System.currentTimeMillis())
        }
        db.writableDatabase.update(
            "lamps",
            values,
            "id = ?",
            arrayOf(id.toString())
        )
    }

    override fun updateLampColour(id : Int, colour : String) {
        val values = ContentValues().apply {
            put("colour", colour)
            put("modified_at", System.currentTimeMillis())
        }
        db.writableDatabase.update("lamps", values, "id = ?", arrayOf(id.toString()))
    }

    override fun deleteLamp(id : Int) : Unit {
        db.writableDatabase.delete("lamps", "id = ?", arrayOf(id.toString()))
    }
}
