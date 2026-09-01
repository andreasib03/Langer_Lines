package com.example.linee_langer.core.di

import android.content.Context
import androidx.room.Room
import com.example.linee_langer.core.database.dao.NotificationDAO
import com.example.linee_langer.core.database.AppDatabase
import com.example.linee_langer.core.database.MigrationDatabases
import com.example.linee_langer.core.database.dao.AnalysisDao
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
            .addMigrations(
                MigrationDatabases.MIGRATION_1_2,
                MigrationDatabases.MIGRATION_6_7,
                MigrationDatabases.MIGRATION_7_8,
                MigrationDatabases.MIGRATION_8_9,
                MigrationDatabases.MIGRATION_9_10
            )
            .fallbackToDestructiveMigration()
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