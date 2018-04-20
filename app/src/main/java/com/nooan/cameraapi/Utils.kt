package com.nooan.cameraapi

import android.app.ActionBar
import android.app.Activity
import android.view.View

object Utils {

    /** Max preview width that is guaranteed by Camera2 API  */
    val MAX_PREVIEW_WIDTH = 1920

    /** Max preview height that is guaranteed by Camera2 API  */
    val MAX_PREVIEW_HEIGHT = 1080

    fun setSystemUiOptionsForFullscreen(activity: Activity) {
        val decorView = activity.window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                // hide status bar
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        val actionBar = activity.actionBar
        actionBar?.hide()
    }
}