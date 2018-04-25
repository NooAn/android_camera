package com.camera.bit.cameraandroid.vision

abstract class TrackedGraphic<T>(overlay: GraphicOverlay) : GraphicOverlay.Graphic(overlay) {
    var id: Int = 0

    internal abstract fun updateItem(item: T)
}