package com.camera.bit.cameraandroid.view

import android.content.Intent
import java.io.File

interface GalleryFragmentView : MvpView {
    fun setItems(list: List<File>)
    fun share(url: Intent)
}
