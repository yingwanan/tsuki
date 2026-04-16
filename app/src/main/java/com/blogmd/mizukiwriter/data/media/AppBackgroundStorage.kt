package com.blogmd.mizukiwriter.data.media

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File

class AppBackgroundStorage(
    private val filesDir: File,
) {
    private val backgroundDirectory = File(filesDir, "app-background").apply { mkdirs() }

    fun importBackground(
        sourceUri: Uri,
        resolver: ContentResolver,
    ): String {
        val extension = resolver.getType(sourceUri)
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?: sourceUri.lastPathSegment?.substringAfterLast('.', missingDelimiterValue = "bin")
            ?: "bin"
        val target = File(backgroundDirectory, "custom-background.${sanitizeAssetExtension(extension)}")
        backgroundDirectory.listFiles()?.forEach(File::delete)
        resolver.openInputStream(sourceUri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to open background image stream")
        return target.absolutePath
    }

    fun clearBackground(path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        if (file.parentFile?.canonicalPath == backgroundDirectory.canonicalPath) {
            file.delete()
        }
        if (backgroundDirectory.exists()) {
            backgroundDirectory.listFiles()?.forEach { child ->
                if (child.isFile) child.delete()
            }
        }
    }
}
