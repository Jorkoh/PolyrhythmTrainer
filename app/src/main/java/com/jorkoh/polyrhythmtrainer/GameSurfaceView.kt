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
}
