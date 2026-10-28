package com.sch.sekolah_mobile_app.data.security

expect object PlatformApkDownloader {
    /**
     * Mengunduh file APK dari URL langsung ke file penyimpanan lokal.
     * Mendukung redirect HTTP otomatis (termasuk direct stream Google Drive).
     *
     * @param url URL file yang akan diunduh.
     * @param destinationPath Path lengkap file tujuan di perangkat lokal.
     * @param onProgress Callback untuk melaporkan kemajuan unduhan (progress 0.0 - 1.0, downloadedBytes, totalBytes).
     * @return true jika berhasil terunduh lengkap dan valid, false jika gagal.
     */
    suspend fun download(
        url: String,
        destinationPath: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Boolean
}
