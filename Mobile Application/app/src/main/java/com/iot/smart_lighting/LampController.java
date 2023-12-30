package com.iot.smart_lighting;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.iot.smart_lighting.Model.SmartLampDB;


public class LampController extends AppCompatActivity {

    // Variable Declaration
    Switch switch1, switch2, switch3;
    ImageView back, info, bulb1, bulb2, bulb3, bulb1_on, bulb2_on, bulb3_on;
    SeekBar intensity1, intensity2, intensity3;
    SmartLampDB myDB;
    SQLiteDatabase sqlDB;

    // Declare ESP32 class
    Esp32 esp32;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lamp_controller);

        back = findViewById(R.id.back_btn1);
        info = findViewById(R.id.info_btn2);

        // Assign XML to variable for Lamp 1
        switch1 = findViewById(R.id.lSwitch1);
        bulb1 = findViewById(R.id.lImage1);
        bulb1_on = findViewById(R.id.lImage1_on);
        intensity1 = findViewById(R.id.seekbar1);

        // Assign XML to variable for Lamp 2
        switch2 = findViewById(R.id.lSwitch2);
        bulb2 = findViewById(R.id.lImage2);
        bulb2_on = findViewById(R.id.lImage2_on);
        intensity2 = findViewById(R.id.seekbar2);

        // Assign XML to variable for Lamp 3
        switch3 = findViewById(R.id.lSwitch3);
        bulb3 = findViewById(R.id.lImage3);
        bulb3_on = findViewById(R.id.lImage3_on);
        intensity3 = findViewById(R.id.seekbar3);

        // Create instance for SmartLampDB
        myDB = new SmartLampDB(LampController.this);

        // Instantiate ESP32 Class
        esp32 = new Esp32(LampController.this);

        // Get initial state of each lamp
        getInitialStates(switch1, bulb1, bulb1_on, intensity1, 1);
        getInitialStates(switch2, bulb2, bulb2_on, intensity2, 2);
        getInitialStates(switch3, bulb3, bulb3_on, intensity3, 3);

        // Event when click switch 1
        int intensityValue1 = getIntensityValue(1);  // Call getIntensityValue() to get the value from database
        eventSwitch(switch1, bulb1, bulb1_on, intensity1, 1, "http://192.168.4.1/lamp1/on?value=" + intensityValue1, "http://192.168.4.1/lamp1/off");
        // Event when click switch 2
        int intensityValue2 = getIntensityValue(2);  // Call getIntensityValue() to get the value from database
        eventSwitch(switch2, bulb2, bulb2_on, intensity2, 2, "http://192.168.4.1/lamp2/on?value=" + intensityValue2, "http://192.168.4.1/lamp2/off");
        // Event when click switch 3
        int intensityValue3 = getIntensityValue(3);  // Call getIntensityValue() to get the value from database
        eventSwitch(switch3, bulb3, bulb3_on, intensity3, 3, "http://192.168.4.1/lamp3/on?value=" + intensityValue3, "http://192.168.4.1/lamp3/off");

        // Event when slide the seekbar 1
        eventSeekBar(intensity1, 1, "http://192.168.4.1/lamp1/on?value=");
        // Event when slide the seekbar 2
        eventSeekBar(intensity2, 2, "http://192.168.4.1/lamp2/on?value=");
        // Event when slide the seekbar 3
        eventSeekBar(intensity3, 3, "http://192.168.4.1/lamp3/on?value=");

        // Info dialog box shown when click for user guide commands
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder infoBox = new AlertDialog.Builder(LampController.this);
                infoBox.setIcon(R.drawable.phoenix);
                infoBox.setTitle("ON/OFF Commands");
                infoBox.setMessage("Supported Commands:" +
                        "\nTurn On Lamp <1|2|3>" +
                        "\nTurn On All Lamps" +
                        "\nTurn Off Lamp <1|2|3>" +
                        "\nTurn Off All Lamps");
                AlertDialog alertDialog = infoBox.create();
                alertDialog.show();
            }
        });

        // Event when click back icon button
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    // Function for Switch Click Event
    public void eventSwitch(Switch switchButton, ImageView bulbOff, ImageView bulbOn, SeekBar intensity, int lampId, String onEndpoint, String offEndpoint) {
        switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                if (isChecked) {
                    bulbOff.setVisibility(View.GONE);
                    bulbOn.setVisibility(View.VISIBLE);
                    intensity.setVisibility(View.VISIBLE);
                    esp32.applyLamp(onEndpoint);
                    updateLampState(lampId, 1);
                    Toast.makeText(LampController.this, "Turn on lamp " + lampId, Toast.LENGTH_SHORT).show();
                } else {
                    bulbOff.setVisibility(View.VISIBLE);
                    bulbOn.setVisibility(View.GONE);
                    intensity.setVisibility(View.GONE);
                    esp32.applyLamp(offEndpoint);
                    updateLampState(lampId, 0);
                    Toast.makeText(LampController.this, "Turn off lamp " + lampId, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Function for slider to change intensity of the lamp
    private void eventSeekBar(SeekBar seekBar, int lampId, String url) {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int intensityValue = progress;
                // Use try-finally to ensure db is close no matter what happen
                try {
                    sqlDB = myDB.getWritableDatabase();
                    ContentValues cv = new ContentValues();
                    cv.put("intensity", intensityValue);
                    sqlDB.update("lamp", cv, "id = ?", new String[]{String.valueOf(lampId)});
                } finally {
                    sqlDB.close();
                }
                String dynamicUrl = url + intensityValue;
                esp32.applyLamp(dynamicUrl);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do Something
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do Something
            }
        });
    }

    // Function to Set Initial State of Switch Button
    private void getInitialStates(Switch switchButton, ImageView bulbOff, ImageView bulbOn, SeekBar intensity, int lampId) {
        // Use try-finally to ensure db is close no matter what happen
        try {
            String query = "SELECT * FROM lamp WHERE id = ?";
            sqlDB = myDB.getReadableDatabase();
            Cursor cursor = sqlDB.rawQuery(query, new String[]{String.valueOf(lampId)});
            if (cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
                int intensitySavedValue = cursor.getInt(cursor.getColumnIndexOrThrow("intensity"));
                if (status == 0) {
                    switchButton.setChecked(false);
                    bulbOff.setVisibility(View.VISIBLE);
                    bulbOn.setVisibility(View.GONE);
                    intensity.setVisibility(View.GONE);
                    intensity.setProgress(intensitySavedValue);
                } else {
                    switchButton.setChecked(true);
                    bulbOff.setVisibility(View.GONE);
                    bulbOn.setVisibility(View.VISIBLE);
                    intensity.setVisibility(View.VISIBLE);
                    intensity.setProgress(intensitySavedValue);
                }
            }
            cursor.close();
        } finally {
            sqlDB.close();
        }
    }

    // Function to get intensity value from DB
    private int getIntensityValue(int lampId) {
        try {
            String query = "SELECT * FROM lamp WHERE id = ?";
            sqlDB = myDB.getReadableDatabase();
            Cursor cursor = sqlDB.rawQuery(query, new String[] {String.valueOf(lampId)});
            if (cursor.moveToFirst()) {
                int intensityValue = cursor.getInt(cursor.getColumnIndexOrThrow("intensity"));
                return intensityValue;
            }
            cursor.close();
        } finally {
            sqlDB.close();
        }
        return 0;
    }

    // Function to update initial state of each lamp
    private void updateLampState(int lampId, int newStatus) {
        // Use try-finally to ensure db is close no matter what happen
        try {
            // Open The Database for Writing
            sqlDB = myDB.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("status", newStatus);
            sqlDB.update("lamp", cv, "id = ?", new String[]{String.valueOf(lampId)});
        } finally {
            sqlDB.close();
        }
    }
}
