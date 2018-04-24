package com.camera.bit.cameraandroid

abstract class TrackedGraphic<T>(overlay: GraphicOverlay) : GraphicOverlay.Graphic(overlay) {
    var id: Int = 0

    internal abstract fun updateItem(item: T)
}