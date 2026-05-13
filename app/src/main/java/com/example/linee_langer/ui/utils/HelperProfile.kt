package com.example.linee_langer.ui.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.print.PrintAttributes
import android.webkit.WebView
import android.webkit.WebViewClient
import android.print.PrintManager
import android.util.Base64
import android.util.Log
import com.example.linee_langer.R
import com.example.linee_langer.dao.AnalysisWithLines
import com.example.linee_langer.domain.models.UserFirebaseModel
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.joinToString
import androidx.core.net.toUri

fun exportDataAsPdf(context: Context, userData: UserFirebaseModel, analyses: List<AnalysisWithLines>, formattedSkinType: String) {
    val webView = WebView(context)

    // 1. Generiamo l'HTML con uno stile pulito
    val htmlContent = generateDataHtml(context, userData, analyses, formattedSkinType)

    // 2. Carichiamo l'HTML nella WebView
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)

    // 3. Quando la pagina è pronta, lanciamo la stampa
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "${context.getString(R.string.app_name)} Report"

            // Crea il documento PDF
            val printAdapter = webView.createPrintDocumentAdapter(jobName)

            // Apre l'interfaccia di sistema per salvare come PDF o stampare
            printManager.print(
                jobName,
                printAdapter,
                PrintAttributes.Builder().build()
            )
        }
    }
}

private fun generateDataHtml(context: Context, userData: UserFirebaseModel, analyses: List<AnalysisWithLines>, skinType: String): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // FIX: Accediamo a analysis.date e analysis.resultSummary
    val tableRows = analyses.joinToString("") { item ->
        val dateString = sdf.format(Date(item.analysis.date))

        val based64Image = getResizedImageBase64(context,item.analysis.imagePath)
        val imageHtml = if (based64Image != null){
            """<img src="data:image/jpeg;base64,$based64Image" style="width: 100px; height: auto; border-radius: 4px;"/>"""
        } else {
            "Immagine non disponibile"
        }
        """
        <tr>
            <td>$dateString</td>
            <td style="width: 30%;">$imageHtml</td>
            <td>${item.analysis.resultSummary}</td>
        </tr>
        """.trimIndent()
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: sans-serif; padding: 20px; color: #333; }
                .header { border-bottom: 2px solid #333; padding-bottom: 10px; margin-bottom: 20px; text-align: center; }
                .user-info { margin-bottom: 30px; background: #f4f4f4; padding: 15px; border-radius: 8px; border: 1px solid #ddd; }
                table { width: 100%; border-collapse: collapse; }
                th, td { border: 1px solid #ddd; padding: 12px; text-align: left; vertical-align: middle; }
                th { background-color: #333; color: white; }
                tr:nth-child(even) { background-color: #fafafa; }
                img { display: block; margin: 0 auto; }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>Report Analisi Langer</h1>
                <p>Generato il ${sdf.format(Date())}</p>
            </div>
            
            <div class="user-info">
                <h3 style="margin-top: 0;">Dati Utente</h3>
                <p><strong>Nome:</strong> ${userData.name}</p>
                <p><strong>Email:</strong> ${userData.email}</p>
                <p><strong>Tipo di Pelle:</strong> $skinType</p>
            </div>

            <h2>Storico Analisi con Immagini</h2>
            <table>
                <thead>
                    <tr>
                        <th>Data</th>
                        <th style="text-align: center;">Anteprima</th>
                        <th>Risultato</th>
                    </tr>
                </thead>
                <tbody>
                    $tableRows
                </tbody>
            </table>
        </body>
        </html>
    """.trimIndent()
}


private fun getResizedImageBase64(context: Context, path: String): String? {

    return try {
        val uri = path.toUri()
        val options = BitmapFactory.Options()

        // 1. Leggiamo solo le dimensioni senza caricare i pixel in RAM
        options.inJustDecodeBounds = true
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        // 2. Calcoliamo il fattore di campionamento (es. carica 1 pixel ogni 4 o 8)
        // Puntiamo a una larghezza di circa 400px per il PDF
        options.inSampleSize = calculateInSampleSize(options, 400, 400)
        options.inJustDecodeBounds = false

        // 3. Carichiamo la bitmap già rimpicciolita
        val resizedBitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        // 4. Compressione e Base64
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val bytes = outputStream.toByteArray()
        resizedBitmap.recycle() // Libera subito la RAM

        Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        Log.e("PDF_Export", "Errore ridimensionamento immagine: ${e.message}")
        null
    }
}

// Funzione di supporto per il calcolo del campionamento
private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    val componentName = intent?.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    context.startActivity(mainIntent)

    // Chiude il processo attuale per liberare la memoria
    Runtime.getRuntime().exit(0)
}