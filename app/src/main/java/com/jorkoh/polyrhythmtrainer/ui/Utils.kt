package com.jorkoh.polyrhythmtrainer.ui

import android.os.SystemClock
import android.view.View
import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<T>.mutate(actions: MutableLiveData<T>.() -> Unit) {
    actions(this)
    this.value = this.value
}

class DebounceClickListener(
    private val debounceInterval: Long = DEBOUNCE_INTERVAL_DEFAULT,
    private val methodToCall: (v: View) -> Unit
) : View.OnClickListener {

    companion object {
        private const val DEBOUNCE_INTERVAL_DEFAULT: Long = 200
        private var lastClickTime: Long = 0
    }

    override fun onClick(v: View) {
        if (SystemClock.elapsedRealtime() - lastClickTime < debounceInterval) {
            return
        }
        lastClickTime = SystemClock.elapsedRealtime()
        methodToCall(v)
    }
}