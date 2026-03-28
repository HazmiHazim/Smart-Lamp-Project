package com.iot.android.smartlamp.util

import android.content.Context
import android.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iot.android.smartlamp.R

object ColorUtils {

    private var colorMap: Map<String, String>? = null

    fun loadColors(context: Context) {
        if (colorMap != null) return
        val inputStream = context.resources.openRawResource(R.raw.colors)
        val json = inputStream.bufferedReader().use { it.readText() }
        val type = object : TypeToken<Map<String, String>>() {}.type
        colorMap = Gson().fromJson(json, type)
    }

    fun findClosestColorName(red: Int, green: Int, blue: Int): String {
        val map = colorMap ?: return "#${String.format("%02X%02X%02X", red, green, blue)}"

        var closestName = "White"
        var minDistance = Double.MAX_VALUE

        for ((name, hex) in map) {
            val parsed = Color.parseColor(hex)
            val r = Color.red(parsed)
            val g = Color.green(parsed)
            val b = Color.blue(parsed)

            val distance = Math.sqrt(
                ((red - r) * (red - r) +
                 (green - g) * (green - g) +
                 (blue - b) * (blue - b)).toDouble()
            )

            if (distance < minDistance) {
                minDistance = distance
                closestName = name
            }
        }

        return closestName
    }

    fun getRgbFromColorName(name: String): Triple<Int, Int, Int> {
        val hex = colorMap?.get(name) ?: return Triple(255, 255, 255)
        val parsed = Color.parseColor(hex)
        return Triple(Color.red(parsed), Color.green(parsed), Color.blue(parsed))
    }
}
