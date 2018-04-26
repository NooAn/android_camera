package com.camera.bit.cameraandroid.presenters

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import com.camera.bit.cameraandroid.ImageRepository
import com.camera.bit.cameraandroid.rotate
import com.camera.bit.cameraandroid.view.CameraFragmentView
import com.google.android.gms.vision.barcode.Barcode
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CameraPresenter(private val imageRepository: ImageRepository) : BasePresenter<CameraFragmentView>() {
    fun clickBarcodeText(text: String) {
        if (text.isNotBlank()) {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(text)
            view?.openWeb(i)
        }
    }

    fun takeLastPicture() {
        view?.showLastPicture(imageRepository.getAllImages().lastOrNull() ?: return)
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
            val bitmap = realImage.rotate(getOrientation(data).toFloat())
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
            view?.addMediaToGallery(it.path ?: return)
            view?.showLastPicture(it)
        }
    }

    private fun getAlbumDir(): File? {
        var storageDir: File? = null
        storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera")
        if (!storageDir.mkdirs() && !storageDir.exists()) {
            return null
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
            e.printStackTrace()
        }
        return null
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

    fun showBarcode(barcode: Barcode) {
        if (barcode.rawValue.isNotEmpty())
            view?.showBarcode(barcode = barcode.rawValue)
    }
}