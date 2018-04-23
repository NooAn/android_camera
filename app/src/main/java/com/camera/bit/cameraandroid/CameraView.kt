package com.camera.bit.cameraandroid

import android.content.Context
import android.hardware.Camera
import android.os.Environment
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class CameraView(context1: Context, camera: Camera) : SurfaceView(context1), SurfaceHolder.Callback {
    var mHolder: SurfaceHolder;
    var mCamera: Camera = camera

    init {
        mCamera.setDisplayOrientation(90);
        //get the holder and load this class as the callback, so we can get camera data here
        mHolder = holder
        mHolder.addCallback(this)
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        try {
            //when the surface is created, we can load the camera to draw images in this surfaceholder
            mCamera.setPreviewDisplay(surfaceHolder)
            mCamera.startPreview()
            mCamera.enableShutterSound(true)
        } catch (e: Exception) {
            Log.d("ERROR", "Camera error on surfaceCreated " + e.message);
        }
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        if (mHolder.surface == null)//check if the surface is ready to receive camera data
            return;

        try {
            mCamera.stopPreview();
        } catch (e: Exception) {
            //this will happen when you are trying the camera if it's not running
        }

        //now, recreate the camera preview
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (e: IOException) {
            Log.d("ERROR", "Camera error on surfaceChanged " + e.message);
        }
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        mCamera.stopPreview();
        mCamera.release();
    }

    fun takePicture() {
        mCamera.takePicture(null, null) { data, camera ->
            val path = generatePicturePath()
            val fos = FileOutputStream(String.format("${path}%d.jpg", System.currentTimeMillis()))
            try {
                fos.write(data)
                fos.flush()
                fos.fd.sync()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                fos.close()
            }
            camera.startPreview()
        }
    }

    private fun getAlbumDir(): File? {
        var storageDir: File? = null
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Camera")
            if (!storageDir.mkdirs()) {
                if (!storageDir.exists()) {
                    return null
                }
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

//    fun takePicture(path: File, session: CameraSession?, callback: Runnable?): Boolean {
//        if (session == null) {
//            return false
//        }
//        val info = session!!.cameraInfo
//        val camera = info.camera
//        try {
//            camera.takePicture(null, null, Camera.PictureCallback { data, camera ->
//                var bitmap: Bitmap? = null
//                val size = (AndroidUtilities.getPhotoSize() / AndroidUtilities.density) as Int
//                val key = String.format(Locale.US, "%s@%d_%d", Utilities.MD5(path.getAbsolutePath()), size, size)
//                try {
//                    val options = BitmapFactory.Options()
//                    options.inJustDecodeBounds = true
//                    BitmapFactory.decodeByteArray(data, 0, data.size, options)
//                    var scaleFactor = Math.max(options.outWidth.toFloat() / AndroidUtilities.getPhotoSize(), options.outHeight.toFloat() / AndroidUtilities.getPhotoSize())
//                    if (scaleFactor < 1) {
//                        scaleFactor = 1f
//                    }
//                    options.inJustDecodeBounds = false
//                    options.inSampleSize = scaleFactor.toInt()
//                    options.inPurgeable = true
//                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
//                } catch (e: Throwable) {
//                }
//
//                try {
//                    if (info.frontCamera !== 0) {
//                        try {
//                            val matrix = Matrix()
//                            matrix.setRotate(getOrientation(data).toFloat())
//                            matrix.postScale(-1f, 1f)
//                            val scaled = Bitmaps.createBitmap(bitmap, 0, 0, bitmap!!.width, bitmap.height, matrix, false)
//                            bitmap.recycle()
//                            val outputStream = FileOutputStream(path)
//                            scaled!!.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
//                            outputStream.flush()
//                            outputStream.getFD().sync()
//                            outputStream.close()
//                            if (scaled != null) {
//                            }
//                            callback?.run()
//                            return@PictureCallback
//                        } catch (e: Throwable) {
//                        }
//
//                    }
//                    val outputStream = FileOutputStream(path)
//                    outputStream.write(data)
//                    outputStream.flush()
//                    outputStream.getFD().sync()
//                    outputStream.close()
//                    if (bitmap != null) {
//                    }
//                } catch (e: Exception) {
//                }
//
//                callback?.run()
//            })
//            return true
//        } catch (e: Exception) {
//        }
//
//        return false
//    }
//
//    private fun getOrientation(jpeg: ByteArray?): Int {
//        if (jpeg == null) {
//            return 0
//        }
//
//        var offset = 0
//        var length = 0
//
//        while (offset + 3 < jpeg.size && jpeg[offset++].toInt() and 0xFF == 0xFF) {
//            val marker = jpeg[offset].toInt() and 0xFF
//
//            if (marker == 0xFF) {
//                continue
//            }
//            offset++
//
//            if (marker == 0xD8 || marker == 0x01) {
//                continue
//            }
//            if (marker == 0xD9 || marker == 0xDA) {
//                break
//            }
//
//            length = pack(jpeg, offset, 2, false)
//            if (length < 2 || offset + length > jpeg.size) {
//                return 0
//            }
//
//            // Break if the marker is EXIF in APP1.
//            if (marker == 0xE1 && length >= 8 &&
//                    pack(jpeg, offset + 2, 4, false) === 0x45786966 &&
//                    pack(jpeg, offset + 6, 2, false) === 0) {
//                offset += 8
//                length -= 8
//                break
//            }
//
//            offset += length
//            length = 0
//        }
//
//        if (length > 8) {
//            var tag = pack(jpeg, offset, 4, false)
//            if (tag != 0x49492A00 && tag != 0x4D4D002A) {
//                return 0
//            }
//            val littleEndian = tag == 0x49492A00
//
//            var count = pack(jpeg, offset + 4, 4, littleEndian) + 2
//            if (count < 10 || count > length) {
//                return 0
//            }
//            offset += count
//            length -= count
//
//            count = pack(jpeg, offset - 2, 2, littleEndian)
//            while (count-- > 0 && length >= 12) {
//                tag = pack(jpeg, offset, 2, littleEndian)
//                if (tag == 0x0112) {
//                    val orientation = pack(jpeg, offset + 8, 2, littleEndian)
//                    when (orientation) {
//                        1 -> return 0
//                        3 -> return 180
//                        6 -> return 90
//                        8 -> return 270
//                    }
//                    return 0
//                }
//                offset += 12
//                length -= 12
//            }
//        }
//        return 0
//    }
//
//    private fun pack(bytes: ByteArray, offset: Int, length: Int, littleEndian: Boolean): Int {
//        var offset = offset
//        var length = length
//        var step = 1
//        if (littleEndian) {
//            offset += length - 1
//            step = -1
//        }
//        val b: Int = 0xFF.toInt()
//        var value = 0
//        while (length-- > 0) {
//            value = value shl 8 or (bytes[offset].toInt() and b)
//            offset += step
//        }
//        return value
//    }


}