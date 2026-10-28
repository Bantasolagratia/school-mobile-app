package com.sch.sekolah_mobile_app.data.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

actual object PlatformApkDownloader {

    actual suspend fun download(
        url: String,
        destinationPath: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        var currentUrl = url
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        val tempFile = File("$destinationPath.tmp")
        val targetFile = File(destinationPath)

        try {
            if (tempFile.exists()) tempFile.delete()
            if (targetFile.exists()) targetFile.delete()

            val maxRedirects = 8
            var redirectCount = 0
            var cookies: String? = null

            while (redirectCount < maxRedirects) {
                val parsedUrl = URL(currentUrl)
                connection = parsedUrl.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 20_000
                connection.readTimeout = 60_000
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                )
                if (!cookies.isNullOrBlank()) {
                    connection.setRequestProperty("Cookie", cookies)
                }

                connection.connect()

                val headerCookies = connection.headerFields["Set-Cookie"]
                if (!headerCookies.isNullOrEmpty()) {
                    cookies = headerCookies.joinToString("; ") { it.split(";")[0] }
                }

                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                            location
                        } else {
                            URL(parsedUrl, location).toString()
                        }
                        connection.disconnect()
                        redirectCount++
                        continue
                    }
                }

                if (responseCode !in 200..299) {
                    connection.disconnect()
                    return@withContext false
                }

                break
            }

            val totalBytes = connection?.contentLengthLong ?: -1L
            inputStream = connection?.inputStream ?: return@withContext false
            outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(16 * 1024)
            var downloadedBytes = 0L
            var read: Int

            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                downloadedBytes += read

                val progress = if (totalBytes > 0) {
                    (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                } else {
                    -1f // indeterminate
                }
                onProgress(progress, downloadedBytes, totalBytes)
            }

            outputStream.flush()
            outputStream.close()
            outputStream = null
            inputStream.close()
            inputStream = null

            // Validasi file hasil unduhan tidak kosong
            if (tempFile.exists() && tempFile.length() > 1024L) {
                if (tempFile.renameTo(targetFile)) {
                    onProgress(1f, downloadedBytes, downloadedBytes)
                    return@withContext true
                }
            }

            return@withContext false
        } catch (e: Exception) {
            try { tempFile.delete() } catch (_: Exception) {}
            return@withContext false
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
            try { connection?.disconnect() } catch (_: Exception) {}
        }
    }
}
