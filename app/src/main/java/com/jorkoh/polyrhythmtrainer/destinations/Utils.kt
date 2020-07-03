package com.jorkoh.polyrhythmtrainer.destinations

import android.animation.TimeInterpolator
import android.os.SystemClock
import android.view.View
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.lifecycle.MutableLiveData
import androidx.transition.Transition
import androidx.transition.TransitionSet
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

// TODO move this somewhere reasonable
enum class PadPosition(val nativeValue: Int) {
    Left(0),
    Right(1)
}

fun <T> MutableLiveData<T>.mutate(actions: MutableLiveData<T>.() -> Unit) {
    actions(this)
    this.value = this.value
}

/**
 * Standard easing.
 *
 * Elements that begin and end at rest use standard easing. They speed up quickly and slow down
 * gradually, in order to emphasize the end of the transition.
 */
val FAST_OUT_SLOW_IN: TimeInterpolator by lazy(LazyThreadSafetyMode.NONE) {
    PathInterpolatorCompat.create(0.4f, 0f, 0.2f, 1f)
}

/**
 * Decelerate easing.
 *
 * Incoming elements are animated using deceleration easing, which starts a transition at peak
 * velocity (the fastest point of an element’s movement) and ends at rest.
 */
val LINEAR_OUT_SLOW_IN: TimeInterpolator by lazy(LazyThreadSafetyMode.NONE) {
    PathInterpolatorCompat.create(0f, 0f, 0.2f, 1f)
}

/**
 * Accelerate easing.
 *
 * Elements exiting a screen use acceleration easing, where they start at rest and end at peak
 * velocity.
 */
val FAST_OUT_LINEAR_IN: TimeInterpolator by lazy(LazyThreadSafetyMode.NONE) {
    PathInterpolatorCompat.create(0.4f, 0f, 1f, 1f)
}

inline fun transitionTogether(crossinline body: TransitionSet.() -> Unit): TransitionSet {
    return TransitionSet().apply {
        ordering = TransitionSet.ORDERING_TOGETHER
        body()
    }
}

operator fun TransitionSet.plusAssign(transition: Transition?) {
    if (transition != null) {
        addTransition(transition)
    }
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

fun Date.toSimpleString(): String {
    val format = SimpleDateFormat.getDateInstance(DateFormat.SHORT)
    return format.format(this)
}