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
import com.camera.bit.cameraandroid.vision.CameraView
import com.camera.bit.cameraandroid.R

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

    private var mCamera: Camera? = null
    var openGallery: ImageView? = null
    var mCameraView: CameraView? = null
    var cameraView: FrameLayout? = null

    private fun initCamera(v: View) {
        cameraView = v.findViewById(R.id.camera_view) as FrameLayout

        try {
            mCamera = Camera.open();//you can use open(int) to use different cameras
            initCameraOpen()
        } catch (e: Exception) {
            Log.d("LOG", "Failed to get camera: " + e.message);
        }

        //btn to close the application
        val imgClose = v.findViewById<ImageButton>(R.id.imgClose)
        imgClose.setOnClickListener {
            activity?.finish()
        }

        val takePicture = v.findViewById<ImageButton>(R.id.takePicture)
        takePicture.setOnClickListener {
            mCameraView?.takePicture()
            setLastPic()
        }

        openGallery = v.findViewById(R.id.gallery)
        openGallery?.setOnClickListener {
            val fragmentTransaction = activity?.supportFragmentManager?.beginTransaction()
            fragmentTransaction?.replace(R.id.main, GalleryFragment.newInstance())?.addToBackStack("gallery")
            fragmentTransaction?.commit()
        }
       // btnGallery?.load(getAllImages(activity?.baseContext!!).lastOrNull() ?: return)
    }

    override fun onResume() {
        super.onResume()
    }

    private fun initCameraOpen() {
        mCameraView = CameraView(activity?.baseContext!!, mCamera!!)
        cameraView?.addView(mCameraView!!)
    }

    private fun setLastPic() {
      //  btnGallery?.load(getAllImages(activity?.baseContext!!).lastOrNull() ?: return)
    }

    override fun onPause() {
        super.onPause()
        mCameraView?.releaseCamera()
    }
}