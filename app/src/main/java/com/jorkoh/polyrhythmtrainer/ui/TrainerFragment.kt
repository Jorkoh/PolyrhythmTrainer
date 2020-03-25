package com.jorkoh.polyrhythmtrainer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jorkoh.polyrhythmtrainer.R
import kotlinx.android.synthetic.main.fragment_trainer.view.*


class TrainerFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_trainer, container, false).apply {
            // TODO pads will be custom views
            this.left_pad.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        onPadTouch(0, event.eventTime)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        performClick()
                        true
                    }
                    else -> false
                }
            }
            this.right_pad.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        onPadTouch(1, event.eventTime)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        performClick()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private external fun onPadTouch(padPosition: Int, timeSinceBoot: Long)
}