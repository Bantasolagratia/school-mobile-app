package com.sch.sekolah_mobile_app.data.security

object ApkUrlCryptoHelper {
    // Derived Base64 AES-256 key from SHA-256("SourceAPKSekolah25143123!")
    // Key: SourceAPKSekolah25143123!
    private const val APK_URL_SECRET_KEY_B64 = "Hrcxa4sKRbFdxrTHqzKxjrQel/9jVg6p4/V8yikJeXA="

    /**
     * Mendekripsi URL APK jika terenkripsi dengan format "iv:ciphertext"
     * Jika format plain-text (dimulai dengan http:// atau https://), langsung dikembalikan apa adanya.
     */
    fun decryptApkUrl(rawOrEncryptedUrl: String): String {
        val trimmed = rawOrEncryptedUrl.trim()
        if (trimmed.isBlank()) return ""
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }

        return try {
            if (trimmed.contains(":")) {
                val parts = trimmed.split(":", limit = 2)
                val ivB64 = parts[0].trim()
                val cipherB64 = parts[1].trim()
                val decrypted = PlatformCrypto.decryptAesGcm(APK_URL_SECRET_KEY_B64, ivB64, cipherB64)
                if (decrypted.isNotBlank()) decrypted else trimmed
            } else {
                trimmed
            }
        } catch (_: Exception) {
            trimmed
        }
    }

    /**
     * Memeriksa apakah URL merupakan tautan folder Google Drive (bukan file langsung).
     */
    fun isGoogleDriveFolder(url: String): Boolean {
        return url.contains("/drive/folders/", ignoreCase = true)
    }

    /**
     * Mengonversi URL publik Google Drive file menjadi direct download stream URL.
     * Contoh:
     * - https://drive.google.com/file/d/1ABCXYZ/view?usp=sharing
     *   -> https://drive.google.com/uc?export=download&id=1ABCXYZ
     */
    fun resolveDirectDownloadUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""

        if (trimmed.contains("drive.google.com", ignoreCase = true)) {
            // Cek format /file/d/FILE_ID
            val fileIdRegex = Regex("""/file/d/([a-zA-Z0-9_-]+)""")
            val fileIdMatch = fileIdRegex.find(trimmed)
            if (fileIdMatch != null) {
                val fileId = fileIdMatch.groupValues[1]
                return "https://drive.google.com/uc?export=download&id=$fileId"
            }

            // Cek format ?id=FILE_ID
            val queryIdRegex = Regex("""[?&]id=([a-zA-Z0-9_-]+)""")
            val queryIdMatch = queryIdRegex.find(trimmed)
            if (queryIdMatch != null) {
                val fileId = queryIdMatch.groupValues[1]
                return "https://drive.google.com/uc?export=download&id=$fileId"
            }
        }

        return trimmed
    }
}

