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

        mainActivityViewModel.leftPadSound.observe(this, Observer { sound ->
            lifecycleScope.launchWhenResumed {
                nativeSetSoundAssets(
                    sound.resourceName,
                    PadPosition.Left.nativeValue,
                    shouldMakeSound(PadPosition.Left)
                )
            }
        })
        mainActivityViewModel.rightPadSound.observe(this, Observer { sound ->
            lifecycleScope.launchWhenResumed {
                nativeSetSoundAssets(
                    sound.resourceName,
                    PadPosition.Right.nativeValue,
                    shouldMakeSound(PadPosition.Right)
                )
            }
        })
    }

    private var leftPadSoundSet = false
    private var rightPadSoundSet = false

    private fun shouldMakeSound(padPosition: PadPosition): Boolean {
        var result = false
        when (padPosition) {
            PadPosition.Left -> {
                result = leftPadSoundSet
                leftPadSoundSet = true
            }
            PadPosition.Right -> {
                result = rightPadSoundSet
                rightPadSoundSet = true
            }
        }
        return result
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