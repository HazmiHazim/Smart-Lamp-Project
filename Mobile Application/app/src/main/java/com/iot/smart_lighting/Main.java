package com.iot.smart_lighting;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.iot.smart_lighting.Model.SmartLampDB;


public class Main extends AppCompatActivity {

    // Variable Declaration
    CardView feature1, feature2, feature3, feature4;
    ImageView info;
    SmartLampDB myDB;
    SQLiteDatabase sqlDB;

    // Declare ESP32 class
    Esp32 esp32;

    // Declare VoiceRecognition class
    VoiceRecognition voiceRec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        feature1 = findViewById(R.id.cardView1);
        feature2 = findViewById(R.id.cardView2);
        feature3 = findViewById(R.id.cardView3);
        feature4 = findViewById(R.id.cardView4);
        info = findViewById(R.id.info_btn1);

        myDB = new SmartLampDB(Main.this);

        // Instantiate ESP32 class
        esp32 = new Esp32(Main.this);

        // Call ping function to connect with ESP32
        esp32.pingESP32();

        // Ask for permission to access location and to access microphone
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO}, 200);

        // Info dialog box shown when click for user guide commands
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Display info in alert dialog box
                AlertDialog.Builder infoBox = new AlertDialog.Builder(Main.this);
                infoBox.setIcon(R.drawable.phoenix);
                infoBox.setTitle("Phoenix Intelligence Guide");
                infoBox.setMessage("Step 1: Say 'HEY PHOENIX' to wake up Phoenix." +
                        "\nStep 2: Say 'COMMANDS' to let Phoenix know you want to issue a command." +
                        "\nStep 3: Say a simple command, such as 'TURN <ON|OFF> LAMP <1|2|3>'.");
                AlertDialog alertDialog = infoBox.create();
                alertDialog.show();
            }
        });

        // Navigation event when click feature 1
        feature1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Main.this, LampController.class);
                startActivity(intent);
            }
        });

        // Navigation event when click feature 2
        feature2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Main.this, Timer.class);
                startActivity(intent);
            }
        });

        // Navigation event when click feature 3
        feature3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Main.this, ColourEditor.class);
                startActivity(intent);
            }
        });

        // Navigation event when click feature 4
        feature4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Main.this, DataAnalysis.class);
                startActivity(intent);
            }
        });
    }

    // Function to request permission for location and microphone
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] granResults) {
        super.onRequestPermissionsResult(requestCode, permissions, granResults);
        if (requestCode == 200) {
            if (granResults.length > 0 && granResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Call SQLite function to create data if not exists, if exists then do nothing
                createOrRetrieve();
                // Instantiate VoiceRecognition class
                voiceRec = new VoiceRecognition(Main.this);
            } else {
                Toast.makeText(Main.this, "Permission to access location is required for data-saving purposes.", Toast.LENGTH_SHORT).show();
                Toast.makeText(Main.this, "Permission to access microphone is required for voice-controlled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Function to create if the table is empty, otherwise retrieve the table data
    public void createOrRetrieve() {
        // Use try-finally to ensure db is close no matter what happen
        try {
            // Open The Database for Reading
            sqlDB = myDB.getReadableDatabase();
            String query = "SELECT * FROM lamp";
            Cursor cursor = sqlDB.rawQuery(query, null);
            if (cursor != null && cursor.moveToFirst()) {
                //Toast.makeText(Main.this, "Locally Data is Received.", Toast.LENGTH_SHORT).show();
                cursor.close();
            } else {
                String esp32Ssid = esp32.getESP32Ssid();
                if (esp32Ssid.equalsIgnoreCase("\"ESP32-SMART-LAMP\"")) {
                    // Lamp 1
                    createData(1, esp32Ssid, 0, 1, 0);
                    // Lamp 2
                    createData(2, esp32Ssid, 0, 1, 0);
                    // Lamp 3
                    createData(3, esp32Ssid, 0, 1, 0);
                } else {
                    Toast.makeText(Main.this, "Connection Failed.", Toast.LENGTH_SHORT).show();
                }
            }
        } finally {
            sqlDB.close();
        }
    }

    // Function to create a data into local database by given parameters
    private void createData(int id, String ssid, int intensity, int connection, int status) {
        // Use try-finally to ensure db is close no matter what happen
        try {
            sqlDB = myDB.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("id", id);
            cv.put("ssid_name", ssid);
            cv.put("intensity", intensity);
            cv.put("connection", connection);
            cv.put("status", status);
            sqlDB.insert("lamp", null, cv);
        } finally {
            sqlDB.close();
        }
    }
}
