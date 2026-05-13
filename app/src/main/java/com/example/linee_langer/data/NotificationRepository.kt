package com.example.linee_langer.data

import com.example.linee_langer.dao.NotificationDAO
import com.example.linee_langer.db.NotificationItem
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class NotificationRepository @Inject constructor(
    private val dao: NotificationDAO
) {

    val allNotifications: Flow<List<NotificationItem>> = dao.getAllNotifications()

    suspend fun addNotification(title: String, description: String) {
        val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        dao.insertNotification(
            NotificationItem(
                title = title,
                description = description,
                timestamp = timestamp,
            )
        )
    }


    suspend fun markAllAsRead() = dao.markAllAsRead()

    suspend fun deleteNotification(id: Int) = dao.deleteNotificationById(id)
}