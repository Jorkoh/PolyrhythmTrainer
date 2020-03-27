package com.jorkoh.polyrhythmtrainer.ui

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.jorkoh.polyrhythmtrainer.R
import kotlinx.android.synthetic.main.fragment_trainer.view.*


class TrainerFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trainer, container, false).apply {
            left_pad.doOnTapResult {
                Log.d("TESTING", "Received left pad tap result ${it.name}")
            }
            right_pad.doOnTapResult {
                Log.d("TESTING", "Received right pad tap result ${it.name}")
            }
            change_theme_button.setOnClickListener {
                changeThemePreference()
            }

            change_theme_button.icon = ContextCompat.getDrawable(requireContext(), when(getCurrentNightMode()){
                Configuration.UI_MODE_NIGHT_YES -> R.drawable.ic_light_theme
                Configuration.UI_MODE_NIGHT_NO -> R.drawable.ic_dark_theme
                else -> R.drawable.ic_light_theme
            })
        }
    }

    private fun changeThemePreference() {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

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
}