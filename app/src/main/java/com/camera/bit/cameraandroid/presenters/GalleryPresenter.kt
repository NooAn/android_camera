package com.camera.bit.cameraandroid.presenters

import com.camera.bit.cameraandroid.view.GalleryFragmentView
import android.content.Intent
import com.camera.bit.cameraandroid.ImageRepository
import java.net.URI


class GalleryPresenter(private val repository: ImageRepository) : BasePresenter<GalleryFragmentView>() {

    fun sharePhote(uriToImage: URI) {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM, uriToImage)
        shareIntent.type = "image/jpeg"
        view?.share(shareIntent)
    }

    fun getAllImages() {
        view?.setItems(list = repository.getAllImages())
    }
}