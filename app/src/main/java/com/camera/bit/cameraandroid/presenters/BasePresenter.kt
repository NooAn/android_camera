package com.camera.bit.cameraandroid.presenters

import com.camera.bit.cameraandroid.view.MvpView


interface MvpPresenter<V : MvpView> {
    fun attachView(mvpView: V)
    fun detachView()
    fun destroy()
}

abstract class BasePresenter<T : MvpView> : MvpPresenter<T> {
    var view: T? = null
    override fun attachView(v: T) {
        view = v
    }

    override fun detachView() {
        view = null
    }

    protected fun isViewAttached(): Boolean {
        return view != null
    }

    override fun destroy() {}
}