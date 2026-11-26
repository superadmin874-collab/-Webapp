package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class PickedFilePayload(
    val fileName: String,
    val mimeType: String,
    val fileExtension: String,
    val fileSizeBytes: Long,
    val bytes: ByteArray
)

object FileStorageHelper {
    private const val CHUNK_RAW_BYTES = 300_000 // ~400 KB Base64 per chunk, well below 1 MB Firestore limit

    val SUPPORTED_EXTENSIONS = setOf(
        "pdf", "jpg", "jpeg", "png", "webp",
        "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "csv"
    )

    val SUPPORTED_MIME_TYPES = arrayOf(
        "application/pdf",
        "image/*",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain",
        "text/csv"
    )

    suspend fun readPickedUri(
        context: Context,
        uri: Uri,
        maxUploadSizeMb: Long
    ): Result<PickedFilePayload> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            var displayName = "document_${System.currentTimeMillis()}"
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) displayName = name
                    }
                }
            }
            val rawMime = resolver.getType(uri) ?: guessMimeFromName(displayName)
            val ext = displayName.substringAfterLast('.', "").lowercase().ifBlank {
                extensionFromMime(rawMime)
            }
            if (ext !in SUPPORTED_EXTENSIONS) {
                return@withContext Result.failure(
                    IllegalArgumentException("Unsupported file type '.$ext'. Supported: PDF, Images, DOC/DOCX, PPT/PPTX, XLS/XLSX, TXT.")
                )
            }
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(IllegalStateException("Unable to read selected file."))
            val maxBytes = maxUploadSizeMb * 1024L * 1024L
            if (bytes.size > maxBytes) {
                return@withContext Result.failure(
                    IllegalArgumentException("File exceeds configured limit of $maxUploadSizeMb MB (${formatFileSize(bytes.size.toLong())}).")
                )
            }
            Result.success(
                PickedFilePayload(
                    fileName = displayName,
                    mimeType = rawMime,
                    fileExtension = ext,
                    fileSizeBytes = bytes.size.toLong(),
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
            chunks.add(Base64.encodeToString(slice, Base64.NO_WRAP))
            offset = end
        }
        return chunks
    }

    fun decodeBase64Chunks(base64Chunks: List<String>): ByteArray {
        val totalParts = base64Chunks.map { Base64.decode(it, Base64.DEFAULT) }
        val totalLen = totalParts.sumOf { it.size }
        val output = ByteArray(totalLen)
        var cursor = 0
        for (part in totalParts) {
            System.arraycopy(part, 0, output, cursor, part.size)
            cursor += part.size
        }
        return output
    }

    suspend fun saveToDownloadsDir(
        context: Context,
        documentId: String,
        fileName: String,
        bytes: ByteArray
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "dorbar_downloads").apply { mkdirs() }
        val safeName = "${documentId}_${fileName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_")}"
        val target = File(dir, safeName)
        FileOutputStream(target).use { it.write(bytes) }
        target
    }

    suspend fun saveToPreviewCache(
        context: Context,
        documentId: String,
        fileExtension: String,
        bytes: ByteArray
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "dorbar_previews").apply { mkdirs() }
        val target = File(dir, "${documentId}_preview.$fileExtension")
        FileOutputStream(target).use { it.write(bytes) }
        target
    }

    suspend fun renderPdfPages(file: File, maxPages: Int = 20): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val count = minOf(renderer.pageCount, maxPages)
            for (i in 0 until count) {
                renderer.openPage(i).use { page ->
                    val width = (page.width * 1.6f).toInt().coerceIn(480, 1440)
                    val height = ((page.height.toFloat() / page.width.coerceAtLeast(1)) * width).toInt()
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bmp)
                }
            }
        } catch (_: Exception) {
            // Return whatever pages rendered cleanly
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
        bitmaps
    }

    fun decodeImageBitmap(bytes: ByteArray): Bitmap? {
        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun openLocalFileExternally(context: Context, file: File, mimeType: String): Result<Unit> {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open document with"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareTextReport(context: Context, fileName: String, content: String): Result<Unit> {
        return try {
            val dir = File(context.cacheDir, "dorbar_previews").apply { mkdirs() }
            val outFile = File(dir, fileName)
            outFile.writeText(content, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export CSV Report"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }

    private fun guessMimeFromName(name: String): String {
        return when (name.substringAfterLast('.', "").lowercase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "csv" -> "text/csv"
            else -> "text/plain"
        }
    }

    private fun extensionFromMime(mime: String): String {
        return when {
            mime.contains("pdf") -> "pdf"
            mime.contains("jpeg") -> "jpg"
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("wordprocessingml") -> "docx"
            mime.contains("msword") -> "doc"
            mime.contains("presentationml") -> "pptx"
            mime.contains("ms-powerpoint") -> "ppt"
            mime.contains("spreadsheetml") -> "xlsx"
            mime.contains("ms-excel") -> "xls"
            mime.contains("csv") -> "csv"
            else -> "txt"
        }
    }
}
