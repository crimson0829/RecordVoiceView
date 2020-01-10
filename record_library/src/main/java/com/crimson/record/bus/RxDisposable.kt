package com.crimson.record.bus

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable


/**
 * @author crimson
 * @date @date 2019-09-20
 * 管理 CompositeDisposable
 */
object RxDisposable {

    private val mDisposable = CompositeDisposable()
    val isDisposed: Boolean
        get() = mDisposable.isDisposed

    fun add(s: Disposable?) {
        if (s != null) {
            mDisposable.add(s)
        }
    }

    fun remove(s: Disposable?) {
        if (s != null) {
            mDisposable.remove(s)
        }
    }

    fun clear() {
        mDisposable.clear()
    }

    fun dispose() {
        mDisposable.dispose()
    }
}