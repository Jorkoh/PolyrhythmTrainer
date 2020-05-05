package com.jorkoh.polyrhythmtrainer

import androidx.preference.PreferenceManager
import androidx.room.Room
import com.jorkoh.polyrhythmtrainer.db.AppDatabase
import com.jorkoh.polyrhythmtrainer.destinations.sounds.SoundsViewModel
import com.jorkoh.polyrhythmtrainer.destinations.trainer.TrainerViewModel
import com.jorkoh.polyrhythmtrainer.repositories.*
import com.tfcporciuncula.flow.FlowSharedPreferences
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Shared preferences
    single { FlowSharedPreferences(PreferenceManager.getDefaultSharedPreferences(androidContext())) }

    // Room stuff
    single { Room.databaseBuilder(androidApplication(), AppDatabase::class.java, AppDatabase.DATABASE_NAME).build() }
    single { get<AppDatabase>().badgesDao() }

    // Repositories
    single<BadgesRepository> { BadgesRepositoryImplementation(get()) }
    single<SoundsRepository> { SoundsRepositoryImplementation(get()) }
    single<TrainerSettingsRepository> { TrainerSettingsRepositoryImplementation(get()) }

    // ViewModels
    viewModel { SoundsViewModel(get()) }
    viewModel { TrainerViewModel(get(), get()) }
    viewModel { MainActivityViewModel(get()) }
}