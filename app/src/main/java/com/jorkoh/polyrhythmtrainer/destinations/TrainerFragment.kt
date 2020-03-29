package com.jorkoh.polyrhythmtrainer.destinations

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.jorkoh.polyrhythmtrainer.R
import kotlinx.android.synthetic.main.fragment_trainer.*
import kotlinx.android.synthetic.main.fragment_trainer.view.*

class TrainerFragment : Fragment() {

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
        // Load the native engine
        nativeLoad(requireContext().assets)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trainer, container, false).apply {
            left_pad.doOnTapResult {
                Log.d("TESTING", "Received left pad tap result ${it.name}")
            }
            right_pad.doOnTapResult {
                Log.d("TESTING", "Received right pad tap result ${it.name}")
            }

            change_theme_button.setOnClickListener(DebounceClickListener {
                changeThemePreference()
            })

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

            play_button.setOnClickListener {
                polyrhythmVisualizer.play()
            }

            change_theme_button.icon = ContextCompat.getDrawable(
                requireContext(), when (getCurrentNightMode()) {
                    Configuration.UI_MODE_NIGHT_YES -> R.drawable.ic_light_theme
                    Configuration.UI_MODE_NIGHT_NO -> R.drawable.ic_dark_theme
                    else -> R.drawable.ic_light_theme
                }
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO temp until level system is implemented
        current_level_text.text = getString(R.string.current_level, 1)

        trainerViewModel.getPolyrhythmSettings().observe(viewLifecycleOwner, Observer { newSettings ->
            x_number_of_beats_text.text = newSettings.xNumberOfBeats.toString()
            y_number_of_beats_text.text = newSettings.yNumberOfBeats.toString()
            polyrhythmVisualizer.polyrhythmSettings = newSettings.copy()
            nativeChangeRhythmSettings(newSettings.xNumberOfBeats, newSettings.yNumberOfBeats, newSettings.BPM)
        })
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

    override fun onDestroy() {
        super.onDestroy()

        nativeUnload()
    }

    private external fun nativeLoad(assetManager: AssetManager)
    private external fun nativeUnload()
    private external fun nativeSetDefaultStreamValues(defaultSampleRate: Int, defaultFramesPerBurst: Int)
    private external fun nativeChangeRhythmSettings(newXNumberOfBeats: Int, newYNumberOfBeats: Int, newBPM: Int)
}