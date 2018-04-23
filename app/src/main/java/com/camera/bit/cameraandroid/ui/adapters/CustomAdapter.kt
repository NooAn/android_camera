package com.camera.bit.cameraandroid.ui.adapters

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.camera.bit.cameraandroid.R
import com.camera.bit.cameraandroid.loadImage
import java.io.File

interface FileSelectionListener {
    fun clickFile()
}

class CustomPagerAdapter(mContext: Context, var selectionListener: () -> Unit) : PagerAdapter() {
    var mLayoutInflater: LayoutInflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var mFiles = mutableListOf<File>()

    fun setItems(files: List<File>) {
        mFiles = files.reversed().toMutableList()
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return mFiles.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as LinearLayout
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val itemView = mLayoutInflater.inflate(R.layout.viewholder_image_file, container, false)

        val imageView = itemView.findViewById(R.id.viewholderImageView) as ImageView

        imageView.loadImage(mFiles[position])

        container.addView(itemView)
        Log.e("LOG", "err")

        return itemView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as LinearLayout)
    }
}