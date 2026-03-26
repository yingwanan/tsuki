package com.blogmd.mizukiwriter.data.media

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.util.Locale

interface AssetStorageContract {
    fun importAsset(
        draftId: Long,
        sourceUri: Uri,
        resolver: ContentResolver,
        preferredBaseName: String? = null,
    ): String

    fun listAssetNames(draftId: Long): List<String>

    fun listAssetFiles(draftId: Long): List<File>

    fun deleteDraftAssets(draftId: Long)
}

class AssetStorage(
    private val filesDir: File,
) : AssetStorageContract {
    fun draftDirectory(draftId: Long): File = File(filesDir, "draft-assets/$draftId").apply { mkdirs() }

    fun listAssets(draftId: Long): List<File> = draftDirectory(draftId)
        .listFiles()
        ?.sortedBy { it.name }
        .orEmpty()

    override fun importAsset(
        draftId: Long,
        sourceUri: Uri,
        resolver: ContentResolver,
        preferredBaseName: String?,
    ): String {
        val extension = resolver.getType(sourceUri)
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?: sourceUri.lastPathSegment?.substringAfterLast('.', missingDelimiterValue = "bin")
            ?: "bin"
        val baseName = sanitize(preferredBaseName ?: sourceUri.lastPathSegment?.substringBeforeLast('.', "") ?: "asset")
        val directory = draftDirectory(draftId)
        val safeExtension = sanitizeAssetExtension(extension)
        var candidate = File(directory, "$baseName.$safeExtension")
        var suffix = 1
        while (candidate.exists()) {
            candidate = File(directory, "$baseName-$suffix.$safeExtension")
            suffix++
        }
        resolver.openInputStream(sourceUri)?.use { input ->
            candidate.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to open asset stream")
        return "./${candidate.name}"
    }

    override fun listAssetNames(draftId: Long): List<String> = listAssets(draftId).map { it.name }

    override fun listAssetFiles(draftId: Long): List<File> = listAssets(draftId)

    override fun deleteDraftAssets(draftId: Long) {
        draftDirectory(draftId).deleteRecursively()
    }

    private fun sanitize(value: String): String = value
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "asset" }
}

internal fun sanitizeAssetExtension(value: String): String = value
    .substringBefore('?')
    .substringBefore('#')
    .lowercase(Locale.US)
    .replace(Regex("[^a-z0-9]"), "")
    .ifBlank { "bin" }
