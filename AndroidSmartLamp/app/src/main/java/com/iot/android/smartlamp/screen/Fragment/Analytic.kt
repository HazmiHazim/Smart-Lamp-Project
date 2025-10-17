package com.iot.android.smartlamp.screen.Fragment

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.iot.android.smartlamp.R

class Analytic : Fragment(R.layout.analytic) {

    private lateinit var swipeRefresh : SwipeRefreshLayout
    private lateinit var lineChart : LineChart
    private lateinit var pieChart : PieChart
    private lateinit var barChart : BarChart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.analytic_swipe_refresh)
        lineChart = view.findViewById(R.id.analytic_line_chart)
        pieChart = view.findViewById(R.id.analytic_pie_chart)
        barChart = view.findViewById(R.id.analytic_bar_chart)

        setupDummyLineChart()
        setupDummyPieChart()
        setupDummyBarChart()
    }

    private fun setupDummyLineChart() {
        // Dummy data points
        val entries = listOf(
            Entry(1f, 10f),
            Entry(2f, 14f),
            Entry(3f, 9f),
            Entry(4f, 18f),
            Entry(5f, 13f)
        )

        // Create a dataset
        val dataSet = LineDataSet(entries, "Weekly Usage").apply {
            color = Color.BLUE
            valueTextColor = Color.BLACK
            lineWidth = 2f
            circleRadius = 4f
            setCircleColor(Color.BLUE)
            mode = LineDataSet.Mode.CUBIC_BEZIER

            // enable gradient fill
            setDrawFilled(true)
            fillColor = Color.BLUE
            // optional: remove data point labels if you want a clean look
            setDrawValues(false)
        }

        // Set chart data
        lineChart.data = LineData(dataSet)

        // Clean look: no gridlines, borders, or legend
        lineChart.apply {
            setDrawGridBackground(false)
            setDrawBorders(false)
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.BLACK
                granularity = 1f
            }

            axisLeft.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.BLACK
            }

            animateX(1000)
            invalidate()
        }
    }

    private fun setupDummyPieChart() {
        // Dummy data entries
        val entries = listOf(
            PieEntry(40f, "Living Room"),
            PieEntry(25f, "Bedroom"),
            PieEntry(20f, "Kitchen"),
            PieEntry(15f, "Outdoor")
        )

        val dataSet = PieDataSet(entries, "Lamp Usage by Room").apply {
            // Color set
            colors = listOf(
                Color.parseColor("#2196F3"), // Blue
                Color.parseColor("#4CAF50"), // Green
                Color.parseColor("#FFC107"), // Amber
                Color.parseColor("#F44336")  // Red
            )

            valueTextColor = Color.WHITE
            valueTextSize = 12f
            sliceSpace = 3f
            selectionShift = 5f
        }

        val pieData = PieData(dataSet)

        pieChart.apply {
            data = pieData
            description.isEnabled = false
            legend.isEnabled = false

            // Chart hole (center circle)
            isDrawHoleEnabled = true
            holeRadius = 40f
            setHoleColor(Color.WHITE)

            // Transparency and animation
            setTransparentCircleRadius(45f)
            setTransparentCircleAlpha(50)
            animateY(1000)

            // Clean look
            setDrawEntryLabels(true)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(11f)

            invalidate() // refresh
        }
    }

    private fun setupDummyBarChart() {
        // Dummy data
        val entries = listOf(
            BarEntry(0f, 10f),
            BarEntry(1f, 20f),
            BarEntry(2f, 15f),
            BarEntry(3f, 25f),
            BarEntry(4f, 18f),
            BarEntry(5f, 30f),
            BarEntry(6f, 12f)
        )

        val dataSet = BarDataSet(entries, "Weekly Sales")
        dataSet.color = Color.BLUE
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        barData.barWidth = 0.9f

        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        barChart.apply {
            data = barData
            description.isEnabled = false
            setFitBars(true)
            animateY(1000)

            // X-axis setup
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                valueFormatter = IndexAxisValueFormatter(days)
                granularity = 1f
                labelCount = days.size
            }

            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            legend.isEnabled = true

            invalidate() // Refresh chart
        }
    }

}