package io.github.sherifshabans.mushaf.sample.quran

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** The four share helpers the mushaf uses, from the app's `ShareImageUtils`. */
object ShareImageUtils {

    suspend fun saveBitmapToCache(context: Context, bitmap: Bitmap, name: String): Uri =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, "$name.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

    fun shareImageUri(context: Context, uri: Uri) = start(
        context,
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    )

    fun shareImageWithText(context: Context, uri: Uri, text: String) = start(
        context,
        Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    )

    fun shareText(context: Context, text: String) = start(
        context,
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    )

    private fun start(context: Context, intent: Intent) {
        context.startActivity(
            Intent.createChooser(intent, "مشاركة").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
