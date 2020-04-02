package com.jorkoh.polyrhythmtrainer.destinations

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.destinations.customviews.PolyrhythmVisualizer
import kotlinx.android.synthetic.main.fragment_trainer.*

@ExperimentalStdlibApi
class TrainerFragment : Fragment() {

    companion object {
        const val TAP_INTERVALS_MAX_SIZE = 6
    }

    private var lastTapTime = 0L
    private var tapIntervals = mutableListOf<Long>()

    private val trainerViewModel: TrainerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set default stream values
        (requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager).apply {
            nativeSetDefaultStreamValues(
                    getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE).toInt(),
                    getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER).toInt()
            )
        }
    }

    override fun onResume() {
        super.onResume()

        // Load the native engine and set the values
        nativeLoad(requireContext().assets)
        trainerViewModel.getPolyrhythmSettings().value?.let { settings ->
            nativeSetRhythmSettings(settings.xNumberOfBeats, settings.yNumberOfBeats, settings.BPM)
        }
    }

    override fun onStop() {
        super.onStop()

        // Unload the native engine
        nativeUnload()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trainer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Change them button
        change_theme_button.setOnClickListener(DebounceClickListener {
            changeThemePreference()
        })
        // Change beats per rhythm line buttons
        x_number_of_beats_increase_button.setOnClickListener {
            trainerViewModel.changeNumberOfBeats(true, RhythmLine.X)
        }
        x_number_of_beats_decrease_button.setOnClickListener {
            trainerViewModel.changeNumberOfBeats(false, RhythmLine.X)
        }
        y_number_of_beats_increase_button.setOnClickListener {
            trainerViewModel.changeNumberOfBeats(true, RhythmLine.Y)
        }
        y_number_of_beats_decrease_button.setOnClickListener {
            trainerViewModel.changeNumberOfBeats(false, RhythmLine.Y)
        }
        // BPM stuff
        bpm_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                trainerViewModel.changeBPM(progress + MIN_BPM)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        bpm_tap_button.setOnClickListener {
            calculateBPMFromTaps()
        }
        // Next state button
        play_stop_button.setOnClickListener {
            polyrhythm_visualizer.advanceToNextState()
        }

        // Style theme icon
        change_theme_button.icon = ContextCompat.getDrawable(
                requireContext(), when (getCurrentNightMode()) {
            Configuration.UI_MODE_NIGHT_YES -> R.drawable.ic_light_theme
            Configuration.UI_MODE_NIGHT_NO -> R.drawable.ic_dark_theme
            else -> R.drawable.ic_light_theme
        })

        polyrhythm_visualizer.doOnStatusChange { newStatus ->
            // TODO fix this being called too early when playing and rotating twice
            setPlayPauseButtonIcon(newStatus)
        }
        // TODO temp until level system is implemented
        current_level_text.text = getString(R.string.current_level, 1)

        trainerViewModel.getPolyrhythmSettings().observe(viewLifecycleOwner, Observer { newSettings ->
            x_number_of_beats_text.text = newSettings.xNumberOfBeats.toString()
            y_number_of_beats_text.text = newSettings.yNumberOfBeats.toString()
            bpm_tap_button.text = getString(R.string.bpm, newSettings.BPM.toString().padStart(3, ' '))
            polyrhythm_visualizer.polyrhythmSettings = newSettings.copy()

            lifecycleScope.launchWhenResumed {
                nativeSetRhythmSettings(newSettings.xNumberOfBeats, newSettings.yNumberOfBeats, newSettings.BPM)
            }
        })
    }

    private fun setPlayPauseButtonIcon(newStatus: PolyrhythmVisualizer.Status) {
        play_stop_button.icon = ContextCompat.getDrawable(requireContext(), when (newStatus) {
            PolyrhythmVisualizer.Status.BEFORE_PLAY -> R.drawable.ic_pause
            PolyrhythmVisualizer.Status.PLAYING -> R.drawable.ic_play
            PolyrhythmVisualizer.Status.AFTER_PLAY -> R.drawable.ic_replay
        })
    }

    private fun calculateBPMFromTaps() {
        val now = System.currentTimeMillis()
        val tapInterval = now - lastTapTime
        lastTapTime = now

        if (lastTapTime == 0L) {
            // If this is the first tap just wait for the next one
            return
        }

        if (tapInterval > 2 * 60000 / MIN_BPM) {
            // The last tap was too long ago, let's get a fresh start
            tapIntervals.clear()
            return
        }

        tapIntervals.add(tapInterval)
        if (tapIntervals.size > TAP_INTERVALS_MAX_SIZE) {
            // Only take into account the last TAP_INTERVALS_MAX_SIZE taps
            tapIntervals.removeFirst()
        }

        // Calculate the actual BPM
        val newBPM = 60000 / tapIntervals.average().toInt()
        if (trainerViewModel.changeBPM(newBPM)) {
            // If its a valid BPM let's also change the BPM bar
            bpm_bar.progress = newBPM - MIN_BPM
        }
    }

    private fun changeThemePreference() {
        val sharedPreferences: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(requireContext())

        val newThemePreference = when (getCurrentNightMode()) {
            Configuration.UI_MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_NO
            Configuration.UI_MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_YES
        }

        AppCompatDelegate.setDefaultNightMode(newThemePreference)
        sharedPreferences.edit(commit = true) {
            putInt("themePreference", newThemePreference)
        }
    }

    private fun getCurrentNightMode(): Int {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    }

    private external fun nativeLoad(assetManager: AssetManager)
    private external fun nativeUnload()
    private external fun nativeSetDefaultStreamValues(defaultSampleRate: Int, defaultFramesPerBurst: Int)

    private external fun nativeSetRhythmSettings(newXNumberOfBeats: Int, newYNumberOfBeats: Int, newBPM: Int)
}