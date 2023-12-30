package com.iot.smart_lighting.Adapter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.iot.smart_lighting.Esp32;
import com.iot.smart_lighting.Model.SmartLampDB;
import com.iot.smart_lighting.R;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimerAdapter extends RecyclerView.Adapter<TimerAdapter.TimerHolder> {

    private Context context;
    private ArrayList time;
    private int selectedIndex;
    SmartLampDB myDB;
    SQLiteDatabase sqlDB;

    // Initialize countdown timer
    CountDownTimer countDownTimer;

    // Initialize ESP32 Class
    Esp32 esp32;
    private String timeChoose;

    public TimerAdapter(Context context, ArrayList time) {
        this.context = context;
        this.time = time;
        this.esp32 = new Esp32(context);
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
        notifyDataSetChanged();
    }

    public void setTimeChoose(String timeChoose) {
        this.timeChoose = timeChoose;
    }

    @Override
    public TimerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.timer_list_item, parent, false);
        return new TimerHolder(view);
    }

    @Override
    public void onBindViewHolder(TimerHolder holder, int position) {
        holder.timeName.setText(String.valueOf(time.get(position)));
        int lampId = selectedIndex + 1;
        String timer = String.valueOf(time.get(position));
        myDB = new SmartLampDB(context);

        // Perform a background task using ExecutorService without blocking the main UI
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            // Perform a background task
            int status = 0;
            // Use try-finally to ensure db is close no matter what happen
            // Don't close the DB in multithreading task because it can lead to error "Connection pool has been closed"
            // If using single-threading then it is fine to close the DB
            try {
                String query = "SELECT * FROM lampTimer WHERE lamp_id = ? AND time = ?";
                sqlDB = myDB.getReadableDatabase();
                Cursor cursor = sqlDB.rawQuery(query, new String[]{String.valueOf(lampId), timer});
                if (cursor.moveToFirst()) {
                    status = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
                }
                cursor.close();
            } finally {
                //sqlDB.close();
            }
            int finalStatus = status;
            handler.post(() -> {
                // UI Thread Task
                holder.switchTimer.setChecked(finalStatus == 1);
                if (finalStatus == 1) {
                    holder.timeName.setTextColor(Color.parseColor("#6A0DAD"));
                    startCountdown(lampId);
                } else {
                    holder.timeName.setTextColor(Color.parseColor("#D9D9D9"));
                    if (lampId == 1) {
                        updateLampState(lampId, 0);
                    }
                    else if (lampId == 2) {
                        updateLampState(lampId, 0);
                    }
                    else if (lampId == 3) {
                        updateLampState(lampId, 0);
                    }
                }
            });
            holder.switchTimer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton switchButton, boolean isChecked) {
                    if (isChecked) {
                        startCountdown(lampId);
                        updateSwitchState(lampId, timer, 1);
                        holder.timeName.setTextColor(Color.parseColor("#6A0DAD"));
                    } else {
                        countDownTimer.cancel();
                        esp32.applyLamp("http://192.168.4.1/timer?stop");
                        updateSwitchState(lampId, timer, 0);
                        holder.timeName.setTextColor(Color.parseColor("#D9D9D9"));
                    }
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return time.size();
    }

    public class TimerHolder extends RecyclerView.ViewHolder {

        TextView timeName;
        Switch switchTimer;

        public TimerHolder(View itemView) {
            super(itemView);

            timeName = itemView.findViewById(R.id.lampName);
            switchTimer = itemView.findViewById(R.id.timerSwitchBtn);
        }
    }

    // Function to start countdown based on time set
    private void startCountdown(int lampId) {
        String timeSet = timeChoose;
        long timeTobeCountdown = 0;
        try {
            if (timeChoose != null) {
                // Remove spaces from the timeChoose string
                timeChoose = timeChoose.replaceAll("\\s+", "");
                // Parse the timeChoose to LocalTime
                LocalTime getTimeChoose = LocalTime.parse(timeChoose);
                // Get current time
                LocalTime currentTime = LocalTime.now();

                // Check if chosen time is earlier than current time
                if (getTimeChoose.isBefore(currentTime)) {
                    // Calculate the time difference
                    timeTobeCountdown = Duration.between(getTimeChoose, currentTime).toMillis();
                    // Add 1 day in milliseconds
                    timeTobeCountdown = timeTobeCountdown + 86400000;
                    System.out.println("Time Count When Less: " + timeTobeCountdown);
                }
                else {
                    // Calculate the time difference
                    timeTobeCountdown = Duration.between(currentTime, getTimeChoose).toMillis();
                    System.out.println("Time Count When More: " + timeTobeCountdown);
                }

                countDownTimer = new CountDownTimer(timeTobeCountdown, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        // Do Something
                    }

                    @Override
                    public void onFinish() {
                        updateSwitchState(lampId, timeSet, 0);
                        updateLampState(lampId, 0);
                        notifyDataSetChanged();
                    }
                }.start();
                // Use the timeToBeCountdown value directly in applyTimer and convert from millis to seconds
                applyTimer(lampId, timeTobeCountdown / 1000);
            }
        } catch (DateTimeException exception) {
            exception.printStackTrace();
        }
    }

    // Function to set timer to the lamp
    private void applyTimer(int lampId, long timer) {
        if (lampId == 1) {
            esp32.applyLamp("http://192.168.4.1/lamp1?timer=" + timer);
            updateLampState(lampId, 1);
        } else if (lampId == 2) {
            esp32.applyLamp("http://192.168.4.1/lamp2?timer=" + timer);
            updateLampState(lampId, 1);
        } else {
            esp32.applyLamp("http://192.168.4.1/lamp3?timer=" + timer);
            updateLampState(lampId, 1);
        }
    }

    // Function to update switch status for each lamp_id and for specific time in database
    private void updateSwitchState(int lampId, String timer, int newStatus) {
        // Use try-finally to ensure db is close no matter what happen
        // Don't close the DB in multithreading task because it can lead to error "Connection pool has been closed"
        // If using single-threading then it is fine to close the DB
        try {
            sqlDB = myDB.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("status", newStatus);
            sqlDB.update("lampTimer", cv, "lamp_id = ? AND time = ?", new String[]{String.valueOf(lampId), timer});
        } finally {
            //sqlDB.close();
        }
    }

    // Function to update initial state of each lamp
    private void updateLampState(int lampId, int newStatus) {
        // Use try-finally to ensure db is close no matter what happen
        try {
            // Open The Database for Reading
            sqlDB = myDB.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("status", newStatus);
            sqlDB.update("lamp", cv, "id = ?", new String[]{String.valueOf(lampId)});
        } finally {
            //sqlDB.close();
        }
    }
}
