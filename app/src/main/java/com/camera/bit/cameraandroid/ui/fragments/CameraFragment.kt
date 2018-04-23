package com.camera.bit.cameraandroid.ui.fragments

import android.hardware.Camera
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import com.camera.bit.cameraandroid.CameraView
import com.camera.bit.cameraandroid.R
import com.camera.bit.cameraandroid.getAllImages
import com.camera.bit.cameraandroid.load

class CameraFragment : Fragment() {

    companion object {
        fun newInstance(): CameraFragment {
            return CameraFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.camera_fragment, container, false)
        initCamera(v)
        return v
    }

    var mCamera: Camera? = null
    var mCameraView: CameraView? = null

    private fun initCamera(v: View) {
        try {
            mCamera = Camera.open();//you can use open(int) to use different cameras
        } catch (e: Exception) {
            Log.d("LOG", "Failed to get camera: " + e.message);
        }

        if (mCamera != null) {
            mCameraView = CameraView(activity?.baseContext!!, mCamera!!)
            val camera_view = v.findViewById(R.id.camera_view) as FrameLayout
            camera_view.addView(mCameraView!!);
        }

        //btn to close the application
        val imgClose = v.findViewById<ImageButton>(R.id.imgClose)
        imgClose.setOnClickListener {
            activity?.finish()
        }

        val takePicture = v.findViewById<ImageButton>(R.id.takePicture)
        takePicture.setOnClickListener {
            mCameraView?.takePicture()
        }

        val openGallery = v.findViewById<ImageView>(R.id.gallery)
        openGallery.load(getAllImages(activity?.baseContext!!).last())
        openGallery.setOnClickListener {
            val fragmentTransaction = activity?.supportFragmentManager?.beginTransaction()
            fragmentTransaction?.replace(R.id.main, GalleryFragment.newInstance())?.addToBackStack("tag")
            fragmentTransaction?.commit()
        }
    }
}