package com.example.linee_langer.core.di

import com.example.linee_langer.core.database.dao.AnalysisDao
import com.example.linee_langer.core.database.dao.NotificationDAO
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.local.NotificationRepository
import com.example.linee_langer.data.remote.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideNotificationRepository(dao: NotificationDAO): NotificationRepository {
        return NotificationRepository(dao)
    }

    @Provides
    @Singleton
    fun provideAnalysisRepository(dao: AnalysisDao, authRepository: AuthRepository, firebaseRepo: com.example.linee_langer.data.remote.FirebaseRepository): AnalysisRepository {
        return AnalysisRepository(dao, authRepository, firebaseRepo)
    }
}