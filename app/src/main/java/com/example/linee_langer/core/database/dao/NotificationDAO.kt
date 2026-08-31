package com.example.linee_langer.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.linee_langer.core.database.entity.NotificationItem
import kotlinx.coroutines.flow.Flow
@Dao
interface NotificationDAO {
    @Query ("SELECT * FROM notifications ORDER BY id DESC")
    fun getAllNotifications(): Flow<List<NotificationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Int)

    @Query("""
        SELECT COUNT(*) > 0 FROM notifications
        WHERE insertedAtMs >= :sinceMs
          AND title = :reminderTitle
    """)
    suspend fun hasRecentNotificationWithTitle(sinceMs: Long, reminderTitle: String): Boolean

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotificationById(notificationId: Int)

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotification()
}