package com.camera.bit.cameraandroid.ui.fragments


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.camera.bit.cameraandroid.*
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.MultiDetector
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.gms.vision.face.FaceDetector
import java.io.File
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
    var barcodeText: TextView? = null

    private fun initCamera(v: View) {
        mGraphicOverlay = v.findViewById<View>(R.id.faceOverlay) as GraphicOverlay
        mPreview = v.findViewById<View>(R.id.preview) as CameraSourcePreview
        barcodeText = v.findViewById<TextView>(R.id.textBarcode)
        barcodeText?.setOnClickListener {
            if (barcodeText?.text.toString().isNotBlank()) {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(barcodeText?.text.toString())
                startActivity(i)
            }
        }
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

        val faceDetector = FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build()
        val faceFactory = FaceTrackerFactory(mGraphicOverlay!!)

        faceDetector.setProcessor(
                MultiProcessor.Builder(faceFactory).build())


        val barcodeDetector = BarcodeDetector.Builder(context).build()
        val barcodeFactory = BarcodeTrackerFactory(mGraphicOverlay!!) {
            showBarcode(it)
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
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(20.0f)
                // .setRequestedPreviewSize(640, 480)
                .setRequestedPreviewSize(1600, 1024)
                .setAutoFocusEnabled(true)
                .build()
    }

    private fun showBarcode(barcode: Barcode) {
        if (barcode.rawValue.isNotEmpty()) {
            barcodeText?.text = barcode.rawValue
        }
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
            val bitmap = realImage.rotate(data, getOrientation(data).toFloat())
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
            //update photo storage for system
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
            val date = Date()
            date.time = System.currentTimeMillis() + Random().nextInt(1000) + 1
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(date)
            return File(getAlbumDir(), "IMG_$timeStamp.jpg")
        } catch (e: Exception) {
        }
        return null
    }

    fun addMediaToGallery(fromPath: String?) {
        fromPath ?: return
        val f = File(fromPath)
        val contentUri = Uri.fromFile(f)
        contentUri.addMediaToGallery(activity)
    }

    private fun getOrientation(jpeg: ByteArray?): Int {
        jpeg ?: return 0

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




