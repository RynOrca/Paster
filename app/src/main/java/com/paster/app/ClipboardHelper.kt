package com.paster.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ClipboardHelper {

    fun copyBitmap(context: Context, bitmap: Bitmap) {
        val dir = File(context.cacheDir, "screenshots").also { it.mkdirs() }

        // Clean up files older than 1 hour
        val cutoff = System.currentTimeMillis() - 3600_000
        dir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }

        val file = File(dir, "paster_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file)

        val clip = ClipData.newUri(context.contentResolver, "Screenshot", uri)

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }
}
