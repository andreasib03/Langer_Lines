package com.example.linee_langer.ui.shared.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import com.example.linee_langer.R

fun launchSupportEmail(
    context: Context,
    userSubject: String,
    userMessage: String,
) {
    val deviceModel = Build.MODEL
    val androidVersion = Build.VERSION.RELEASE
    val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    val technicalInfo = context.getString(
        R.string.support_technical_info_header,
        deviceModel,
        androidVersion,
        appVersion
    ).trimIndent()

    val fullSubject = context.getString(R.string.support_subject_prefix, userSubject)
    val supportEmail = context.getString(R.string.support_email_address)

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:".toUri()
        putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
        putExtra(Intent.EXTRA_SUBJECT, fullSubject)
        putExtra(Intent.EXTRA_TEXT, "$userMessage\n$technicalInfo")
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.error_no_email_client), Toast.LENGTH_SHORT).show()
    }
}