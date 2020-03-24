package com.jorkoh.polyrhythmtrainer

import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


class RendererWrapper : GLSurfaceView.Renderer {
    override fun onSurfaceCreated(gl10: GL10?, eglConfig: EGLConfig?) {
        native_onSurfaceCreated()
    }

    override fun onSurfaceChanged(gl10: GL10, width: Int, height: Int) {
        native_onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl10: GL10) {
        native_onDrawFrame()
    }


    external fun native_onSurfaceCreated()
    external fun native_onSurfaceChanged(widthInPixels: Int, heightInPixels: Int)
    external fun native_onDrawFrame()
}
