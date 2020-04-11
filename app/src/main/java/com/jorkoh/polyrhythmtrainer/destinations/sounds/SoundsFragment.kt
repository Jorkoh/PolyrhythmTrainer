package com.jorkoh.polyrhythmtrainer.destinations.sounds

import android.os.Bundle
import android.transition.*
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.destinations.FAST_OUT_SLOW_IN
import com.jorkoh.polyrhythmtrainer.destinations.plusAssign
import com.jorkoh.polyrhythmtrainer.destinations.transitionTogether
import kotlinx.android.synthetic.main.sounds_fragment.*
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit

class SoundsFragment : Fragment() {

    companion object {
        const val TRANSITION_NAME_LEFT_PAD = "sounds_left_pad"
        const val TRANSITION_NAME_RIGHT_PAD = "sounds_right_pad"
    }

    private val soundsViewModel: SoundsViewModel by viewModel()

    private val soundsAdapter = SoundAdapter { soundId, padPosition ->
        soundsViewModel.changeSelectedSound(soundId, padPosition)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Non-shared elements when we are entering the sounds screen
        enterTransition = Slide(Gravity.BOTTOM).apply {
            startDelay = 110
            duration = 160
            interpolator = FAST_OUT_SLOW_IN
            mode = Slide.MODE_IN
        }

        // Non-shared elements when we are backing from the sounds screen
        returnTransition = transitionTogether {
            this += Slide(Gravity.BOTTOM).apply {
                duration = 170
                mode = Slide.MODE_OUT
            }
        }

        // Shared elements when we are entering the sounds screen
        sharedElementEnterTransition = transitionTogether {
            duration = 148
            startDelay = 88
            interpolator = FAST_OUT_SLOW_IN
            this += ChangeImageTransform()
            this += ChangeClipBounds()
            this += ChangeBounds()
            this += ChangeTransform()
        }

        // Shared elements when we are backing from the sounds screen
        sharedElementReturnTransition = transitionTogether {
            duration = 240
            interpolator = FAST_OUT_SLOW_IN
            this += ChangeImageTransform()
            this += ChangeClipBounds()
            this += ChangeBounds()
            this += ChangeTransform()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        postponeEnterTransition(400L, TimeUnit.MILLISECONDS)
        return inflater.inflate(R.layout.sounds_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sounds_recycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = soundsAdapter
            (itemAnimator as? DefaultItemAnimator)?.supportsChangeAnimations = false
        }

        soundsViewModel.sounds.observe(viewLifecycleOwner, Observer { sounds ->
            soundsAdapter.setNewSounds(sounds)
            sounds_recycler.doOnNextLayout {
                startPostponedEnterTransition()
            }
        })

        var leftPadSoundSet = false
        var rightPadSoundSet = false

        soundsViewModel.leftPadSound.observe(viewLifecycleOwner, Observer {
            if (leftPadSoundSet) {
                sounds_left_pad.ripple()
            }
            leftPadSoundSet = true
        })
        soundsViewModel.rightPadSound.observe(viewLifecycleOwner, Observer {
            if (rightPadSoundSet) {
                sounds_right_pad.ripple()
            }
            rightPadSoundSet = true
        })

        ViewCompat.setTransitionName(sounds_left_pad, TRANSITION_NAME_LEFT_PAD)
        ViewCompat.setTransitionName(sounds_right_pad, TRANSITION_NAME_RIGHT_PAD)
    }
}