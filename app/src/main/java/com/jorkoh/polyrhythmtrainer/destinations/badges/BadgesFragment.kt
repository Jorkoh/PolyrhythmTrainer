package com.jorkoh.polyrhythmtrainer.destinations.badges

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Slide
import com.jorkoh.polyrhythmtrainer.R
import com.jorkoh.polyrhythmtrainer.destinations.FAST_OUT_SLOW_IN
import com.jorkoh.polyrhythmtrainer.destinations.applyLoopingAnimatedVectorDrawable
import com.jorkoh.polyrhythmtrainer.destinations.plusAssign
import com.jorkoh.polyrhythmtrainer.destinations.transitionTogether
import kotlinx.android.synthetic.main.badges_fragment.*
import org.koin.android.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit

class BadgesFragment : Fragment() {

    private val badgesViewModel: BadgesViewModel by viewModel()

    private val badgesAdapter = BadgeAdapter(
            { Toast.makeText(requireContext(), "impossible trophy explanation", Toast.LENGTH_LONG).show() },
            { Toast.makeText(requireContext(), "normal modes trophy explanation", Toast.LENGTH_LONG).show() }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Non-shared elements when we are entering the badges screen
        enterTransition = Slide(Gravity.BOTTOM).apply {
            startDelay = 100
            duration = 250
            interpolator = FAST_OUT_SLOW_IN
            mode = Slide.MODE_IN
        }

        // Non-shared elements when we are backing from the badges screen
        returnTransition = transitionTogether {
            this += Slide(Gravity.BOTTOM).apply {
                duration = 230
                interpolator = FAST_OUT_SLOW_IN
                mode = Slide.MODE_OUT
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        postponeEnterTransition(400L, TimeUnit.MILLISECONDS)
        return inflater.inflate(R.layout.badges_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        badges_recycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = badgesAdapter
        }

        badgesViewModel.badgesGroupedByPolyrhythm.observe(viewLifecycleOwner, Observer { newBadgeGroups ->
            badgesAdapter.badgeGroups = newBadgeGroups
            if (newBadgeGroups.isEmpty()) {
                badges_empty_animation.visibility = View.VISIBLE
                badges_empty_text.visibility = View.VISIBLE
                badges_empty_animation.applyLoopingAnimatedVectorDrawable(R.drawable.badges_empty_animation)
            } else {
                badges_empty_animation.visibility = View.INVISIBLE
                badges_empty_text.visibility = View.INVISIBLE
            }
            badges_recycler.doOnNextLayout {
                startPostponedEnterTransition()
            }
        })
    }
}