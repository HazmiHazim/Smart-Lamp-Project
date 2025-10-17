package com.iot.android.smartlamp.util

import android.view.View

object ScreenUtils {

    fun resizeImage(view: View, sizeDp: Int) {
        val scale = view.context.resources.displayMetrics.density
        val sizeInPx = (sizeDp * scale + 0.5f).toInt()
        val params = view.layoutParams
        params.width = sizeInPx
        params.height = sizeInPx
        view.layoutParams = params
    }
}