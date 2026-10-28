package com.sch.sekolah_mobile_app.data.security

expect object PlatformAppInstaller {
    /**
     * Memeriksa apakah aplikasi memiliki izin memasang paket (khusus Android O / API 26+).
     */
    fun canInstallPackages(): Boolean

    /**
     * Membuka halaman pengaturan sistem untuk mengaktifkan izin "Install unknown apps".
     */
    fun openInstallPermissionSettings()

    /**
     * Memulai proses instalasi file APK via PackageInstaller sistem operasi.
     * @param filePath Path lokal absolut dari file APK hasil unduhan.
     */
    fun installApk(filePath: String): Boolean

    /**
     * Mendapatkan direktori cache lokal untuk menyimpan file unduhan APK sementara.
     */
    fun getDownloadDirectory(): String
}
