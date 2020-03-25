package com.jorkoh.polyrhythmtrainer

import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


class RendererWrapper : GLSurfaceView.Renderer {
    override fun onSurfaceCreated(gl10: GL10?, eglConfig: EGLConfig?) {}

    override fun onSurfaceChanged(gl10: GL10, width: Int, height: Int) {}

    override fun onDrawFrame(gl10: GL10) {
        nativeOnDrawFrame()
    }

    private external fun nativeOnDrawFrame()
}
