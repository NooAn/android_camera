package com.nooan.cameraapi

import android.support.v4.view.ViewCompat.getRotation
import android.hardware.Camera.CameraInfo
import android.widget.ImageButton
import android.support.v4.content.ContextCompat.startActivity
import android.annotation.SuppressLint
import android.widget.FrameLayout
import android.text.method.Touch.onTouchEvent
import android.view.MotionEvent
import android.os.Bundle
import android.widget.Toast
import android.view.ScaleGestureDetector
import android.app.Activity
import android.content.Intent
import android.hardware.Camera
import android.util.Log
import android.view.View
import java.io.IOException
import java.nio.file.FileSystem
import java.util.prefs.Preferences


class CameraApi1Activity : Activity() {

    private var cameraController: Camera1Controller? = null
    private var mPreview: CameraPreview? = null

    private var cameraId: Int = 0
    private var zoomScaleGestureDetector: ScaleGestureDetector? = null
    private var zoomScaleGestureListener: ZoomScaleGestureListener? = null

    // ===============================================================================================
    // Capture Callback
    // ===============================================================================================

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        fun onPictureTaken(bytes: ByteArray, camera: Camera) {
            FileSystem.saveImage(this@CameraApi1Activity, bytes, /*isApi1=*/ true)
            // Post this on the UI thread to allow the controller state machine to complete it's
            // transitions.
            mPreview!!.post({ mPreview!!.reset() })
        }
    }

    /** Handles clicks on the camera selection button.  */
    private val cameraSelectionOnClickListener = object : View.OnClickListener() {

        override fun onClick(view: View) {
            if (cameraController!!.isAcquired) {
                Log.i(TAG, "changing cameras, releasing camera")
                cameraController!!.releaseCamera()
            }

            // Swap camera ids.
            when (cameraId) {
                CameraInfo.CAMERA_FACING_BACK -> cameraId = CameraInfo.CAMERA_FACING_FRONT
                CameraInfo.CAMERA_FACING_FRONT -> cameraId = CameraInfo.CAMERA_FACING_BACK
                else -> {
                    Log.e(TAG, "out of bounds camera id: $cameraId")
                    cameraId = CameraInfo.CAMERA_FACING_BACK
                }
            }
            setCameraIconForCurrentCamera()
            zoomScaleGestureListener!!.reset()

            Log.i(TAG, "restarting with new camera")
            try {
                cameraController!!.acquireCamera(cameraId)
                configureOutputSize()
                cameraController!!.setDefaultParameters(windowManager.defaultDisplay.rotation)
                zoomScaleGestureListener!!.initZoomParameters()
                cameraController!!.setZoom(zoomScaleGestureListener!!.getZoom())
                configurePreview()
            } catch (e: IOException) {
                Log.w(TAG, "failed to acquire camera", e)
            }

        }
    }

    // ===============================================================================================
    // Activity Framework Callbacks
    // ===============================================================================================

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "[onCreate]")
        preferences = Preferences(this)
        setContentView(R.layout.camera1)
        //Utils.setSystemUiOptionsForFullscreen(this)
        val captureButton = findViewById(R.id.button_capture)
        captureButton.setOnClickListener({ v ->
            cameraController!!.takePicture { captureButton, c ->

            }
        })

        cameraController = Camera1Controller(captureButton)

        zoomScaleGestureListener = ZoomScaleGestureListener(
                cameraController, findViewById(R.id.zoom_level_label), STATE_ZOOM)
        zoomScaleGestureListener!!.restoreInstanceState(savedInstanceState)
        zoomScaleGestureDetector = ScaleGestureDetector(this, zoomScaleGestureListener)

        initTopControls()
    }

    /** Configure the system UI elements when we receive focus.  */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d(TAG, "[onWindowFocusChanged] hasFocus = $hasFocus")
        if (hasFocus) {
            // Utils.setSystemUiOptionsForFullscreen(this)
        }
    }

    /** Acquire the camera and start the preview.  */
    public override fun onResume() {
        super.onResume()
        Log.d(TAG, "[onResume]")
        try {
            cameraController!!.acquireCamera(cameraId)
            configureOutputSize()
            cameraController!!.setDefaultParameters(windowManager.defaultDisplay.rotation)
            zoomScaleGestureListener!!.initZoomParameters()
            cameraController!!.setZoom(zoomScaleGestureListener!!.getZoom())
            configurePreview()
        } catch (e: IOException) {
            val errorMessage = "Failed to acquire camera"
            // Toasts.showToast(this, errorMessage, Toast.LENGTH_LONG)
            Log.w(TAG, errorMessage, e)
            finish()
        }

    }

    /** Release the camera.  */
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "[onPause]")
        if (cameraController!!.isAcquired()) {
            cameraController!!.releaseCamera()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        zoomScaleGestureListener!!.saveInstanceState(outState)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return zoomScaleGestureDetector!!.onTouchEvent(event)
    }

    // ===============================================================================================
    // UI Management
    // ===============================================================================================

    private fun configurePreview() {
        if (mPreview == null) {
            val preview = findViewById(R.id.camera_preview)
            mPreview = CameraPreview(this, cameraController)
            preview.addView(mPreview)
        } else {
            mPreview!!.reset()
        }
    }

    // ===============================================================================================
    // Top Controls
    // ===============================================================================================

    private fun initTopControls() {
        initApiSwitch()
        initCameraSelection()
        setCameraIconForCurrentCamera()
        val cameraSelectionButton = findViewById(R.id.control_camera_selection)
        cameraSelectionButton.setOnClickListener(cameraSelectionOnClickListener)
    }

    @SuppressLint("SetTextI18n")
    private fun initApiSwitch() {
        val button = findViewById(R.id.api_selector)
        button.setText("API 1")
        button.setOnClickListener({ v ->
            Log.i(TAG, "switching to API 2")
            finish()
            startActivity(Intents.createApi2Intent())
        })
    }

    /** Initializes cameraId state from global preferences.  */
    private fun initCameraSelection() {
        cameraId = preferences!!.getCameraId()
        if (cameraId > CameraInfo.CAMERA_FACING_FRONT) {
            Log.e(TAG, "out of bounds camera id: $cameraId")
            cameraId = CameraInfo.CAMERA_FACING_BACK
            preferences!!.setCameraId(cameraId)
        }
    }

    private fun setCameraIconForCurrentCamera() {
        val button = findViewById(R.id.control_camera_selection)
        when (cameraId) {
            CameraInfo.CAMERA_FACING_BACK -> button.setImageResource(R.drawable.ic_camera_rear_white_24)
            CameraInfo.CAMERA_FACING_FRONT -> button.setImageResource(R.drawable.ic_camera_front_white_24)
            else -> {
            }
        }
    }

    /** Call this after a camera has been acquired.  */
    private fun configureOutputSize() {
        Log.d(TAG, "configureOutputSize")
        val supportedSizes = cameraController!!.getSupportedPictureSizes()
        cameraController!!.setPictureSize(supportedSizes[0])
    }

    companion object {

        private val TAG = "PvcCamApi1"
        private val STATE_ZOOM = "zoom"
    }
}

/**
 * Convenience methods for starting internal intents.
 */
object Intents {

    private val PACKAGE_NAME = "com.google.android.imaging.pixelvisualcorecamera"

    fun createApi1Intent(): Intent {
        val intent = Intent()
        intent.setClassName(
                PACKAGE_NAME,
                "$PACKAGE_NAME.api1.CameraApi1Activity")
        return intent
    }

    fun createApi2Intent(): Intent {
        val intent = Intent()
        intent.setClassName(
                PACKAGE_NAME,
                "$PACKAGE_NAME.api2.CameraApi2Activity")
        return intent
    }
}