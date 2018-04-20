package com.nooan.cameraapi

import android.hardware.Camera
import android.hardware.Camera.AutoFocusCallback
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.FaceDetectionListener
import android.hardware.Camera.Parameters
import android.hardware.Camera.Size
import android.media.FaceDetector
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import java.io.IOException

/**
 * Manages the state of an API 1 camera.
 */
internal class Camera1Controller(private val captureButton: View?) {
    private var camera: Camera? = null
    private var state = STATE_NOT_ACQUIRED
    private var cameraId: Int = 0

    val maxZoom: Int
        get() {
            assertNotState(STATE_NOT_ACQUIRED, "Camera must be acquired before querying zoom")
            return camera!!.parameters.maxZoom
        }

    val zoomRatios: IntArray
        get() {
            assertNotState(STATE_NOT_ACQUIRED, "Camera must be acquired before querying zoom")
            val ratios = camera!!.parameters.zoomRatios
            val zoomRatios = IntArray(ratios.size)
            val it = ratios.iterator()
            for (i in ratios.indices) {
                zoomRatios[i] = it.next()
            }
            return zoomRatios
        }

    /** Returns true if the camera is currently acquired.  */
    val isAcquired: Boolean
        get() = state != STATE_NOT_ACQUIRED

    /** Returns true if the preview is active.  */
    val isPreviewActive: Boolean
        get() = state == STATE_PREVIEW

    val supportedPictureSizes: Array<android.util.Size?>
        get() {
            assertNotState(STATE_NOT_ACQUIRED, "A camera must be acquired before fetching parameters")
            val sizes = camera!!.parameters.supportedPictureSizes
            val supportedSizes = arrayOfNulls<android.util.Size>(sizes.size)
            for (i in sizes.indices) {
                val s = sizes[i]
                supportedSizes[i] = android.util.Size(s.width, s.height)
            }
            return supportedSizes
        }

    interface CaptureCallback {

        /**
         * Called when the capture is complete. Parameters are passed through from
         * Camera.PictureCallback#onPictureTaken. The metrics recorder hosts diagnostic information
         * from the capture process.
         */
        fun onPictureTaken(bytes: ByteArray, camera: Camera)
    }

    init {
        moveToState(STATE_NOT_ACQUIRED)
    }

    // ===============================================================================================
    // Configuration
    // ===============================================================================================

    /**
     * Configures camera parameters common to all configurations.
     * Must be called before preview started.
     */
    fun setDefaultParameters(displayRotation: Int) {
        Log.i(TAG, "setDefaultParameters")
        assertState(STATE_ACQUIRED,
                "Default parameters may only be set before a preview is started")

        val info = CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        val lensFacingFront = info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT

        val previewOrientationDegrees = Orientation.getPreviewOrientation(lensFacingFront, displayRotation, info.orientation)
        camera!!.setDisplayOrientation(previewOrientationDegrees)
        val params = camera!!.parameters

        // We happen to know the preview sizes available for Pixel 2.
        params.setPreviewSize(Utils.MAX_PREVIEW_WIDTH, Utils.MAX_PREVIEW_HEIGHT)
        params.setRotation(
                Orientation.getOutputOrientation(lensFacingFront, displayRotation, info.orientation))

        // Continuous picture is not supported Pixel 2's front camera.
        val supportFocusModes = params.supportedFocusModes
        if (supportFocusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            Log.i(TAG, "setting continuous picture focus mode")
            params.focusMode = Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        }

        // HDR+: Flash mode must be off.
        params.flashMode = Parameters.FLASH_MODE_OFF

        // HDR+: Color effect must be none.
        params.colorEffect = Parameters.EFFECT_NONE

        // HDR+: White balance must be auto.
        params.whiteBalance = Parameters.WHITE_BALANCE_AUTO

        camera!!.parameters = params
    }

    /** Set the preview display before the preview is started.  */
    @Throws(IOException::class)
    fun setPreviewDisplay(previewDisplay: SurfaceHolder) {
        assertState(STATE_ACQUIRED, "SurfaceHolder may only be set prior to preview")
        camera!!.setPreviewDisplay(previewDisplay)
    }

    // ===============================================================================================
    // Camera Control
    // ===============================================================================================

    /** Acquire the camera. Note, some parameters must be set before starting a preview.  */
    @Throws(IOException::class)
    fun acquireCamera(cameraId: Int) {
        Log.i(TAG, "acquireCamera")
        assertState(STATE_NOT_ACQUIRED, "Attempting to acquire camera while already holding a camera")
        this.cameraId = cameraId
        camera = Camera.open(cameraId)
        if (camera != null) {
            moveToState(STATE_ACQUIRED)
        } else {
            throw IOException("Failed to open camera")
        }
        camera!!.setFaceDetectionListener { faces, camera ->

        }
    }

    /** Starts the preview stream. The camera must be acquired first.  */
    fun startPreview() {
        Log.i(TAG, "startPreview")
        assertState(STATE_ACQUIRED, "Preview may only be started when camera is acquired")
        camera!!.startPreview()
        moveToState(STATE_PREVIEW)
    }

    /** Stops the preview stream.  */
    fun stopPreview() {
        Log.i(TAG, "stopPreview")
        assertNotState(STATE_NOT_ACQUIRED, "Preview may only be started when camera is acquired")
        camera!!.stopPreview()
        moveToState(STATE_ACQUIRED)
    }

    /**
     * Initiate still image capture. Acquire focus lock if in auto-focus mode.
     * The camera internally transitions to acquired after a capture is complete,
     * the preview is not automatically restarted. The shutterCallback must
     * call #captureComplete before restarting the preview.
     */
    fun takePicture(captureCallback: CaptureCallback) {
        Log.i(TAG, "takePicture")
        assertState(STATE_PREVIEW, "Preview must be started before taking a picture")
        

        camera!!.takePicture(null, null) { bytes, cameraId ->
            Log.d(TAG, "takePicture: callback started")
            captureCallback.onPictureTaken(bytes, cameraId)
            captureComplete()
            Log.d(TAG, "takePicture: callback complete")
        }
        moveToState(STATE_CAPTURE)
    }

    /** Release the camera.  */
    fun releaseCamera() {
        Log.i(TAG, "releaseCamera")
        assertNotState(STATE_NOT_ACQUIRED, "Attempting to release camera while not holding a camera")
        camera!!.release()
        camera = null
        moveToState(STATE_NOT_ACQUIRED)
    }

    /**
     * Call this from the shutter callback after capturing an image and
     * before restarting the preview.
     */
    private fun captureComplete() {
        Log.d(TAG, "captureComplete")
        moveToState(STATE_ACQUIRED)
    }

    private fun startFaceDetection() {
        Log.i(TAG, "focus: face detection started")
        camera!!.startFaceDetection()
    }

    fun setZoom(level: Int) {
        assertNotState(STATE_NOT_ACQUIRED, "Camera must be acquired before modifying zoom")
        Log.d(TAG, "setZoom($level)")
        val params = camera!!.parameters
        params.zoom = level
        camera!!.parameters = params
    }

    // ===============================================================================================
    // State Management
    // ===============================================================================================

    private fun moveToState(newState: Int) {
        Log.i(TAG, "last state: " + STATE_NAMES[state] + ", new state: " + STATE_NAMES[newState])
        when (newState) {
            STATE_NOT_ACQUIRED -> if (captureButton != null) {
                captureButton.isEnabled = false
            }
            STATE_ACQUIRED -> if (captureButton != null) {
                captureButton.isEnabled = false
            }
            STATE_PREVIEW -> {
                if (captureButton != null) {
                    captureButton.isEnabled = true
                }
                // Face detection is not required for HDR+ shots.
                startFaceDetection()
            }
            STATE_CAPTURE -> if (captureButton != null) {
                captureButton.isEnabled = false
            }
            else -> throw IllegalStateException("unrecognized state: $newState")
        }
        state = newState
    }

    private fun assertState(expectedState: Int, message: String) {
        if (state != expectedState) {
            throw IllegalStateException(
                    String.format("Current state: %d, expected: %d, %s", state, expectedState, message))
        }
    }

    private fun assertNotState(disallowedState: Int, message: String) {
        if (state == disallowedState) {
            throw IllegalStateException(
                    String.format("Current state: %d, %s", state, message))
        }
    }

    fun setPictureSize(size: android.util.Size) {
        Log.i(TAG, String.format("setting picture size (%d, %d)", size.width, size.height))
        assertNotState(STATE_NOT_ACQUIRED, "A camera must be acquired before setting parameters")
        val params = camera!!.parameters
        params.setPictureSize(size.width, size.height)
        camera!!.parameters = params
    }

    companion object {

        private val TAG = "PcvCamCon1"

        private val STATE_NOT_ACQUIRED = 0
        private val STATE_ACQUIRED = 1
        private val STATE_PREVIEW = 2
        private val STATE_CAPTURE = 3
        private val STATE_NAMES = arrayOf("Not acquired", "Acquired", "Preview", "Capture")
    }
}