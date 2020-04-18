package com.jorkoh.polyrhythmtrainer

import androidx.preference.PreferenceManager
import com.jorkoh.polyrhythmtrainer.destinations.sounds.SoundsViewModel
import com.jorkoh.polyrhythmtrainer.destinations.trainer.TrainerViewModel
import com.jorkoh.polyrhythmtrainer.repositories.TrainerSettingsRepository
import com.jorkoh.polyrhythmtrainer.repositories.TrainerSettingsRepositoryImplementation
import com.jorkoh.polyrhythmtrainer.repositories.SoundsRepository
import com.jorkoh.polyrhythmtrainer.repositories.SoundsRepositoryImplementation
import com.tfcporciuncula.flow.FlowSharedPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { FlowSharedPreferences(PreferenceManager.getDefaultSharedPreferences(androidContext())) }

    single<SoundsRepository> { SoundsRepositoryImplementation(get()) }
    single<TrainerSettingsRepository> { TrainerSettingsRepositoryImplementation(get()) }

    viewModel { SoundsViewModel(get()) }
    viewModel { TrainerViewModel(get()) }
    viewModel { MainActivityViewModel(get()) }
}