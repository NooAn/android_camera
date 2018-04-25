package com.camera.bit.cameraandroid.view

import android.content.Intent
import java.io.File

interface CameraFragmentView : MvpView {
    fun openWeb(intent: Intent)
    fun addMediaToGallery(path: String)
    fun showBarcode(barcode: String)
    fun showLastPicture(path: File)
}
