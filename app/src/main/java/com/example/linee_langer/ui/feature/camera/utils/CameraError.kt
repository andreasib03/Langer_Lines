package com.example.linee_langer.ui.feature.camera.utils

sealed class CameraError {
    object SaveFailed : CameraError()
    object GalleryAnalysisFailed : CameraError()
    data class Generic(val message: String) : CameraError()  // generico
}