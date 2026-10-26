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
}

