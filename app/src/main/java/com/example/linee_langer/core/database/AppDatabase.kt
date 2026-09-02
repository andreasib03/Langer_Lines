package com.example.linee_langer.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.linee_langer.core.database.dao.AnalysisDao
import com.example.linee_langer.core.database.dao.NotificationDAO
import com.example.linee_langer.core.database.entity.LangerLineEntity
import com.example.linee_langer.core.database.entity.NotificationItem
import com.example.linee_langer.core.database.entity.SkinAnalysisEntity

@Database(
    entities = [
        NotificationItem::class,
        SkinAnalysisEntity:: class,
        LangerLineEntity:: class], version = 12, exportSchema = true)
    abstract class AppDatabase : RoomDatabase() {
        abstract fun notificationDAO(): NotificationDAO
        abstract fun analysisDAO(): AnalysisDao

}