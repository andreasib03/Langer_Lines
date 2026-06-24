package com.example.linee_langer.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.linee_langer.R
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.local.NotificationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject


@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AnalysisRepository,
    private val notificationRepo: NotificationRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val lastDate = repository.getLastAnalysisDate() ?: 0L
            val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L

            if (System.currentTimeMillis() - lastDate > sevenDaysInMillis) {
                notificationRepo.addNotification(
                    title = applicationContext.getString(R.string.reminder_title),
                    description = applicationContext.getString(R.string.reminder_body)
                )
            }
            return Result.success()
        } catch (e: Exception){
            Result.failure()
        }

    }
}