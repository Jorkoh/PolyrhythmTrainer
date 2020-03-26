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
        return inflater.inflate(R.layout.fragment_trainer, container, false)
    }


}