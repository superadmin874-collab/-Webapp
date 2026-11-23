package com.example.core

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class PickedFilePayload(
    val fileName: String,
    val fileType: String,
    val mimeType: String,
    val bytes: ByteArray
)

object DocumentStorageHelper {
    // 420 KB raw slice encodes to ~560 KB Base64 (< 800 KB security rule ceiling)
    const val CHUNK_RAW_BYTES = 420 * 1024

    val APPROVED_EXTENSIONS = setOf(
        "PDF", "JPG", "JPEG", "PNG", "WEBP",
        "DOC", "DOCX", "PPT", "PPTX", "XLS", "XLSX", "TXT"
    )

    suspend fun readPickedFile(context: Context, uri: Uri, maxSizeBytes: Long): Result<PickedFilePayload> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                var fileName = "study_material_${System.currentTimeMillis()}"
                var reportedSize = -1L

                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: fileName
                        if (sizeIdx >= 0) reportedSize = cursor.getLong(sizeIdx)
                    }
                }

                if (reportedSize > maxSizeBytes) {
                    return@withContext Result.failure(
                        IllegalArgumentException("File '$fileName' exceeds ${formatBytes(maxSizeBytes)} upload limit.")
                    )
                }

                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(IllegalStateException("Unable to read selected file."))

                if (bytes.isEmpty()) {
                    return@withContext Result.failure(IllegalArgumentException("Selected file '$fileName' is empty."))
                }
                if (bytes.size > maxSizeBytes) {
                    return@withContext Result.failure(
                        IllegalArgumentException("File '$fileName' (${formatBytes(bytes.size.toLong())}) exceeds ${formatBytes(maxSizeBytes)} limit.")
                    )
                }

                val ext = resolveExtension(fileName, mimeType)
                if (ext !in APPROVED_EXTENSIONS) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Unsupported format '.$ext'. Supported: ${APPROVED_EXTENSIONS.joinToString()}")
                    )
                }

                Result.success(
                    PickedFilePayload(
                        fileName = fileName,
                        fileType = ext,
                        mimeType = mimeType,
                        bytes = bytes
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun splitIntoBase64Chunks(bytes: ByteArray): List<String> {
        if (bytes.isEmpty()) return emptyList()
        val chunks = mutableListOf<String>()
        var offset = 0
        while (offset < bytes.size) {
            val end = minOf(offset + CHUNK_RAW_BYTES, bytes.size)
            val slice = bytes.copyOfRange(offset, end)
            chunks += Base64.encodeToString(slice, Base64.NO_WRAP)
            offset = end
        }
        return chunks
    }

    fun decodeBase64Chunks(chunks: List<String>): ByteArray {
        val decodedParts = chunks.map { Base64.decode(it, Base64.DEFAULT) }
        val total = decodedParts.sumOf { it.size }
        val output = ByteArray(total)
        var pos = 0
        for (part in decodedParts) {
            System.arraycopy(part, 0, output, pos, part.size)
            pos += part.size
        }
        return output
    }

    suspend fun saveToInternalDocs(
        context: Context,
        docId: String,
        fileName: String,
        bytes: ByteArray
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "darbar_docs")
        if (!dir.exists()) dir.mkdirs()
        val safeName = "${docId}_${fileName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_")}"
        val target = File(dir, safeName)
        target.writeBytes(bytes)
        target
    }

    suspend fun exportToPublicDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): String = withContext(Dispatchers.IO) {
        val cleanName = fileName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, cleanName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Darbar")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                return@withContext "Downloads/Darbar/$cleanName"
            }
        }
        val extDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Darbar")
        if (!extDir.exists()) extDir.mkdirs()
        val file = File(extDir, cleanName)
        file.writeBytes(bytes)
        file.absolutePath
    }

    fun openFileExternally(context: Context, file: File, mimeType: String): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun exportCsvAndShare(context: Context, fileName: String, csvContent: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.cacheDir, "darbar_docs")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                file.writeText(csvContent, Charsets.UTF_8)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Export CSV Report"))
                true
            } catch (_: Exception) {
                false
            }
        }

    fun resolveExtension(fileName: String, mimeType: String): String {
        val fromName = fileName.substringAfterLast('.', "").uppercase(Locale.US)
        if (fromName.isNotBlank() && fromName in APPROVED_EXTENSIONS) return fromName
        return when {
            mimeType.contains("pdf") -> "PDF"
            mimeType.contains("png") -> "PNG"
            mimeType.contains("webp") -> "WEBP"
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> "JPG"
            mimeType.contains("wordprocessingml") -> "DOCX"
            mimeType.contains("msword") -> "DOC"
            mimeType.contains("presentationml") -> "PPTX"
            mimeType.contains("powerpoint") -> "PPT"
            mimeType.contains("spreadsheetml") -> "XLSX"
            mimeType.contains("excel") -> "XLS"
            mimeType.contains("text/plain") -> "TXT"
            else -> fromName.ifBlank { "OTHER" }
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }
}
