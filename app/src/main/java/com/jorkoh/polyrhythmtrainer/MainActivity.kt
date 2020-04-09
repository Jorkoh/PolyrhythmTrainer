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
        setContentView(R.layout.main_activity)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Set default stream values
        (getSystemService(Context.AUDIO_SERVICE) as AudioManager).apply {
            nativeSetDefaultStreamValues(
                getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE).toInt(),
                getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER).toInt()
            )
        }
    }

    override fun onResume() {
        super.onResume()

        // Load the native engine
        nativeLoad(assets)
    }

    override fun onPause() {
        super.onPause()

        // Unload the native engine
        nativeUnload()
    }

    private external fun nativeLoad(assetManager: AssetManager)
    private external fun nativeUnload()
    private external fun nativeSetDefaultStreamValues(defaultSampleRate: Int, defaultFramesPerBurst: Int)
}