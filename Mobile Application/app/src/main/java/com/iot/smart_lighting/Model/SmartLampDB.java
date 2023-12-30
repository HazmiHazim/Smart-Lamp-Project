package com.iot.smart_lighting.Model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SmartLampDB extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "SmartLamp.db";

    public SmartLampDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table lamp
        db.execSQL("CREATE TABLE IF NOT EXISTS lamp (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "ssid_name VARCHAR(50) NOT NULL, intensity INTEGER NOT NULL, connection INTEGER NOT NULL, status INTEGER NOT NULL);");

        // Create table timer
        db.execSQL("CREATE TABLE IF NOT EXISTS lampTimer (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "time VARCHAR(10) NOT NULL, status INTEGER NOT NULL, lamp_id INTEGER NOT NULL, FOREIGN KEY(lamp_id) REFERENCES lamp(id));");

        // Create table colour
        db.execSQL("CREATE TABLE IF NOT EXISTS lampColour (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "colour VARCHAR(20) NOT NULL, lamp_id INTEGER NOT NULL, FOREIGN KEY (lamp_id) REFERENCES lamp(id));");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS lamp");
        db.execSQL("DROP TABLE IF EXISTS lampTimer");
        db.execSQL("DROP TABLE IF EXISTS lampColour");
        onCreate(db);
    }

    // To downgrade your version database, uncomment this code
    /*
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
    */
}
