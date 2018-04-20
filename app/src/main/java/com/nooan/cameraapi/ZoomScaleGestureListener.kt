package com.nooan.cameraapi

import android.os.Bundle
import android.text.TextUtils
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.widget.TextView
import java.util.Locale

/** API 1 zoom controller. Changes the zoom in response to scale events.  */
class ZoomScaleGestureListener
/**
 * @param label the TextView to update as the zoom changes
 * @param stateKey the bundle key with which to store/retrieve the state
 */
internal constructor(
        private val controller: Camera1Controller,
        private val label: TextView,
        private val stateKey: String) : SimpleOnScaleGestureListener() {
    private var maxZoom: Int = 0
    private var zoomRatios: IntArray? = null
    var zoom: Int = 0
        private set
    private var startingSpan: Float = 0.toFloat()
    private var intermediateZoomLevel: Int = 0

    init {
        if (TextUtils.isEmpty(stateKey)) {
            throw IllegalArgumentException("A non-empty state key is required")
        }
    }

    /** Reset the state when swapping cameras.  */
    fun reset() {
        zoom = DEFAULT_ZOOM
    }

    /** Must be called while the camera is acquired.  */
    fun initZoomParameters() {
        maxZoom = controller.maxZoom
        zoomRatios = controller.zoomRatios
    }

    fun restoreInstanceState(savedInstanceState: Bundle?) {
        zoom = savedInstanceState?.getInt(stateKey, DEFAULT_ZOOM) ?: DEFAULT_ZOOM
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putInt(stateKey, zoom)
    }

    private fun formatZoomLabel(zoomLevel: Int): String {
        return String.format(Locale.US, "x%.2f", zoomRatios!![zoomLevel].toDouble() / 100)
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        label.text = formatZoomLabel(zoom)
        label.visibility = View.VISIBLE
        startingSpan = detector.currentSpan
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        val currentSpan = detector.currentSpan
        val distanceChange = (currentSpan - startingSpan).toDouble()
        val zoomLevelChange = distanceChange / DP_PER_ZOOM_INTERVAL

        // Clamp the zoom level to valid intervals.
        intermediateZoomLevel = Math.min(
                Math.max(Math.round(zoom + zoomLevelChange).toInt(), 0),
                maxZoom)
        controller.setZoom(intermediateZoomLevel)
        label.text = formatZoomLabel(intermediateZoomLevel)

        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        zoom = intermediateZoomLevel
        label.visibility = View.GONE
    }

    companion object {

        // Adjusts the sensitivity of the gesture.
        private val DP_PER_ZOOM_INTERVAL = 8
        private val DEFAULT_ZOOM = 0
    }
}