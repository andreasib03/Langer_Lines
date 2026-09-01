package com.example.linee_langer.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.linee_langer.data.local.AnalysisRepository
import com.example.linee_langer.data.remote.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val analysisRepository: AnalysisRepository,
    private val authRepository: AuthRepository
) {

    fun scheduleFullSync(forceIfPending: Boolean = false){
        val uid = authRepository.currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            analysisRepository.resetFailedAnalyses(uid)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            val policy = if (forceIfPending) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP

            WorkManager.getInstance(context)
                .beginUniqueWork("DataUploadSync", policy, uploadRequest)
                .then(syncRequest)
                .enqueue()
        }

    }
}