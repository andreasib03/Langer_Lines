package com.example.linee_langer.db

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.linee_langer.dao.AnalysisDao
import com.example.linee_langer.dao.NotificationDAO

@Database(
    entities = [
        NotificationItem::class,
        SkinAnalysisEntry:: class,
        LangerLineEntity:: class], version = 6, exportSchema = true)
    abstract class AppDatabase : RoomDatabase() {
        abstract fun notificationDAO(): NotificationDAO
        abstract fun analysisDAO(): AnalysisDao

}
    @Entity(tableName = "notifications")
    data class NotificationItem(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        val title: String,
        val description: String,
        val timestamp: String,
        val isRead: Boolean = false
    )
