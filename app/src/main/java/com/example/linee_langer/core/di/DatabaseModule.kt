package com.example.linee_langer.core.di

import android.content.Context
import androidx.room.Room
import com.example.linee_langer.core.database.dao.NotificationDAO
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.data.local.NotificationRepository
import com.example.linee_langer.data.local.UserPreferencesManager
import com.example.linee_langer.core.database.AppDatabase
import com.example.linee_langer.core.database.dao.AnalysisDao
import com.example.linee_langer.domain.usecases.AnalyzeSkinUseCases
import com.example.linee_langer.domain.detector.ILangerDetector
import com.example.linee_langer.domain.detector.LangerDetector
import com.example.linee_langer.domain.usecases.UserUseCase
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
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "Langer_DB"
        )
            .fallbackToDestructiveMigration()
            //.addMigrations(MIGRATION_1_2) // fix per prod
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



}