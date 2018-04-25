package com.camera.bit.cameraandroid.vision

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.android.gms.vision.face.Face

class FaceGraphic(mGraphicOverlay: GraphicOverlay) : TrackedGraphic<Face>(mGraphicOverlay) {


    companion object {
        const val FACE_POSITION_RADIUS = 10.0f
        const val ID_TEXT_SIZE = 40.0f
        const val ID_Y_OFFSET = 50.0f
        const val ID_X_OFFSET = -50.0f
        const val BOX_STROKE_WIDTH = 5.0f
        val COLOR_CHOICES = arrayListOf(Color.BLUE,
                Color.CYAN,
                Color.GREEN,
                Color.MAGENTA,
                Color.RED,
                Color.WHITE,
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
        mIdPaint?.textSize = ID_TEXT_SIZE

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
        val x = translateX(face.position.x + face.width / 2)
        val y = translateY(face.position.y + face.height / 2)
        canvas?.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint)
        canvas?.drawText("id: $id", x + ID_X_OFFSET, y + ID_Y_OFFSET, mIdPaint)
        canvas?.drawText("smille: ${String.format("%.2f", face.isSmilingProbability)}", x + ID_X_OFFSET * 2, y + ID_Y_OFFSET * 2, mIdPaint)
        canvas?.drawText("right eye: ${String.format("%.2f", face.isRightEyeOpenProbability)}", x - ID_X_OFFSET * 2, y - ID_Y_OFFSET * 2, mIdPaint)
        canvas?.drawText("left eye: ${String.format("%.2f", face.isLeftEyeOpenProbability)}", x - ID_X_OFFSET * 2, y - ID_Y_OFFSET * 3, mIdPaint)

        // Draws a bounding box around the face.
        val xOffset = scaleX(face.width / 2.0f)
        val yOffset = scaleY(face.height / 2.0f)
        val left = x - xOffset
        val top = y - yOffset
        val right = x + xOffset
        val bottom = y + yOffset
        canvas?.drawRect(left, top, right, bottom, mBoxPaint)

    }
}
