package com.jorkoh.polyrhythmtrainer

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager


class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Apply day-night mode, dark mode by default
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        AppCompatDelegate.setDefaultNightMode(
            sharedPreferences.getInt(
                "themePreference",
                AppCompatDelegate.MODE_NIGHT_YES
            )
        )
    }
}