package com.camera.bit.cameraandroid.ui.fragments


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.transition.*
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.camera.bit.cameraandroid.ImageRepository
import com.camera.bit.cameraandroid.R
import com.camera.bit.cameraandroid.addMediaToGallery
import com.camera.bit.cameraandroid.load
import com.camera.bit.cameraandroid.presenters.CameraPresenter
import com.camera.bit.cameraandroid.view.CameraFragmentView
import com.camera.bit.cameraandroid.vision.BarcodeTrackerFactory
import com.camera.bit.cameraandroid.vision.CameraSourcePreview
import com.camera.bit.cameraandroid.vision.FaceTrackerFactory
import com.camera.bit.cameraandroid.vision.GraphicOverlay
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.MultiDetector
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.gms.vision.face.FaceDetector
import java.io.File
import java.io.IOException


class CameraVisionFragment : Fragment(), CameraFragmentView {

    override fun openWeb(intent: Intent) {
        startActivity(intent)
    }

    companion object {
        fun newInstance(): CameraVisionFragment {
            return CameraVisionFragment()
        }
    }

    private lateinit var presenter: CameraPresenter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = CameraPresenter(ImageRepository(activity?.baseContext!!))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.camera_vision_fragment, container, false)
        presenter.attachView(this)
        initCamera(v ?: return null)
        return v
    }


    var btnGallery: ImageView? = null
    private var mCameraSource: CameraSource? = null
    var mGraphicOverlay: GraphicOverlay? = null
    var mPreview: CameraSourcePreview? = null
    var barcodeText: TextView? = null

    private fun initCamera(v: View) {
        mGraphicOverlay = v.findViewById<View>(R.id.faceOverlay) as GraphicOverlay
        mPreview = v.findViewById<View>(R.id.preview) as CameraSourcePreview
        barcodeText = v.findViewById<TextView>(R.id.textBarcode)
        barcodeText?.setOnClickListener {
            presenter.clickBarcodeText(barcodeText?.text.toString())
        }
        createCameraSource(CameraSource.CAMERA_FACING_BACK)


        //btn to close the application
        val changeCamera = v.findViewById<ImageView>(R.id.switchCamera)
        changeCamera.setOnClickListener {
            changeCamera()
        }
        val takePicture = v.findViewById<ImageButton>(R.id.takePicture)
        takePicture.setOnClickListener {
            mCameraSource?.takePicture(null) {
                presenter.makePicture(it)
            }
        }

        btnGallery = v.findViewById(R.id.gallery)
        btnGallery?.setOnClickListener {
            val fragmentTransaction = activity?.supportFragmentManager?.beginTransaction()
            val gallery = GalleryFragment.newInstance()


            //setEnterTransition(fadeTransition)
            // exitTransition = Fade()
            //   gallery.enterTransition = Fade()
            //  gallery.exitTransition = Fade()
//
//            gallery.sharedElementReturnTransition = DetailsTransition()
//            gallery.sharedElementEnterTransition = DetailsTransition()
//            sharedElementEnterTransition = DetailsTransition()
//            sharedElementReturnTransition = DetailsTransition()
            val slideTransition = Slide(Gravity.RIGHT)
            slideTransition.duration = 700;
            exitTransition = Fade()
            enterTransition = Fade()
            gallery.enterTransition = slideTransition;
            gallery.sharedElementEnterTransition = ChangeBounds()
            gallery.allowEnterTransitionOverlap = false;
            gallery.allowReturnTransitionOverlap = false;
            fragmentTransaction
                    ?.replace(R.id.main, gallery)
                    ?.addSharedElement(btnGallery, "firstImage")
                    ?.addToBackStack("gallery")
            fragmentTransaction?.commit()
        }
        presenter.takeLastPicture()
    }

    override fun showLastPicture(path: File) {
        btnGallery?.load(path)
    }

    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    override fun onPause() {
        super.onPause()
        mPreview?.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mCameraSource?.release()

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    var cameraNow = CameraSource.CAMERA_FACING_BACK

    fun changeCamera() {
        mCameraSource?.release()
        mPreview?.release()
        mPreview?.destroyDrawingCache()
        mCameraSource = null
        if (cameraNow == CameraSource.CAMERA_FACING_BACK) {
            createCameraSource(CameraSource.CAMERA_FACING_FRONT)
            cameraNow = CameraSource.CAMERA_FACING_FRONT
        } else {
            createCameraSource(CameraSource.CAMERA_FACING_BACK)
            cameraNow = CameraSource.CAMERA_FACING_BACK
        }

    }

    private fun createCameraSource(facing: Int) {

        val context = activity?.applicationContext

        val faceDetector = FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build()
        val faceFactory = FaceTrackerFactory(mGraphicOverlay!!)

        faceDetector.setProcessor(
                MultiProcessor.Builder(faceFactory).build())


        val barcodeDetector = BarcodeDetector.Builder(context).build()
        val barcodeFactory = BarcodeTrackerFactory(mGraphicOverlay!!) {
            presenter.showBarcode(it)
        }
        barcodeDetector.setProcessor(
                MultiProcessor.Builder(barcodeFactory).build())


        val multiDetector = MultiDetector.Builder()
                .add(faceDetector)
                .add(barcodeDetector)
                .build()

        if (!multiDetector.isOperational) {
            Log.w("LOG", "Face detector dependencies are not yet available.")
        }

        mCameraSource = CameraSource.Builder(context, multiDetector)
                .setFacing(facing)
                .setRequestedFps(16.0f)
                // .setRequestedPreviewSize(640, 480)
                .setRequestedPreviewSize(1600, 1024)
                .setAutoFocusEnabled(true)
                .build()
    }

    override fun showBarcode(barcode: String) {
        barcodeText?.text = barcode
    }


    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() {
        if (mCameraSource != null) {
            try {
                mPreview?.start(mCameraSource, mGraphicOverlay)
            } catch (e: IOException) {
                Log.e("LOG", "Unable to start camera source.", e)
                mCameraSource?.release()
                mCameraSource = null
            }
        }
    }


    override fun addMediaToGallery(path: String) =
            Uri.fromFile(File(path)).addMediaToGallery(activity)


}

class DetailsTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER;
        addTransition(ChangeBounds())
                .addTransition(ChangeImageTransform());
    }
}


