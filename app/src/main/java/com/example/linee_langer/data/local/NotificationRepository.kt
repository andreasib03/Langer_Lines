package com.example.linee_langer.data.local

import com.example.linee_langer.core.database.entity.NotificationItem
import com.example.linee_langer.core.database.dao.NotificationDAO
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class NotificationRepository @Inject constructor(
    private val dao: NotificationDAO
) {

    val allNotifications: Flow<List<NotificationItem>> = dao.getAllNotifications()

    suspend fun addNotification(title: String, description: String, targetRoute: String? = null) {
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        dao.insertNotification(
            NotificationItem(
                title = title,
                description = description,
                timestamp = timestamp,
                targetRoute = targetRoute
            )
        )
    }

    suspend fun hasRecentReminderNotification(
        windowMs: Long,
        title: String
    ): Boolean {
        val sinceMs = System.currentTimeMillis() - windowMs
        return dao.hasRecentNotificationWithTitle(sinceMs = sinceMs, reminderTitle = title)
    }


    suspend fun markAllAsRead() = dao.markAllAsRead()

    suspend fun markAsRead(id: Int) = dao.markAsRead(id)

    suspend fun deleteNotification(id: Int) = dao.deleteNotificationById(id)

    suspend fun deleteAllNotifications() = dao.deleteAllNotification()
}