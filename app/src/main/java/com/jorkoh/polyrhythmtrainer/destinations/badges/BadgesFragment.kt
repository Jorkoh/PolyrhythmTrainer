package com.jorkoh.polyrhythmtrainer.destinations.badges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jorkoh.polyrhythmtrainer.R
import org.koin.android.viewmodel.ext.android.viewModel

class BadgesFragment : Fragment() {

    private val badgesViewModel: BadgesViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.badges_fragment, container, false)
    }
}