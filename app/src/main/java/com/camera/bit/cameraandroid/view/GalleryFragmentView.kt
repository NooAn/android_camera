package com.camera.bit.cameraandroid.view

import java.io.File

interface GalleryFragmentView : MvpView {
    fun setItems(list: ArrayList<File>)
}
