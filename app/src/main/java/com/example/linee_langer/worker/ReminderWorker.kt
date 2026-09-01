package com.example.linee_langer.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.linee_langer.R
import com.example.linee_langer.core.utils.logCaughtException
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.local.NotificationRepository
import com.example.linee_langer.data.local.UserPreferencesManager
import com.example.linee_langer.data.remote.AuthRepository
import com.example.linee_langer.ui.navigation.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private const val TAG = "ReminderWorker"
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AnalysisRepository,
    private val notificationRepo: NotificationRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val notificationsEnabled = userPreferencesManager.isNotificationEnabled.first()
            if (!notificationsEnabled) return Result.success()

            val uid = authRepository.currentUser?.uid
            if (uid.isNullOrBlank()) return Result.success()

            val nowMs    = System.currentTimeMillis()
            val lastDate = repository.getLastAnalysisDate(uid) ?: return Result.success()

            val sevenDaysMs = TimeUnit.DAYS.toMillis(7)

            val daysSinceLast = (nowMs - lastDate) / sevenDaysMs
            if (daysSinceLast < 1L) return Result.success() // meno di 7 giorni

            val reminderTitle = applicationContext.getString(R.string.reminder_title)

            val recentReminderExists = notificationRepo
                .hasRecentReminderNotification(sevenDaysMs, reminderTitle)
            if (recentReminderExists) return Result.success()

            notificationRepo.addNotification(
                title       = reminderTitle,
                description = applicationContext.getString(R.string.reminder_body),
                targetRoute = Screen.Camera.route
            )

            Result.success()
        } catch (e: Exception) {
            logCaughtException(TAG, "Generazione reminder analisi fallita", e)
            Result.retry()
        }
    }
}