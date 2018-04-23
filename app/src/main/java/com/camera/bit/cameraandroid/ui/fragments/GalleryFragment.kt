package com.camera.bit.cameraandroid.ui.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.camera.bit.cameraandroid.R
import com.camera.bit.cameraandroid.getAllImages
import com.camera.bit.cameraandroid.presenters.GalleryPresenter
import com.camera.bit.cameraandroid.ui.adapters.CustomPagerAdapter
import com.camera.bit.cameraandroid.view.GalleryFragmentView
import java.io.File


class GalleryFragment() : Fragment(), GalleryFragmentView {
    override fun setItems(list: ArrayList<File>) {
    }

    private lateinit var viewAdapter: CustomPagerAdapter
    private var presenter: GalleryPresenter? = null

    companion object {
        fun newInstance(): GalleryFragment {
            return GalleryFragment()
        }
    }

    var viewPager: ViewPager? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.gallery_fragment, container, false)
        presenter?.attachView(this)

        viewAdapter = CustomPagerAdapter(activity?.baseContext!!) {
            presenter?.sharePhote()
        }

        viewPager = v.findViewById<ViewPager>(R.id.pager).apply {
            adapter = viewAdapter
        }
        viewAdapter.setItems(getAllImages(activity?.baseContext!!))

        return v
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager = null
    }
}