package com.camera.bit.cameraandroid

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.io.File

fun ImageView.loadImage(file: File) = Glide.with(this)
        .load(file)
        .apply(RequestOptions()
                .placeholder(R.drawable.ic_launcher_foreground)
                .fitCenter())
        .into(this)

fun ImageView.load(file: File) = Glide.with(this)
        .load(file)
        .apply(RequestOptions()
                .placeholder(R.drawable.ic_photo)
                .circleCrop())
        .into(this)

fun Bitmap.rotate(orientation: Float): Bitmap {
    val matrix = Matrix()
    matrix.preScale(-1f, 1f);
    matrix.setRotate(orientation)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, false)
}

fun Uri?.addMediaToGallery(activity: Activity?) {
    this ?: return
    try {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        mediaScanIntent.data = this
        activity?.sendBroadcast(mediaScanIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}