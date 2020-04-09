package com.jorkoh.polyrhythmtrainer

import androidx.preference.PreferenceManager
import com.jorkoh.polyrhythmtrainer.destinations.sounds.SoundsViewModel
import com.jorkoh.polyrhythmtrainer.destinations.trainer.TrainerViewModel
import com.jorkoh.polyrhythmtrainer.repositories.SoundsRepository
import com.jorkoh.polyrhythmtrainer.repositories.SoundsRepositoryImplementation
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

@FlowPreview
@ExperimentalCoroutinesApi
val appModule = module {

    single { FlowSharedPreferences(PreferenceManager.getDefaultSharedPreferences(androidContext())) }

    single<SoundsRepository> { SoundsRepositoryImplementation(get()) }

    viewModel { SoundsViewModel(get()) }
    viewModel { TrainerViewModel() }
    viewModel { MainActivityViewModel(get()) }
}