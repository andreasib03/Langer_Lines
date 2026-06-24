package com.example.linee_langer.ui.feature.notifications

sealed class NotificationType {
    object GalleryAnalysisComplete : NotificationType()
    data class LinesDetected(val count: Int) : NotificationType()
}