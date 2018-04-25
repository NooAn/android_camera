package com.camera.bit.cameraandroid.vision

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.camera.bit.cameraandroid.vision.FaceGraphic.Companion.COLOR_CHOICES
import com.google.android.gms.vision.barcode.Barcode

internal class BarcodeGraphic(overlay: GraphicOverlay, private val function: (a: Barcode) -> Unit) : TrackedGraphic<Barcode>(overlay) {

    private val mRectPaint: Paint
    private val mTextPaint: Paint
    @Volatile
    private var mBarcode: Barcode? = null

    init {

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.size
        val selectedColor = COLOR_CHOICES[mCurrentColorIndex]

        mRectPaint = Paint()
        mRectPaint.color = selectedColor
        mRectPaint.style = Paint.Style.STROKE
        mRectPaint.strokeWidth = 2.0f

        mTextPaint = Paint()
        mTextPaint.color = selectedColor

        mTextPaint.textSize = 36.0f
    }

    override fun updateItem(item: Barcode) {
        mBarcode = item
        function(mBarcode!!)
        postInvalidate()
    }

    override fun draw(canvas: Canvas) {
        val barcode = mBarcode ?: return

        // Draws the bounding box around the barcode.
        val rect = RectF(barcode.boundingBox)
        rect.left = translateX(rect.left)
        rect.top = translateY(rect.top)
        rect.right = translateX(rect.right)
        rect.bottom = translateY(rect.bottom)
        canvas.drawRect(rect, mRectPaint)
        
        // Draws a label at the bottom of the barcode indicate the barcode value that was detected.
        // canvas.drawText(barcode.rawValue, rect.left, rect.bottom, mTextPaint)
    }

    companion object {
        private var mCurrentColorIndex = 0
    }
}