package com.jorkoh.polyrhythmtrainer

import android.content.Context
import android.content.res.AssetManager
import android.media.AudioManager
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setDefaultStreamValues(this)
    }

    override fun onResume() {
        super.onResume()
        native_onStart(assets)
    }

    override fun onPause() {
        super.onPause()
        native_onStop()
    }

    fun setDefaultStreamValues(context: Context) {
        val myAudioMgr = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val sampleRateStr = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val defaultSampleRate = sampleRateStr.toInt()
        val framesPerBurstStr = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        val defaultFramesPerBurst = framesPerBurstStr.toInt()
        native_setDefaultStreamValues(defaultSampleRate, defaultFramesPerBurst)
    }

    /**
     * Native methods that are implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    private external fun native_onStart(assetManager: AssetManager)
    private external fun native_onStop()
    private external fun native_setDefaultStreamValues(
        defaultSampleRate: Int,
        defaultFramesPerBurst: Int
    )

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}