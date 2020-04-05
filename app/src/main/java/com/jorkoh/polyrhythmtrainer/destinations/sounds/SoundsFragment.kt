package com.jorkoh.polyrhythmtrainer.destinations.sounds

import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.ChangeClipBounds
import android.transition.ChangeImageTransform
import android.transition.ChangeTransform
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.destinations.*
import kotlinx.android.synthetic.main.fragment_sounds.*

class SoundsFragment : Fragment() {

    companion object {
        const val TRANSITION_NAME_LEFT_PAD = "sounds_left_pad"
        const val TRANSITION_NAME_RIGHT_PAD = "sounds_right_pad"
    }

    private val soundsViewModel: SoundsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = transitionTogether {
            this.duration = 148
            startDelay = 88
            interpolator = FAST_OUT_SLOW_IN
            this += ChangeImageTransform()
            this += ChangeClipBounds()
            this += ChangeBounds()
            this += ChangeTransform()
        }
        sharedElementReturnTransition = transitionTogether {
            this.duration = 240
            interpolator = FAST_OUT_SLOW_IN
            this += ChangeImageTransform()
            this += ChangeClipBounds()
            this += ChangeBounds()
            this += ChangeTransform()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sounds, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setTransitionName(sounds_left_pad, TRANSITION_NAME_LEFT_PAD)
        ViewCompat.setTransitionName(sounds_right_pad, TRANSITION_NAME_RIGHT_PAD)
    }
}