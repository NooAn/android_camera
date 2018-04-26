package com.camera.bit.cameraandroid.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.camera.bit.cameraandroid.ImageRepository
import com.camera.bit.cameraandroid.R
import com.camera.bit.cameraandroid.presenters.GalleryPresenter
import com.camera.bit.cameraandroid.ui.adapters.CustomPagerAdapter
import com.camera.bit.cameraandroid.view.GalleryFragmentView
import java.io.File


class GalleryFragment() : Fragment(), GalleryFragmentView {

    private lateinit var viewAdapter: CustomPagerAdapter
    private lateinit var presenter: GalleryPresenter

    companion object {
        fun newInstance(): GalleryFragment {
            return GalleryFragment()
        }
    }

    var viewPager: ViewPager? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.gallery_fragment, container, false)
        presenter = GalleryPresenter(ImageRepository(activity?.baseContext!!))
        presenter.attachView(this)

        val share = v.findViewById<ImageView>(R.id.shareImage)
        share.visibility = View.INVISIBLE

        viewAdapter = CustomPagerAdapter(activity?.baseContext!!) {
            share.visibility = View.VISIBLE
            share.setOnClickListener { _ ->
                presenter.sharePhote(it.toURI())
            }
        }

        viewPager = v.findViewById<ViewPager>(R.id.pager).apply {
            adapter = viewAdapter
        }

        viewPager?.setPageTransformer(true, { view, position ->
            view.setBackgroundColor(resources.getColor(R.color.black))
            if (position < -1) {
                view.alpha = 0f
            } else if (position <= 1) { // [-1,1]
                val misScale = 0.85f;
                val scaleFactor = Math.max(misScale, 1 - Math.abs(position))
                val vertMargin = view.height * (1 - scaleFactor) / 2
                val horzMargin = view.width * (1 - scaleFactor) / 2
                if (position < 0) {
                    view.translationX = horzMargin - vertMargin / 2
                } else view.translationX = -horzMargin + vertMargin / 2

                view.scaleX = scaleFactor
                view.scaleY = scaleFactor

                // Fade the page relative to its size.
                val minAlpha = 0.5f;
                view.alpha = minAlpha + (scaleFactor - misScale) / (1 - misScale) * (1 - minAlpha)

            } else {
                // This page is way off-screen to the right.
                view.alpha = 0f
            }
        })
        viewPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                share.visibility = View.INVISIBLE
            }
        })
        presenter.getAllImages()

        return v
    }

    override fun share(url: Intent) {
        activity?.startActivity(url)
    }

    override fun setItems(list: List<File>) {
        viewAdapter.setItems(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager = null
    }
}