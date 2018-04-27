package com.camera.bit.cameraandroid.ui.activity

import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Window
import android.view.WindowManager
import com.camera.bit.cameraandroid.R
import com.camera.bit.cameraandroid.ui.fragments.CameraVisionFragment

class MainActivity : AppCompatActivity() {

    private val MY_PERMISSIONS_REQUEST_CAMERA: Int = 300
    private val MY_PERMISSIONS_REQUEST_SAVE: Int = 301
    var PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main)

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.CAMERA), MY_PERMISSIONS_REQUEST_CAMERA)
            }
        } else {
            if (savedInstanceState == null) {
                openCameraFragment()
            }
        }
    }

    private fun openCameraFragment() {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.main, CameraVisionFragment.newInstance())
        fragmentTransaction.commit()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_SAVE, MY_PERMISSIONS_REQUEST_CAMERA -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    openCameraFragment()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
        if (context != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount
        if (count == 0) {
            super.onBackPressed()
        } else {
            supportFragmentManager.popBackStack()
        }
    }
}

