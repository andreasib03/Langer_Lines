package com.example.linee_langer.core.di

import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.local.UserPreferencesManager
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.data.remote.FirebaseRepository
import com.example.linee_langer.domain.detector.ILangerDetector
import com.example.linee_langer.domain.detector.LangerDetector
import com.example.linee_langer.domain.usecases.AnalyzeSkinUseCases
import com.example.linee_langer.domain.usecases.UserUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    @Singleton
    fun provideLangerDetector(): ILangerDetector {
        return LangerDetector()
    }

    @Singleton
    @Provides
    fun provideAnalyzeSkinUseCases(detector: ILangerDetector): AnalyzeSkinUseCases {
        return AnalyzeSkinUseCases(detector)
    }


    @Provides
    @Singleton
    fun provideUserUseCase(
        authRepository: AuthRepository,
        analysisRepository: AnalysisRepository,
        userPreferencesManager: UserPreferencesManager,
        firebaseRepository: FirebaseRepository
    ): UserUseCase {
        return UserUseCase(authRepository, analysisRepository, firebaseRepository, userPreferencesManager)
    }
}
