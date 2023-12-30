package com.iot.smart_lighting;

import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.iot.smart_lighting.Adapter.TimerAdapter;
import com.iot.smart_lighting.Model.SmartLampDB;

import java.util.ArrayList;
import java.util.Calendar;

public class Timer extends AppCompatActivity {

    // Variable Declaration
    ImageView back, info, noTimerData;
    LinearLayout lampNavTimer, timerLamp1, timerLamp2, timerLamp3;
    LinearLayout timerLampArr[];
    FloatingActionButton addTimer;
    RecyclerView recyclerView;
    SmartLampDB myDB;
    SQLiteDatabase sqlDB;
    TimerAdapter adapter;
    View selector1, selector2, selector3;
    View selectorArr[];

    private String timeChoose;

    private int selectedIndex = 0;

    // Initialize Arraylist Globally
    ArrayList<String> time = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timer);

        back = findViewById(R.id.back_btn2);
        info = findViewById(R.id.info_btn3);
        noTimerData = findViewById(R.id.noTimerFound);

        // Assign XML to variable for navigation timer
        lampNavTimer = findViewById(R.id.lampNavigationTimer);
        timerLamp1 = findViewById(R.id.lNav1);
        timerLamp2 = findViewById(R.id.lNav2);
        timerLamp3 = findViewById(R.id.lNav3);

        // Assign XML to variable for floating action button
        addTimer = findViewById(R.id.addTimerBtn);

        // Assign XML to variable for List of timer data
        recyclerView = findViewById(R.id.recyclerViewTimer);

        // Assign XML to variable for current navigation pointer
        selector1 = findViewById(R.id.lNavSelector1);
        selector2 = findViewById(R.id.lNavSelector2);
        selector3 = findViewById(R.id.lNavSelector3);

        // Create instance for SmartLampDB
        myDB = new SmartLampDB(Timer.this);

        // Attach TimerAdapter to this class
        adapter = new TimerAdapter(Timer.this, time);
        recyclerView.setLayoutManager(new LinearLayoutManager(Timer.this));
        recyclerView.setAdapter(adapter);

        // Add all timerLamp LinearLayouts to the array
        timerLampArr = new LinearLayout[] {timerLamp1, timerLamp2, timerLamp3};

        // Add all selectors to the array
        selectorArr = new View[] {selector1, selector2, selector3};

        // Set default navigation lamp page to lamp 1 when opening the page
        selectNavigationLamp(0);

        // Info dialog box shown when click for user guide commands
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder infoBox = new AlertDialog.Builder(Timer.this);
                infoBox.setIcon(R.drawable.phoenix);
                infoBox.setTitle("Timer Commands");
                infoBox.setMessage("WARNING: ENSURE THAT THE LAMP IS ON TO USE THIS FEATURE\n" +
                        "\nSupported Commands:" +
                        "\nSet Timer <1 to 60> <Seconds|Minutes|Hours> for Lamp <1|2|3>");
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

        // Click event on which lamp is used
        for (int i = 0; i < timerLampArr.length; i++) {
            final int index = i;
            timerLampArr[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectNavigationLamp(index);
                    adapter.setSelectedIndex(index);
                }
            });
        }

        // Make data deleted by swiping left or right
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    String timer = time.get(position);
                    deleteTimer(selectedIndex + 1, timer);
                    time.remove(timer);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(Timer.this, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                }
                if (time.isEmpty()) {
                    noTimerData.setVisibility(View.VISIBLE);
                }
            }
        }).attachToRecyclerView(recyclerView);

        // Event when click floating action button
        addTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get current time
                Calendar calendar = Calendar.getInstance();
                int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
                int currentMinutes = calendar.get(Calendar.MINUTE);

                // Create time picker (spinner) in dialog box
                TimePickerDialog timePickerSpinner = new TimePickerDialog(Timer.this, R.style.CustomTimePickerDialog,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int hours, int minutes) {
                                timeChoose = checkDigit(hours) + " : " + checkDigit(minutes);
                                noTimerData.setVisibility(View.GONE);
                                time.add(timeChoose);
                                // Save to SQL database
                                createTimer(timeChoose, 1, selectedIndex + 1);
                                adapter.setSelectedIndex(selectedIndex);
                                adapter.setTimeChoose(timeChoose);
                                Toast.makeText(Timer.this, "Set Time: " + timeChoose, Toast.LENGTH_SHORT).show();
                            }
                        }, currentHour, currentMinutes, true);

                timePickerSpinner.setButton(DialogInterface.BUTTON_POSITIVE, "Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Nothing
                    }
                });

                timePickerSpinner.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogTimer, int i) {
                        dialogTimer.dismiss();
                    }
                });
                timePickerSpinner.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                timePickerSpinner.setTitle("Set Timer");
                timePickerSpinner.setCancelable(true);
                timePickerSpinner.show();
            }
        });
    }

    // Store value 0 for timepicker
    private String checkDigit(int timeDigit) {
        return timeDigit <= 9 ? "0" + timeDigit : String.valueOf(timeDigit);
    }

    // Method to handle the navigation page
    private void selectNavigationLamp(int index) {
        if (index != selectedIndex) {
            selectorArr[selectedIndex].setVisibility(View.GONE); // Hide the current selector
            selectorArr[index].setVisibility(View.VISIBLE); // Show the selector for the selected page
            selectedIndex = index; // Update the selected index

            // Perform any other actions or updates based on the selected index
            // For example, update the content displayed on the screen
        }
        // Calling function getTime() to get the time stored in DB
        getTimer(selectedIndex + 1);
    }

    // Get all timer data stored in SQLite
    private void getTimer(int lampId) {
        // Use try-finally to ensure db is close no matter what happen
        try {
            String query = "SELECT * FROM lampTimer WHERE lamp_id = ?";
            sqlDB = myDB.getReadableDatabase();
            Cursor cursor = sqlDB.rawQuery(query, new String[] {String.valueOf(lampId)});
            // Clear the existing timer list before adding new data
            time.clear();
            if (cursor.moveToFirst()) {
                do {
                    // Add each timer data to the list
                    time.add(cursor.getString(cursor.getColumnIndexOrThrow("time")));
                } while (cursor.moveToNext());
                noTimerData.setVisibility(View.GONE);
            }
            else {
                noTimerData.setVisibility(View.VISIBLE);
            }
            adapter.notifyDataSetChanged();
            cursor.close();
        }
        finally {
            sqlDB.close();
        }
    }

    // Create lamp timer data in SQLite
    private void createTimer(String timeChoose, int status, int lampId) {
        // Use try-finally to ensure db is close no matter what happen
        try {
            sqlDB = myDB.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("time", timeChoose);
            cv.put("status", status);
            cv.put("lamp_id", lampId);
            sqlDB.insert("lampTimer", null, cv);
        }
        finally {
            sqlDB.close();
        }
    }

    // Function to delete the selected timer from DB
    private void deleteTimer(int lampId, String timer) {
        // Use try-finally to ensure db is close no matter what happen
        try {
            sqlDB = myDB.getWritableDatabase();
            sqlDB.delete("lampTimer", "lamp_id = ? AND time = ?", new String[] {String.valueOf(lampId), timer});
        }
        finally {
            sqlDB.close();
        }
    }
}