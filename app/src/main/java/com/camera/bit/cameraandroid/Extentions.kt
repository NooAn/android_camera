package com.camera.bit.cameraandroid

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