package com.jorkoh.polyrhythmtrainer.destinations.trainer

import android.content.SharedPreferences
import android.content.res.Configuration
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.SeekBar
import android.widget.Spinner
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.transition.Slide
import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.db.Badge
import com.jorkoh.polyrhythmtrainer.destinations.DebounceClickListener
import com.jorkoh.polyrhythmtrainer.destinations.FAST_OUT_SLOW_IN
import com.jorkoh.polyrhythmtrainer.destinations.plusAssign
import com.jorkoh.polyrhythmtrainer.destinations.sounds.SoundsFragment
import com.jorkoh.polyrhythmtrainer.destinations.trainer.customviews.PolyrhythmVisualizer
import com.jorkoh.polyrhythmtrainer.destinations.transitionTogether
import com.jorkoh.polyrhythmtrainer.repositories.Mode
import com.jorkoh.polyrhythmtrainer.repositories.RhythmLine
import com.jorkoh.polyrhythmtrainer.repositories.TrainerSettingsRepositoryImplementation.Companion.DEFAULT_BPM
import com.jorkoh.polyrhythmtrainer.repositories.TrainerSettingsRepositoryImplementation.Companion.DEFAULT_X_NUMBER_OF_BEATS
import com.jorkoh.polyrhythmtrainer.repositories.TrainerSettingsRepositoryImplementation.Companion.DEFAULT_Y_NUMBER_OF_BEATS
import com.jorkoh.polyrhythmtrainer.repositories.TrainerSettingsRepositoryImplementation.Companion.MAX_NUMBER_OF_BEATS
import com.jorkoh.polyrhythmtrainer.repositories.TrainerSettingsRepositoryImplementation.Companion.MIN_BPM
import com.jorkoh.polyrhythmtrainer.repositories.TrainerSettingsRepositoryImplementation.Companion.MIN_NUMBER_OF_BEATS
import kotlinx.android.synthetic.main.trainer_fragment.*
import kotlinx.android.synthetic.main.trainer_mode_spinner_dropdown_item.view.*
import kotlinx.android.synthetic.main.trainer_mode_spinner_item.view.*
import kotlinx.android.synthetic.main.trainer_view.view.*
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.*

@ExperimentalStdlibApi
class TrainerFragment : Fragment() {

    companion object {
        const val TAP_INTERVALS_MAX_SIZE = 6

        const val TRANSITION_NAME_LEFT_PAD = "trainer_left_pad"
        const val TRANSITION_NAME_RIGHT_PAD = "trainer_right_pad"
    }

    private val trainerViewModel: TrainerViewModel by viewModel()

    private lateinit var audioManager: AudioManager

    private val deviceListener = DeviceListener()
    private var devicesInitialized = false

    private val spinnerAdapter = object : BaseAdapter() {

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val mode = getItem(position)
            val itemView = convertView
                    ?: layoutInflater.inflate(
                            R.layout.trainer_mode_spinner_dropdown_item,
                            parent,
                            false
                    )

            itemView.trainer_mode_spinner_dropdown_item_icon.setImageResource(mode.iconResource)
            itemView.trainer_mode_spinner_dropdown_item_text.text =
                    resources.getText(mode.displayNameResource)

            return itemView
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            // Why aren't you using position here? https://stackoverflow.com/a/40764217
            val mode = (parent as Spinner).selectedItem as Mode
            val itemView = convertView
                    ?: layoutInflater.inflate(R.layout.trainer_mode_spinner_item, parent, false)

            itemView.trainer_mode_spinner_item_text.text =
                    resources.getText(mode.displayNameResource)

            return itemView
        }

        override fun getItem(position: Int) = trainerViewModel.modes[position]

        override fun getItemId(position: Int) = getItem(position).modeId.toLong()

        override fun getCount() = trainerViewModel.modes.size
    }

    private val spinnerListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>) {}

        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
            trainerViewModel.changeMode(id.toInt())
        }
    }

    // Tap for BPM stuff
    private var lastTapTime = 0L
    private var tapIntervals = mutableListOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioManager = getSystemService(requireContext(), AudioManager::class.java) as AudioManager

        // Non-shared elements when we are navigating to another screen
        exitTransition = transitionTogether {
            this += Slide(Gravity.TOP).apply {
                duration = 275
                interpolator = FAST_OUT_SLOW_IN
                mode = Slide.MODE_OUT
                // Alpha value of views is not preserved during exit transition causing sudden change of color. Not an easy fix
                //  excludeTarget(R.id.trainer_view_top_layout, true)
            }
        }

        // Non-shared elements when we are coming back from another screen
        reenterTransition = Slide(Gravity.TOP).apply {
            startDelay = 50
            duration = 215
            interpolator = FAST_OUT_SLOW_IN
            mode = Slide.MODE_IN
        }
    }

    override fun onResume() {
        super.onResume()

        nativeRegisterVisualizer(trainer_view.trainer_view_polyrhythm_visualizer)
        nativeSetBpm(trainerViewModel.bpm.value ?: DEFAULT_BPM)
        nativeSetXNumberOfBeats(trainerViewModel.xNumberOfBeats.value ?: DEFAULT_X_NUMBER_OF_BEATS)
        nativeSetYNumberOfBeats(trainerViewModel.yNumberOfBeats.value ?: DEFAULT_Y_NUMBER_OF_BEATS)
        with(trainerViewModel.mode.value ?: Mode()) {
            nativeSetModeSettings(engineMeasures, playerMeasures, successWindow)
        }

        devicesInitialized = false
        audioManager.registerAudioDeviceCallback(deviceListener, null)
    }

    override fun onPause() {
        super.onPause()

        nativeUnregisterVisualizer()
        trainer_view.stop()

        audioManager.unregisterAudioDeviceCallback(deviceListener)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.trainer_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Change theme button
        trainer_change_theme_button.setOnClickListener(DebounceClickListener {
            changeThemePreference()
        })
        trainer_sounds_button.setOnClickListener {
            findNavController().navigate(
                    R.id.action_trainerFragment_to_soundsFragment,
                    null,
                    null,
                    FragmentNavigatorExtras(
                            trainer_left_pad to SoundsFragment.TRANSITION_NAME_LEFT_PAD,
                            trainer_right_pad to SoundsFragment.TRANSITION_NAME_RIGHT_PAD
                    )
            )
        }
        trainer_trophies_button.setOnClickListener {
            findNavController().navigate(R.id.action_trainerFragment_to_trophiesFragment)
        }

        // Change beats per rhythm line buttons
        trainer_x_number_of_beats_increase_button.setOnClickListener {
            trainerViewModel.changeNumberOfBeats(true, RhythmLine.X)
        }
        trainer_x_number_of_beats_decrease_button.setOnClickListener {
            trainerViewModel.changeNumberOfBeats(false, RhythmLine.X)
        }
        trainer_y_number_of_beats_increase_button.setOnClickListener {
            trainerViewModel.changeNumberOfBeats(true, RhythmLine.Y)
        }
        trainer_y_number_of_beats_decrease_button.setOnClickListener {
            trainerViewModel.changeNumberOfBeats(false, RhythmLine.Y)
        }
        // BPM stuff
        trainer_bpm_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                trainerViewModel.changeBPM(progress + MIN_BPM)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        trainer_bpm_tap_button.setOnClickListener {
            calculateBPMFromTaps()
        }
        // Next state button
        trainer_play_stop_button.setOnClickListener {
            trainer_view.advanceToNextState()
        }

        // Style theme icon
        trainer_change_theme_button.icon = ContextCompat.getDrawable(
                requireContext(),
                when (getCurrentNightMode()) {
                    Configuration.UI_MODE_NIGHT_YES -> R.drawable.ic_light_theme
                    Configuration.UI_MODE_NIGHT_NO -> R.drawable.ic_dark_theme
                    else -> R.drawable.ic_light_theme
                }
        )

        trainer_view.doOnStatusChange { newStatus ->
            setPlayPauseReplayButtonIcon(newStatus)
        }

        trainer_view.doOnExerciseEnd { success, xNumberOfBeats, yNumberOfBeats, bpm, mode, lastPlayedMeasure ->
            if (success) {
                trainerViewModel.addBadgeIfNeeded(
                        Badge(0, xNumberOfBeats, yNumberOfBeats, bpm, mode.modeId, lastPlayedMeasure, Date())
                )
            }
        }

        trainer_mode_spinner.adapter = spinnerAdapter
        trainer_mode_spinner.onItemSelectedListener = spinnerListener

        trainerViewModel.bpm.observe(viewLifecycleOwner, Observer { newBpm ->
            trainer_bpm_tap_button.text = getString(R.string.bpm, newBpm.toString().padStart(3, ' '))
            trainer_view.bpm = newBpm
            trainer_bpm_bar.progress = newBpm - MIN_BPM
            lifecycleScope.launchWhenResumed {
                nativeSetBpm(newBpm)
            }
        })

        trainerViewModel.xNumberOfBeats.observe(viewLifecycleOwner, Observer { newXNumberOfBeats ->
            trainer_x_number_of_beats_text.text = newXNumberOfBeats.toString()
            trainer_x_number_of_beats_decrease_button.isEnabled = newXNumberOfBeats > MIN_NUMBER_OF_BEATS
            trainer_x_number_of_beats_increase_button.isEnabled = newXNumberOfBeats < MAX_NUMBER_OF_BEATS
            trainer_view.xNumberOfBeats = newXNumberOfBeats
            lifecycleScope.launchWhenResumed {
                nativeSetXNumberOfBeats(newXNumberOfBeats)
            }
        })

        trainerViewModel.yNumberOfBeats.observe(viewLifecycleOwner, Observer { newYNumberOfBeats ->
            trainer_y_number_of_beats_text.text = newYNumberOfBeats.toString()
            trainer_y_number_of_beats_decrease_button.isEnabled = newYNumberOfBeats > MIN_NUMBER_OF_BEATS
            trainer_y_number_of_beats_increase_button.isEnabled = newYNumberOfBeats < MAX_NUMBER_OF_BEATS
            trainer_view.yNumberOfBeats = newYNumberOfBeats
            lifecycleScope.launchWhenResumed {
                nativeSetYNumberOfBeats(newYNumberOfBeats)
            }
        })

        trainerViewModel.mode.observe(viewLifecycleOwner, Observer { newMode ->
            trainer_mode_spinner.onItemSelectedListener = null
            trainer_mode_spinner.setSelection(
                    trainerViewModel.modes.indexOfFirst { it.modeId == newMode.modeId },
                    false
            )
            trainer_mode_spinner.onItemSelectedListener = spinnerListener
            trainer_view.mode = newMode
            lifecycleScope.launchWhenResumed {
                nativeSetModeSettings(
                        newMode.engineMeasures,
                        newMode.playerMeasures,
                        newMode.successWindow
                )
            }
        })

        ViewCompat.setTransitionName(trainer_left_pad, TRANSITION_NAME_LEFT_PAD)
        ViewCompat.setTransitionName(trainer_right_pad, TRANSITION_NAME_RIGHT_PAD)
    }

    private fun setPlayPauseReplayButtonIcon(newStatus: PolyrhythmVisualizer.Status) {
        trainer_play_stop_button.icon = ContextCompat.getDrawable(
                requireContext(),
                when (newStatus) {
                    PolyrhythmVisualizer.Status.BEFORE_PLAY -> R.drawable.ic_play
                    PolyrhythmVisualizer.Status.PLAYING -> R.drawable.ic_pause
                    PolyrhythmVisualizer.Status.AFTER_PLAY -> R.drawable.ic_replay
                }
        )
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
        trainerViewModel.changeBPM(newBPM)
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

    inner class DeviceListener : AudioDeviceCallback() {

        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            // Ignore first call after registering the listener
            if (devicesInitialized) {
                trainer_view.stop()
            }
            devicesInitialized = true
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            trainer_view.stop()
        }
    }

    private external fun nativeRegisterVisualizer(visualizer: PolyrhythmVisualizer)
    private external fun nativeUnregisterVisualizer()
    private external fun nativeSetBpm(newBpm: Int)
    private external fun nativeSetXNumberOfBeats(newXNumberOfBeats: Int)
    private external fun nativeSetYNumberOfBeats(newYNumberOfBeats: Int)
    private external fun nativeSetModeSettings(newEngineMeasures: Int, newPlayerMeasures: Int, successWindow: Float)
}