package com.jorkoh.polyrhythmtrainer

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder


class GameSurfaceView : GLSurfaceView {
    private val mRenderer: RendererWrapper

    constructor(context: Context?) : super(context) {
        setEGLContextClientVersion(2)
        mRenderer = RendererWrapper()
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setEGLContextClientVersion(2)
        mRenderer = RendererWrapper()
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        native_surfaceDestroyed()
        super.surfaceDestroyed(holder)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In our case we care about DOWN events.
        when (e.action) {
            MotionEvent.ACTION_DOWN -> native_onTouchInput(
                0,
                e.eventTime,
                e.x.toInt(),
                e.y.toInt()
            )
        }
        return true
    }


    external fun native_onTouchInput(
        eventType: Int,
        timeSinceBootMs: Long,
        pixel_x: Int,
        pixel_y: Int
    )

    external fun native_surfaceDestroyed()
}
