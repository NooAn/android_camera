package com.camera.bit.cameraandroid.vision

import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.face.Face


internal class BarcodeTrackerFactory(private val mGraphicOverlay: GraphicOverlay,
                                     private val function: (Barcode) -> Unit) : MultiProcessor.Factory<Barcode> {

    override fun create(barcode: Barcode): Tracker<Barcode> =
            GraphicTracker(mGraphicOverlay, BarcodeGraphic(mGraphicOverlay, function))

}

internal class FaceTrackerFactory(private val mGraphicOverlay: GraphicOverlay) : MultiProcessor.Factory<Face> {

    override fun create(face: Face?): Tracker<Face> =
            GraphicTracker(mGraphicOverlay, FaceGraphic(mGraphicOverlay))

}
