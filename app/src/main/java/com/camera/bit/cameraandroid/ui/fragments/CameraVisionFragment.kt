package com.camera.bit.cameraandroid.ui.fragments


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import com.camera.bit.cameraandroid.*
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.gms.vision.Detector.Detections
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CameraVisionFragment : Fragment() {

    companion object {
        fun newInstance(): CameraVisionFragment {
            return CameraVisionFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.camera_vision_fragment, container, false)
        initCamera(v ?: return v)
        return v
    }


    var openGallery: ImageView? = null
    private var mCameraSource: CameraSource? = null
    var mGraphicOverlay: GraphicOverlay? = null
    var mPreview: CameraSourcePreview? = null

    private fun initCamera(v: View) {
        mGraphicOverlay = v.findViewById<View>(R.id.faceOverlay) as GraphicOverlay
        mPreview = v.findViewById<View>(R.id.preview) as CameraSourcePreview
        createCameraSource()
        //btn to close the application
        val imgClose = v.findViewById<ImageButton>(R.id.imgClose)
        imgClose.setOnClickListener {
            activity?.finish()
        }

        val takePicture = v.findViewById<ImageButton>(R.id.takePicture)
        takePicture.setOnClickListener {
            mCameraSource?.takePicture(null) {
                makePicture(it)
            }
            setLastPic()
        }

        openGallery = v.findViewById(R.id.gallery)
        openGallery?.setOnClickListener {
            val fragmentTransaction = activity?.supportFragmentManager?.beginTransaction()
            fragmentTransaction?.replace(R.id.main, GalleryFragment.newInstance())?.addToBackStack("gallery")
            fragmentTransaction?.commit()
        }
        openGallery?.load(getAllImages(activity?.baseContext!!).lastOrNull() ?: return)
    }

    private fun setLastPic() {
        openGallery?.load(getAllImages(activity?.baseContext!!).lastOrNull() ?: return)
    }

    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    override fun onPause() {
        super.onPause()
        mPreview?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mCameraSource != null) {
            mCameraSource?.release()
        }
    }

    private fun createCameraSource() {

        val context = activity?.applicationContext
        val detector = FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build()

        detector.setProcessor(
                MultiProcessor.Builder<Face>(GraphicFaceTrackerFactory())
                        .build())

        if (!detector.isOperational) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w("LOG", "Face detector dependencies are not yet available.")
        }

        mCameraSource = CameraSource.Builder(context, detector)
                // .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .setAutoFocusEnabled(true)
                .build()
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

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private inner class GraphicFaceTrackerFactory : MultiProcessor.Factory<Face> {
        override fun create(face: Face): Tracker<Face> {
            return GraphicFaceTracker(mGraphicOverlay!!)
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker(private val mOverlay: GraphicOverlay) : Tracker<Face>() {
        private val mFaceGraphic: FaceGraphic = FaceGraphic(mOverlay)


        /**
         * Start tracking the detected face instance within the face overlay.
         */
        override fun onNewItem(faceId: Int, item: Face?) {
            mFaceGraphic.setId(faceId)
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        override fun onUpdate(detectionResults: Detections<Face>?, face: Face?) {
            mOverlay.add(mFaceGraphic)
            mFaceGraphic.updateFace(face)
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        override fun onMissing(detectionResults: Detections<Face>?) {
            mOverlay.remove(mFaceGraphic)
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        override fun onDone() {
            mOverlay.remove(mFaceGraphic)
        }
    }

    fun makePicture(data: ByteArray) {
        val path = generatePicturePath()
        val fos = FileOutputStream(path)

        try {
            val targetW = 480
            val targetH = 640

            // Get the dimensions of the bitmap
            val bmOptions = BitmapFactory.Options()
            bmOptions.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path?.path, bmOptions)
            val photoW = bmOptions.outWidth
            val photoH = bmOptions.outHeight

            // Determine how much to scale down the image
            val scaleFactor = Math.min(photoW / targetW, photoH / targetH)

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false
            bmOptions.inSampleSize = scaleFactor
            bmOptions.inPurgeable = true

            val realImage = BitmapFactory.decodeByteArray(data, 0, data.size, bmOptions)
            val bitmap = rotate(realImage, data)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
            realImage.recycle()
            fos.flush()
            fos.fd.sync()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fos.close()
        }
        path?.let {
            addMediaToGallery(it.path)
        }
    }

    private fun getAlbumDir(): File? {
        var storageDir: File? = null
        storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera")
        if (!storageDir.mkdirs()) {
            if (!storageDir.exists()) {
                return null
            }
        }
        return storageDir
    }

    private fun generatePicturePath(): File? {
        try {
            val storageDir = getAlbumDir()
            val date = Date()
            date.time = System.currentTimeMillis() + Random().nextInt(1000) + 1
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(date)
            return File(storageDir, "IMG_$timeStamp.jpg")
        } catch (e: Exception) {
        }
        return null
    }

    fun addMediaToGallery(fromPath: String?) {
        if (fromPath == null) {
            return
        }
        val f = File(fromPath)
        val contentUri = Uri.fromFile(f)
        addMediaToGallery(contentUri)
    }

    fun addMediaToGallery(uri: Uri?) {
        if (uri == null) {
            return
        }
        try {
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = uri
            activity?.sendBroadcast(mediaScanIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun rotate(bitmap: Bitmap, data: ByteArray): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val matrix = Matrix()
        matrix.preScale(-1f, 1f);
        matrix.setRotate(getOrientation(data).toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, false)
    }

    private fun getOrientation(jpeg: ByteArray?): Int {
        if (jpeg == null) {
            return 0
        }

        var offset = 0
        var length = 0

        while (offset + 3 < jpeg.size && jpeg[offset++].toInt() and 0xFF == 0xFF) {
            val marker = jpeg[offset].toInt() and 0xFF

            if (marker == 0xFF) {
                continue
            }
            offset++

            if (marker == 0xD8 || marker == 0x01) {
                continue
            }
            if (marker == 0xD9 || marker == 0xDA) {
                break
            }

            length = pack(jpeg, offset, 2, false)
            if (length < 2 || offset + length > jpeg.size) {
                return 0
            }

            // Break if the marker is EXIF in APP1.
            if (marker == 0xE1 && length >= 8 &&
                    pack(jpeg, offset + 2, 4, false) === 0x45786966 &&
                    pack(jpeg, offset + 6, 2, false) === 0) {
                offset += 8
                length -= 8
                break
            }

            offset += length
            length = 0
        }

        if (length > 8) {
            var tag = pack(jpeg, offset, 4, false)
            if (tag != 0x49492A00 && tag != 0x4D4D002A) {
                return 0
            }
            val littleEndian = tag == 0x49492A00

            var count = pack(jpeg, offset + 4, 4, littleEndian) + 2
            if (count < 10 || count > length) {
                return 0
            }
            offset += count
            length -= count

            count = pack(jpeg, offset - 2, 2, littleEndian)
            while (count-- > 0 && length >= 12) {
                tag = pack(jpeg, offset, 2, littleEndian)
                if (tag == 0x0112) {
                    val orientation = pack(jpeg, offset + 8, 2, littleEndian)
                    when (orientation) {
                        1 -> return 0
                        3 -> return 180
                        6 -> return 90
                        8 -> return 270
                    }
                    return 0
                }
                offset += 12
                length -= 12
            }
        }
        return 0
    }

    private fun pack(bytes: ByteArray, offset: Int, length: Int, littleEndian: Boolean): Int {
        var offset = offset
        var length = length
        var step = 1
        if (littleEndian) {
            offset += length - 1
            step = -1
        }
        val b: Int = 0xFF.toInt()
        var value = 0
        while (length-- > 0) {
            value = value shl 8 or (bytes[offset].toInt() and b)
            offset += step
        }
        return value
    }

}