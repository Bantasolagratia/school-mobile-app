package com.sch.sekolah_mobile_app.data.security

actual object PlatformApkDownloader {
    actual suspend fun download(
        url: String,
        destinationPath: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Boolean = false
}
