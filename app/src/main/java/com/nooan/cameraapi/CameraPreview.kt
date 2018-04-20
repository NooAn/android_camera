package com.nooan.cameraapi

import android.content.Context
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException


class CameraPreview internal constructor(context: Context, private val controller: Camera1Controller) : SurfaceView(context), SurfaceHolder.Callback {

    private val mHolder: SurfaceHolder
    private var surfaceValid: Boolean = false

    init {

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = holder
        mHolder.addCallback(this)
    }

    /** Stop the preview if it is running. Then start the preview.  */
    fun reset() {
        if (controller.isPreviewActive) {
            try {
                controller.stopPreview()
            } catch (e: Exception) {
                Log.w(TAG, "tried to stopPreview", e)
            }

        }

        if (surfaceValid) {
            try {
                Log.i(TAG, "reset() starting preview")
                controller.setPreviewDisplay(mHolder)
                controller.startPreview()
            } catch (e: Exception) {
                Log.w(TAG, "Error starting camera preview: ", e)
            }

        }
    }

    /**
     * The preview is started on surfaceChanged(), which is always called after surfaceCreated().
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated")
        surfaceValid = true
        try {
            controller.setPreviewDisplay(holder)
        } catch (e: IOException) {
            Log.d(TAG, "Error setting camera preview", e)
        }

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        Log.d(TAG, "surfaceChanged")
        if (mHolder.surface == null) {
            Log.w(TAG, "surfaceChanged, but surface doesn't exist!")
            return
        }

        // Stop the preview if active.
        if (controller.isPreviewActive) {
            try {
                controller.stopPreview()
            } catch (e: Exception) {
                Log.w(TAG, "caught exception stopping preview", e)
            }

        }

        // Start preview with new settings.
        try {
            Log.i(TAG, "surfaceChanged() starting preview")
            controller.startPreview()
        } catch (e: Exception) {
            Log.i(TAG, "Error starting camera preview: ", e)
        }

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed")
        surfaceValid = false
    }

    companion object {

        private val TAG = "PvcCamPreview"
    }
}
