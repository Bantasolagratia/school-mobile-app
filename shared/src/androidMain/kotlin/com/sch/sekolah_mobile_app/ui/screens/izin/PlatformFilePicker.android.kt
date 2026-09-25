package com.sch.sekolah_mobile_app.ui.screens.izin

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.sch.sekolah_mobile_app.data.model.SelectedFile

@Composable
actual fun rememberPlatformFilePicker(
    onFileSelected: (SelectedFile) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                var displayName = "dokumen"
                var size = 0L

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            size = cursor.getLong(sizeIndex)
                        }
                    }
                }

                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                val lowerName = displayName.lowercase()
                val isValidType = mimeType.startsWith("image/") || mimeType == "application/pdf" ||
                        lowerName.endsWith(".pdf") || lowerName.endsWith(".jpg") ||
                        lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp")

                if (!isValidType) {
                    onError("Format file tidak didukung. Hanya file Gambar (JPG/PNG/WEBP) atau PDF yang diizinkan.")
                    return@rememberLauncherForActivityResult
                }

                if (size > 10 * 1024 * 1024) {
                    onError("Ukuran file terlalu besar (maksimal 10 MB).")
                    return@rememberLauncherForActivityResult
                }

                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    onError("Gagal membaca isi file.")
                    return@rememberLauncherForActivityResult
                }

                val effectiveMime = if (mimeType != "application/octet-stream") mimeType else {
                    when {
                        lowerName.endsWith(".pdf") -> "application/pdf"
                        lowerName.endsWith(".png") -> "image/png"
                        lowerName.endsWith(".webp") -> "image/webp"
                        else -> "image/jpeg"
                    }
                }

                onFileSelected(
                    SelectedFile(
                        name = displayName,
                        mimeType = effectiveMime,
                        bytes = bytes,
                        sizeBytes = bytes.size.toLong()
                    )
                )
            } catch (e: Exception) {
                onError("Gagal memproses file: ${e.message}")
            }
        }
    }

    return {
        launcher.launch(arrayOf("image/*", "application/pdf"))
    }
}
