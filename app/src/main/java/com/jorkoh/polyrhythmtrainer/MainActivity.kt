package com.jorkoh.polyrhythmtrainer

import android.content.Context
import android.content.res.AssetManager
import android.media.AudioManager
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    init {
        System.loadLibrary("native-lib")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setDefaultStreamValues(this)
    }

    override fun onResume() {
        super.onResume()
        nativeOnStart(assets)
    }

    override fun onPause() {
        super.onPause()
        nativeOnStop()
    }

    private fun setDefaultStreamValues(context: Context) {
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).apply {
            nativeSetDefaultStreamValues(
                getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE).toInt(),
                getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER).toInt()
            )
        }
    }


    private external fun nativeOnStart(assetManager: AssetManager)
    private external fun nativeOnStop()
    private external fun nativeSetDefaultStreamValues(defaultSampleRate: Int, defaultFramesPerBurst: Int)
}