package com.camera.bit.cameraandroid

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face


class FaceTrackerFactory(graphicOverlay: GraphicOverlay) : MultiProcessor.Factory<Face> {
    var mGraphicOverlay: GraphicOverlay = graphicOverlay

    override fun create(face: Face?): Tracker<Face> {
        val graphic = FaceGraphic(mGraphicOverlay)
        return GraphicTracker(mGraphicOverlay, graphic)
    }
}

class GraphicTracker<T>(private val mOverlay: GraphicOverlay, private val mGraphic: TrackedGraphic<T>) : Tracker<T>() {

    /**
     * Start tracking the detected item instance within the item overlay.
     */
    override fun onNewItem(id: Int, item: T?) {
        mGraphic.id = id
    }

    /**
     * Update the position/characteristics of the item within the overlay.
     */
    override fun onUpdate(detectionResults: Detector.Detections<T>?, item: T?) {
        mOverlay.add(mGraphic)
        mGraphic.updateItem(item!!)
    }

    /**
     * Hide the graphic when the corresponding face was not detected.  This can happen for
     * intermediate frames temporarily, for example if the face was momentarily blocked from
     * view.
     */
    override fun onMissing(detectionResults: Detector.Detections<T>?) {
        mOverlay.remove(mGraphic)
    }

    /**
     * Called when the item is assumed to be gone for good. Remove the graphic annotation from
     * the overlay.
     */
    override fun onDone() {
        mOverlay.remove(mGraphic)
    }
}

class FaceGraphic(mGraphicOverlay: GraphicOverlay) : TrackedGraphic<Face>(mGraphicOverlay) {


    companion object {
        val FACE_POSITION_RADIUS = 10.0f
        val ID_TEXT_SIZE = 40.0f
        val ID_Y_OFFSET = 50.0f
        val ID_X_OFFSET = -50.0f
        val BOX_STROKE_WIDTH = 5.0f
        val COLOR_CHOICES = arrayListOf(Color.MAGENTA,
                Color.RED,
                Color.YELLOW)
    }

    var mCurrentColorIndex = 0

    private var mFacePositionPaint: Paint? = null
    private var mIdPaint: Paint? = null
    private var mBoxPaint: Paint? = null
    var mFace: Face? = null

    init {
        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.size
        val selectedColor = COLOR_CHOICES[mCurrentColorIndex]

        mFacePositionPaint = Paint()
        mFacePositionPaint?.color = selectedColor

        mIdPaint = Paint()
        mIdPaint?.color = selectedColor
        mIdPaint?.setTextSize(ID_TEXT_SIZE)

        mBoxPaint = Paint()
        mBoxPaint?.color = selectedColor
        mBoxPaint?.style = Paint.Style.STROKE
        mBoxPaint?.strokeWidth = BOX_STROKE_WIDTH
    }

    override fun updateItem(item: Face) {
        mFace = item
        postInvalidate()
    }

    override fun draw(canvas: Canvas?) {
        val face = mFace ?: return

        // Draws a circle at the position of the detected face, with the face's track id below.
        val cx = translateX(face.position.x + face.width / 2)
        val cy = translateY(face.position.y + face.height / 2)
        canvas?.drawCircle(cx, cy, FACE_POSITION_RADIUS, mFacePositionPaint)
        canvas?.drawText("id: $id", cx + ID_X_OFFSET, cy + ID_Y_OFFSET, mIdPaint)

        // Draws an oval around the face.
//        val xOffset = scaleX(face.width / 2.0f)
//        val yOffset = scaleY(face.height / 2.0f)
//        val left = cx - xOffset
//        val top = cy - yOffset
//        val right = cx + xOffset
//        val bottom = cy + yOffset
//        canvas?.drawOval(left, top, right, bottom, mBoxPaint)

        // Draws a bounding box around the face.
        val xOffset = scaleX(face.width / 2.0f)
        val yOffset = scaleY(face.height / 2.0f)
        val left = cx - xOffset
        val top = cy - yOffset
        val right = cx + xOffset
        val bottom = cy + yOffset
        canvas?.drawRect(left, top, right, bottom, mBoxPaint)

    }
}
