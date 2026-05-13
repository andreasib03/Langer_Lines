package com.example.linee_langer.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.example.linee_langer.dao.AnalysisDao

//handles saving/loading images to local storage

class ImageRepository() {



}

/* fun captureAndSaveImage(
        context: Context,
        imageCapture: ImageCapture,
        onImageSaved: (Uri) -> Unit
    ){
        val name = "Langer-${
            System.currentTimeMillis()}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/LangerAnalysis")
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    output.savedUri?.let { uri ->
                        Toast.makeText(context, "Photo saved", Toast.LENGTH_SHORT).show()
                        onImageSaved(uri)
                    }
                }

                override fun onError(error: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${error.message}", error)
                }
            }
        )*/