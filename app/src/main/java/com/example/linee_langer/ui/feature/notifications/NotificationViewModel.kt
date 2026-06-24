package com.example.linee_langer.ui.feature.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linee_langer.R
import com.example.linee_langer.core.database.entity.NotificationItem
import com.example.linee_langer.data.local.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val notifications = notificationRepository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasUnread = notifications.map { list ->
        list.any { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // 2. Stato per il numero (Int)
    val unreadCount = notifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)


    /**
     * Segna tutte le notifiche come lette
     */
    fun markAllAsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.markAllAsRead()
        }
    }

    fun deleteNotification(notification: NotificationItem) {
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.deleteNotification(notification.id)
        }
    }

    fun sendAnalysisSuccessNotification() {
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.addNotification(
                title = context.getString(R.string.notification_analysis_complete),
                description = context.getString(R.string.notification_analysis_body)
            )
        }

    }





}