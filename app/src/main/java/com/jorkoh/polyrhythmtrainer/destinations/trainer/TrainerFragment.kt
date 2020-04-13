package com.jorkoh.polyrhythmtrainer.destinations.trainer

import android.content.SharedPreferences
import android.content.res.Configuration
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
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
import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.destinations.DebounceClickListener
import com.jorkoh.polyrhythmtrainer.destinations.FAST_OUT_SLOW_IN
import com.jorkoh.polyrhythmtrainer.destinations.plusAssign
import com.jorkoh.polyrhythmtrainer.destinations.sounds.SoundsFragment
import com.jorkoh.polyrhythmtrainer.destinations.trainer.customviews.PolyrhythmVisualizer
import com.jorkoh.polyrhythmtrainer.destinations.transitionTogether
import kotlinx.android.synthetic.main.trainer_fragment.*
import org.koin.android.viewmodel.ext.android.viewModel

@ExperimentalStdlibApi
class TrainerFragment : Fragment() {

    companion object {
        const val TAP_INTERVALS_MAX_SIZE = 6

        const val TRANSITION_NAME_LEFT_PAD = "trainer_left_pad"
        const val TRANSITION_NAME_RIGHT_PAD = "trainer_right_pad"
    }

    private lateinit var audioManager: AudioManager

    private val deviceListener = DeviceListener()
    private var devicesInitialized = false

    // Tap for BPM stuff
    private var lastTapTime = 0L
    private var tapIntervals = mutableListOf<Long>()

    private val trainerViewModel: TrainerViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioManager = getSystemService(requireContext(), AudioManager::class.java) as AudioManager

        // Non-shared elements when we are navigating to the sounds screen
        exitTransition = transitionTogether {
            this += Slide(Gravity.TOP).apply {
                duration = 275
                interpolator = FAST_OUT_SLOW_IN
                mode = Slide.MODE_OUT
            }
        }

        // Non-shared elements when we are coming back from the sounds screen
        reenterTransition = Slide(Gravity.TOP).apply {
            startDelay = 48
            duration = 216
            interpolator = FAST_OUT_SLOW_IN
            mode = Slide.MODE_IN
        }
    }

    override fun onResume() {
        super.onResume()

        nativeRegisterVisualizer(trainer_polyrhythm_visualizer)
        trainerViewModel.getPolyrhythmSettings().value?.let { settings ->
            nativeSetRhythmSettings(settings.xNumberOfBeats, settings.yNumberOfBeats, settings.BPM)
        }

        devicesInitialized = false
        audioManager.registerAudioDeviceCallback(deviceListener, null)
    }

    override fun onPause() {
        super.onPause()

        nativeUnregisterVisualizer()
        trainer_polyrhythm_visualizer.stop()

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
                TrainerFragmentDirections.actionTrainerFragmentToSoundsFragment(),
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
            trainer_polyrhythm_visualizer.advanceToNextState()
        }

        // Style theme icon
        trainer_change_theme_button.icon = ContextCompat.getDrawable(
            requireContext(), when (getCurrentNightMode()) {
                Configuration.UI_MODE_NIGHT_YES -> R.drawable.ic_light_theme
                Configuration.UI_MODE_NIGHT_NO -> R.drawable.ic_dark_theme
                else -> R.drawable.ic_light_theme
            }
        )

        trainer_polyrhythm_visualizer.doOnStatusChange { newStatus ->
            // TODO fix this being called too early when playing and rotating twice
            setPlayPauseReplayButtonIcon(newStatus)
        }

        // TODO temp until level system is implemented
        ArrayAdapter.createFromResource(requireContext(), R.array.modes, R.layout.trainer_mode_spinner_item)
            .also { adapter ->
                // Specify the layout to use when the list of choices appears
                adapter.setDropDownViewResource(R.layout.trainer_mode_spinner_item)
                // Apply the adapter to the spinner
                trainer_mode_spinner.adapter = adapter
            }
        trainer_mode_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>) {            }

            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                Toast.makeText(requireContext(), parent.getItemAtPosition(position) as String, Toast.LENGTH_LONG).show()
            }
        }

        trainerViewModel.getPolyrhythmSettings()
            .observe(viewLifecycleOwner, Observer { newSettings ->
                trainer_x_number_of_beats_text.text = newSettings.xNumberOfBeats.toString()
                trainer_y_number_of_beats_text.text = newSettings.yNumberOfBeats.toString()
                trainer_bpm_tap_button.text =
                    getString(R.string.bpm, newSettings.BPM.toString().padStart(3, ' '))
                trainer_polyrhythm_visualizer.polyrhythmSettings = newSettings.copy()

                lifecycleScope.launchWhenResumed {
                    nativeSetRhythmSettings(
                        newSettings.xNumberOfBeats,
                        newSettings.yNumberOfBeats,
                        newSettings.BPM
                    )
                }
            })

        ViewCompat.setTransitionName(trainer_left_pad, TRANSITION_NAME_LEFT_PAD)
        ViewCompat.setTransitionName(trainer_right_pad, TRANSITION_NAME_RIGHT_PAD)
    }

    private fun setPlayPauseReplayButtonIcon(newStatus: PolyrhythmVisualizer.Status) {
        trainer_play_stop_button.icon = ContextCompat.getDrawable(
            requireContext(), when (newStatus) {
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
        if (trainerViewModel.changeBPM(newBPM)) {
            // If its a valid BPM let's also change the BPM bar
            trainer_bpm_bar.progress = newBPM - MIN_BPM
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

    inner class DeviceListener : AudioDeviceCallback() {

        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            // Ignore first call after registering the listener
            if (devicesInitialized) {
                trainer_polyrhythm_visualizer.stop()
            }
            devicesInitialized = true
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            trainer_polyrhythm_visualizer.stop()
        }
    }

    private external fun nativeRegisterVisualizer(visualizer: PolyrhythmVisualizer)
    private external fun nativeUnregisterVisualizer()
    private external fun nativeSetRhythmSettings(newXNumberOfBeats: Int, newYNumberOfBeats: Int, newBPM: Int)
}