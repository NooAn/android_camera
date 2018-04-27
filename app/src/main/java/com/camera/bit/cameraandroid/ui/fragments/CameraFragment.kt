package com.camera.bit.cameraandroid.ui.fragments

import android.content.Context
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
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import com.camera.bit.cameraandroid.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

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

class CameraView(context1: Context, camera: Camera) : SurfaceView(context1), SurfaceHolder.Callback {
    var mHolder: SurfaceHolder;
    var mCamera: Camera? = camera

    init {
        mCamera?.setDisplayOrientation(90)
        //get the holder and load this class as the callback, so we can get camera data here
        mHolder = holder
        mHolder.addCallback(this)
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        try {
            //when the surface is created, we can load the camera to draw images in this surfaceholder
            mCamera?.setPreviewDisplay(surfaceHolder)
            mCamera?.startPreview()
            mCamera?.enableShutterSound(true)

        } catch (e: Exception) {
            Log.d("ERROR", "Camera error on surfaceCreated " + e.message);
        }
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        mHolder.surface ?: return ///check if the surface is ready to receive camera data

        try {
            mCamera?.stopPreview()
        } catch (e: Exception) {
            //this will happen when you are trying the camera if it's not running
            e.printStackTrace()
        }
        val camParams = mCamera?.getParameters()
        val size = camParams?.getSupportedPreviewSizes()?.get(0)
        camParams?.setPreviewSize(size!!.width, size.height)
        mCamera?.parameters = camParams

        //now, recreate the camera preview
        try {
            mCamera?.setPreviewDisplay(mHolder)
            mCamera?.setDisplayOrientation(90)
            mCamera?.startPreview();
        } catch (e: IOException) {
            Log.d("ERROR", "Camera error on surfaceChanged " + e.message);
        }
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        mCamera?.stopPreview();
        mCamera?.release();
    }

    fun releaseCamera() {
        mCamera?.setPreviewCallback(null)
        mCamera?.release()
        mCamera = null
    }

    fun takePicture() {
        mCamera?.takePicture(null, null) { data, camera ->
            val path = generatePicturePath()
            val fos = FileOutputStream(path)

            try {
                val targetW = 450
                val targetH = 540

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
                realImage.recycle()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
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
            camera.startPreview()
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
            context.sendBroadcast(mediaScanIntent)
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