package com.example.linee_langer.ui.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.linee_langer.dao.NotificationDAO
import com.example.linee_langer.db.NotificationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    application: Application,
    private val dao: NotificationDAO
) : AndroidViewModel(application) {

    // quando dovrà poi eliminare le cose: private val repository = NotificationRepository(AppDatabase.getDatabase(application).notificationDAO())

    val notifications = dao.getAllNotifications()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val hasUnread = notifications.map { list ->
        list.any { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    // 2. Stato per il numero (Int)
    val unreadCount = notifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)


    /**
     * Segna tutte le notifiche come lette
     */
    fun markAllAsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.markAllAsRead()
        }
    }

    fun deleteNotification(notification: NotificationItem) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteNotificationById(notification.id)
        }
    }

    fun sendAnalysisSuccessNotification() {
        viewModelScope.launch(Dispatchers.IO) {
            val newNotification = NotificationItem(
                title = "Analisi Completata",
                description = "L'analisi delle linee di Langer è stata salvata con successo nel tuo storico.",
                timestamp = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                isRead = false
            )
            dao.insertNotification(newNotification)
        }

    }


}