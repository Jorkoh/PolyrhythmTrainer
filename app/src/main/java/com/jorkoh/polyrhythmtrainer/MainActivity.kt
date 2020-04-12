package com.jorkoh.polyrhythmtrainer

import android.content.Context
import android.content.res.AssetManager
import android.media.AudioManager
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.jorkoh.polyrhythmtrainer.destinations.PadPosition
import org.koin.android.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val mainActivityViewModel: MainActivityViewModel by viewModel()

    private lateinit var audioManager: AudioManager

    init {
        System.loadLibrary("native-lib")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Set default stream values
        audioManager.apply {
            nativeSetDefaultStreamValues(
                getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE).toInt(),
                getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER).toInt()
            )
        }

        var leftPadSoundSet = false
        var rightPadSoundSet = false

        mainActivityViewModel.leftPadSound.observe(this, Observer { sound ->
            lifecycleScope.launchWhenResumed {
                nativeSetSoundAssets(
                    sound.resourceName,
                    PadPosition.Left.nativeValue,
                    leftPadSoundSet
                )
                leftPadSoundSet = true
            }
        })
        mainActivityViewModel.rightPadSound.observe(this, Observer { sound ->
            lifecycleScope.launchWhenResumed {
                nativeSetSoundAssets(
                    sound.resourceName,
                    PadPosition.Right.nativeValue,
                    rightPadSoundSet
                )
                rightPadSoundSet = true
            }
        })
    }

    override fun onStart() {
        super.onStart()

        // Load the native engine
        nativeLoad(assets)
        mainActivityViewModel.leftPadSound.value?.let { sound ->
            nativeSetSoundAssets(sound.resourceName, PadPosition.Left.nativeValue, false)
        }
        mainActivityViewModel.rightPadSound.value?.let { sound ->
            nativeSetSoundAssets(sound.resourceName, PadPosition.Right.nativeValue, false)
        }
    }

    override fun onStop() {
        super.onStop()

        // Unload the native engine
        nativeUnload()
    }

    private external fun nativeLoad(assetManager: AssetManager)
    private external fun nativeUnload()
    private external fun nativeSetDefaultStreamValues(defaultSampleRate: Int, defaultFramesPerBurst: Int)
    private external fun nativeSetSoundAssets(newLeftPadSound: String, padPosition: Int, withAudioFeedback: Boolean)
}