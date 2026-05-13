package com.example.linee_langer.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.linee_langer.data.AnalysisRepository
import com.example.linee_langer.data.NotificationRepository
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
        val lastDate = repository.getLastAnalysisDate() ?: 0L
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L

        if (System.currentTimeMillis() - lastDate > sevenDaysInMillis) {
            notificationRepo.addNotification(
                title = "Monitoraggio Pelle",
                description = "È passata una settimana dall'ultima analisi. Controlla la tua pelle oggi!"
            )
        }
        return Result.success()
    }
}