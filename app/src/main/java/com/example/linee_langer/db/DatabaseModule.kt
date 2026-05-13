package com.example.linee_langer.db

import android.content.Context
import com.example.linee_langer.dao.AnalysisDao
import com.example.linee_langer.dao.NotificationDAO
import com.example.linee_langer.data.AnalysisRepository
import com.example.linee_langer.data.AuthRepository
import com.example.linee_langer.data.NotificationRepository
import com.example.linee_langer.data.UserPreferencesManager
import com.example.linee_langer.domain.usecases.AnalyzeSkinUseCases
import com.example.linee_langer.logic.LangerDetector
import com.example.linee_langer.logic.UserUseCase

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "Langer_DB"
        )
            .fallbackToDestructiveMigration() // Usalo con cautela in produzione
            .build()
    }

    @Provides
    fun provideNotificationDao(database: AppDatabase): NotificationDAO {
        return database.notificationDAO()
    }

    @Provides
    fun provideAnalysisDao(database: AppDatabase): AnalysisDao {
        return database.analysisDAO()
    }

    @Provides
    @Singleton
    fun provideLangerDetector(): LangerDetector {
        return LangerDetector()
    }

    @Provides
    fun provideAnalyzeSkinUseCases(detector: LangerDetector): AnalyzeSkinUseCases {
        return AnalyzeSkinUseCases(detector)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(dao: NotificationDAO): NotificationRepository {
        return NotificationRepository(dao)
    }

    @Provides
    @Singleton
    fun provideAnalysisRepository(dao: AnalysisDao): AnalysisRepository {
        return AnalysisRepository(dao)
    }

    @Provides
    @Singleton
    fun provideUserUseCase(
        authRepository: AuthRepository,
        analysisRepository: AnalysisRepository,
        userPreferencesManager: UserPreferencesManager
    ): UserUseCase {
        return UserUseCase(authRepository, analysisRepository, userPreferencesManager)
    }



}