package com.jorkoh.polyrhythmtrainer.destinations.trophies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.jorkoh.polyrhythmtrainer.R

class TrophiesFragment : Fragment() {

    private val trophiesViewModel: TrophiesViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trophies, container, false)
    }
}