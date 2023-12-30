package com.iot.smart_lighting;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.iot.smart_lighting.Model.SmartLampDB;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.OpacityBar;
import com.larswerkman.holocolorpicker.SVBar;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

public class ColourEditor extends AppCompatActivity {

    // Variable Declaration
    ImageView back, info;
    LinearLayout lampNavColour, colourLamp1, colourLamp2, colourLamp3;
    LinearLayout colourLampArr[];
    View selector1, selector2, selector3;
    ColorPicker colourWheel;
    SaturationBar saturation;
    ValueBar luminance;
    SmartLampDB myDB;
    SQLiteDatabase sqlDB;
    Esp32 esp32;
    View selectorArr[];

    private int selectedIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.colour_editor);

        // Assign XML to variable for button
        back = findViewById(R.id.back_btn3);
        info = findViewById(R.id.info_btn4);

        // Assign XML to variable for navigation timer
        lampNavColour = findViewById(R.id.lampNavigationColour);
        colourLamp1 = findViewById(R.id.lNav4);
        colourLamp2 = findViewById(R.id.lNav5);
        colourLamp3 = findViewById(R.id.lNav6);

        // Assign XML to variable for current navigation pointer
        selector1 = findViewById(R.id.lNavSelector4);
        selector2 = findViewById(R.id.lNavSelector5);
        selector3 = findViewById(R.id.lNavSelector6);

        // Assign XML to variable for colour picker
        colourWheel = findViewById(R.id.colourWheelPicker);
        saturation = findViewById(R.id.saturationBar);
        luminance = findViewById(R.id.valueBar);

        // Create instance for SmartLamp DB
        myDB = new SmartLampDB(ColourEditor.this);

        // Instantiate ESP32 Class
        esp32 = new Esp32(ColourEditor.this);

        // Add all colourLamp LinearLayouts to the array
        colourLampArr = new LinearLayout[] {colourLamp1, colourLamp2, colourLamp3};

        // Add all selectors View to the array
        selectorArr = new View[] {selector1, selector2, selector3};

        // Make bar function colour changing with colour wheel
        colourWheel.addSaturationBar(saturation);
        colourWheel.addValueBar(luminance);

        // Disable the show old colour
        colourWheel.setShowOldCenterColor(false);

        // Set default navigation lamp page to lamp 1 when opening the page
        selectNavigationLamp(0);

        // Click event on which lamp is used
        for (int i = 0; i < colourLampArr.length; i++) {
            final int index = i;
            colourLampArr[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectNavigationLamp(index);
                }
            });
        }

        // Info dialog box shown when click for user guide commands
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder infoBox = new AlertDialog.Builder(ColourEditor.this);
                infoBox.setIcon(R.drawable.phoenix);
                infoBox.setTitle("Colour Commands");
                infoBox.setMessage("WARNING: ENSURE THAT THE LAMP IS ON TO USE THIS FEATURE\n" +
                        "\nSupported Commands:" +
                        "\nSet <Red|Green|Blue|Yellow|Indigo|Lavender> Colour for Lamp <1|2|3>");
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

        // Event for change colour
        colourWheel.setOnColorChangedListener(new ColorPicker.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                int lamp = selectedIndex + 1;

                // Get Colour in Hex String
                String hexColour = String.format("#%06X", (0xFFFFFF & color));

                // Get Colour in RGB
                int red = Color.red(color);
                int green = Color.green(color);
                int blue = Color.blue(color);

                esp32.applyLamp("http://192.168.4.1/lamp" + lamp + "/colour?red=" + red + "&green=" + green + "&blue=" + blue);
                updateColour(lamp, hexColour, 1);
            }
        });
    }

    // Function to handle the navigation page
    private void selectNavigationLamp(int index) {
        if (index != selectedIndex) {
            selectorArr[selectedIndex].setVisibility(View.GONE); // Hide the current selector
            selectorArr[index].setVisibility(View.VISIBLE); // Show the selector for the selected page
            selectedIndex = index; // Update the selected index

            // Perform any other actions or updates based on the selected index
            // For example, update the content displayed on the screen
        }
        // Calling function getColour() to get the colour stored in DB
        String colour = createOrGetColour(selectedIndex + 1);
        colourWheel.setColor(Color.parseColor(colour));
    }

    // Get all colour data stored in SQLite
    private String createOrGetColour(int lampId) {
        // Use try-finally to ensure db is close no matter what happen
        try {
            String query = "SELECT * FROM lampColour WHERE lamp_id = ?";
            // Open The Database for Reading
            sqlDB = myDB.getReadableDatabase();
            Cursor cursor = sqlDB.rawQuery(query, new String[] {String.valueOf(lampId)});
            if (cursor != null && cursor.moveToFirst()) {
                String colour = cursor.getString(cursor.getColumnIndexOrThrow("colour"));
                cursor.close();
                return colour;
            }
            else {
                // Colour for Lamp 1
                createData(1);
                // Colour for Lamp 2
                createData(2);
                // Colour for Lamp 3
                createData(3);
            }
        } finally {
            sqlDB.close();
        }
        return "#FFFFFF";
    }

    // Create a default data in table lampColour in SQLite
    private void createData(int lampId) {
        // Use try-finally to ensure db is close no matter what happen
        try {
            sqlDB = myDB.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("colour", "#FFFFFF");
            cv.put("lamp_id", lampId);
            sqlDB.insert("lampColour", null, cv);
        } finally {
            sqlDB.close();
        }
    }

    // Update colour based on lamp_id selected
    private void updateColour(int lampId, String colour, int status) {
        // Use try-finally to ensure db is close no matter what happen
        try {
            // Open The Database for Writing
            sqlDB = myDB.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("colour", colour);
            sqlDB.update("lampColour", cv, "id = ?", new String[] {String.valueOf(lampId)});

            // Update status in lamp table
            ContentValues cvStatus = new ContentValues();
            cvStatus.put("status", status);
            sqlDB.update("lamp", cvStatus, "id = ?", new String[] {String.valueOf(lampId)});
        } finally {
            sqlDB.close();
        }
    }
}
