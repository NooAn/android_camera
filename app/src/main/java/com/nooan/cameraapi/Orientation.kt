package com.nooan.cameraapi

import android.util.Log
import android.view.Surface

/**
 * Utility for working with device and camera orientations.
 */
object Orientation {

    private val TAG = "PvcCamOrient"

    /** Returns the camera (jpeg) output orientation.  */
    fun getOutputOrientation(
            lensFacingFront: Boolean, displayRotationCode: Int, sensorOrientationDegrees: Int): Int {

        val degrees = convertRotationToDegrees(displayRotationCode)
        Log.d(TAG, "display rotation = " + degrees + "°, "
                + "camera orientation = " + sensorOrientationDegrees + "°")
        val result: Int
        if (lensFacingFront) {
            result = (sensorOrientationDegrees + degrees) % 360
        } else {
            result = (sensorOrientationDegrees - degrees + 360) % 360
        }

        Log.i(TAG, "output orientation -> $result°")
        return result
    }

    /**
     * Returns the orientation for the camera preview. Only used in api 1.
     * This is managed automatically by api 2.
     */
    fun getPreviewOrientation(
            lensFacingFront: Boolean, displayRotationCode: Int, sensorOrientation: Int): Int {

        val degrees = convertRotationToDegrees(displayRotationCode)
        var result: Int
        if (lensFacingFront) {
            result = (sensorOrientation + degrees) % 360
            result = (360 - result) % 360
        } else {
            result = (sensorOrientation - degrees + 360) % 360
        }

        Log.i(TAG, "preview orientation -> $result°")
        return result
    }

    private fun convertRotationToDegrees(rotationCode: Int): Int {
        val degrees: Int
        when (rotationCode) {
            Surface.ROTATION_0 -> degrees = 0

            Surface.ROTATION_90 -> degrees = 90

            Surface.ROTATION_180 -> degrees = 180

            Surface.ROTATION_270 -> degrees = 270
            else -> throw IllegalStateException("Invalid rotation code: $rotationCode")
        }
        return degrees
    }
}