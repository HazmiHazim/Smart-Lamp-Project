package com.iot.smart_lighting;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class DataAnalysis extends AppCompatActivity {

    ImageView back, info;
    LineChart lineChart;

    // Declare ESP32 Class
    Esp32 esp32;

    // Declare Handler and Runnable for running a background thread for auto refresh
    Handler handler;
    Runnable refresh;

    // Create ArrayList to store current data received
    private List<Entry> entries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_analysis);

        back = findViewById(R.id.back_btn4);
        info = findViewById(R.id.info_btn5);
        lineChart = findViewById(R.id.line_chart);

        // Instantiate ESP32 Class
        esp32 = new Esp32(DataAnalysis.this);

        // For auto refresh
        handler = new Handler();

        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder infoBox = new AlertDialog.Builder(DataAnalysis.this);
                infoBox.setIcon(R.drawable.phoenix);
                infoBox.setTitle("Data Analysis Commands");
                infoBox.setMessage("Supported Commands:" +
                        "\nOpen Data Analysis" +
                        "\nShow Data Analysis");
                AlertDialog alertDialog = infoBox.create();
                alertDialog.show();
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // Call function to styling the chart
        styleChart();

        // Auto refresh every 5 seconds
        refresh = new Runnable() {
            @Override
            public void run() {
                esp32.getDataAnalysis("http://192.168.4.1/analysis?current", new Esp32.DataAnalysisCallBack() {
                    @Override
                    public void onSuccess(String response) {
                        double current = Double.parseDouble(response);
                        updateChartUI(current);
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(DataAnalysis.this, "Failed to retrieve data", Toast.LENGTH_SHORT).show();
                    }
                });
                handler.postDelayed(this, 5000); // Refresh every 5 seconds (5000 milliseconds)
            }
        };
        handler.post(refresh);
    }

    // Stop auto refresh if leave the page
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refresh);
    }

    // Function to styling the chart based on preferences
    private void styleChart() {
        // Styling Line Chart
        lineChart.getDescription().setEnabled(false);
        lineChart.setAutoScaleMinMaxEnabled(true);
        lineChart.setBackgroundColor(Color.parseColor("#000000"));

        // Disable X Axis
        XAxis x = lineChart.getXAxis();
        x.setEnabled(false);
        x.setGranularityEnabled(true);
        x.setDrawAxisLine(true);
        x.setTextColor(Color.parseColor("#F5F5F5"));

        // Y Axis on the Left Side
        YAxis yLeft = lineChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setDrawAxisLine(true);
        yLeft.setDrawLabels(true);
        yLeft.setGranularityEnabled(true);
        yLeft.setTextColor(Color.parseColor("#F5F5F5"));

        // Y Axis on the Right Side
        YAxis yRight = lineChart.getAxisRight();
        yRight.setEnabled(false);

        Legend legend = lineChart.getLegend();
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setTextColor(Color.parseColor("#F5F5F5"));
        legend.setForm(Legend.LegendForm.CIRCLE);
    }

    // Function to update LineChart with the given data from ESP32
    private void updateChartUI(double current) {
        // Add data received to the graph
        entries.add(new Entry(entries.size(), (float) current));

        // Run UI update on the main thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LineDataSet dataSet = new LineDataSet(entries, "Current Flow (Ampere)");
                dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                dataSet.setCubicIntensity(0.2f);
                dataSet.setDrawFilled(true);
                dataSet.setFillColor(Color.parseColor("#87CEEB"));
                dataSet.setFillAlpha(1000);
                dataSet.setLineWidth(2);

                LineData lineData = new LineData(dataSet);
                lineChart.setData(lineData);
                lineChart.invalidate();
            }
        });
    }
}
