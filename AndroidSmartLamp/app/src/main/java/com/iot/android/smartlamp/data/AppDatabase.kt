package com.iot.android.smartlamp.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabase(context : Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db : SQLiteDatabase) {
        db.execSQL("CREATE TABLE lamps (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "public_id TEXT NOT NULL, name TEXT NOT NULL, model TEXT NOT NULL, state INTEGER NOT NULL, " +
                "colour TEXT, brightness INTEGER, tx_key TEXT NOT NULL, rx_key TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, modified_at INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db : SQLiteDatabase, oldVersion : Int, newVersion : Int) {
        db.execSQL("DROP TABLE IF EXISTS lamps")
        onCreate(db)
    }

    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "SmartLamp.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}