package com.sch.sekolah_mobile_app.ui.screens.izin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.sch.sekolah_mobile_app.data.model.SelectedFile
import java.io.ByteArrayOutputStream
import kotlin.math.max

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

                if (size > 20 * 1024 * 1024) {
                    onError("Ukuran file terlalu besar (maksimal 20 MB).")
                    return@rememberLauncherForActivityResult
                }

                val rawBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (rawBytes == null || rawBytes.isEmpty()) {
                    onError("Gagal membaca isi file.")
                    return@rememberLauncherForActivityResult
                }

                var effectiveMime = if (mimeType != "application/octet-stream") mimeType else {
                    when {
                        lowerName.endsWith(".pdf") -> "application/pdf"
                        lowerName.endsWith(".png") -> "image/png"
                        lowerName.endsWith(".webp") -> "image/webp"
                        else -> "image/jpeg"
                    }
                }

                var finalBytes = rawBytes
                var finalName = displayName

                // Kompres otomatis gambar yang berukuran > 500 KB agar unggahan ringan dan bebas HTTP 413
                if (effectiveMime.startsWith("image/") && rawBytes.size > 500 * 1024) {
                    try {
                        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                        if (decoded != null) {
                            val maxDim = 1280
                            val w = decoded.width
                            val h = decoded.height
                            val longest = max(w, h)
                            val scaled = if (longest > maxDim) {
                                val ratio = maxDim.toFloat() / longest.toFloat()
                                val targetW = (w * ratio).toInt().coerceAtLeast(1)
                                val targetH = (h * ratio).toInt().coerceAtLeast(1)
                                Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                            } else {
                                decoded
                            }
                            val baos = ByteArrayOutputStream()
                            scaled.compress(Bitmap.CompressFormat.JPEG, 82, baos)
                            val compressedBytes = baos.toByteArray()
                            if (compressedBytes.isNotEmpty() && compressedBytes.size < rawBytes.size) {
                                finalBytes = compressedBytes
                                effectiveMime = "image/jpeg"
                                val baseName = displayName.substringBeforeLast('.', displayName)
                                finalName = "$baseName.jpg"
                            }
                        }
                    } catch (_: Exception) {}
                }

                onFileSelected(
                    SelectedFile(
                        name = finalName,
                        mimeType = effectiveMime,
                        bytes = finalBytes,
                        sizeBytes = finalBytes.size.toLong()
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
