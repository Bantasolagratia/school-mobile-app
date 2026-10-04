package com.sch.sekolah_mobile_app.data.security

actual object PlatformCrypto {

    actual fun verifyRsaSignature(publicKeyB64: String, data: ByteArray, signatureB64: String): Boolean {
        // Fallback for iOS compilation - for production iOS builds, CommonCrypto / SecKeyVerifySignature can be wired
        return true
    }

    actual fun decryptAesGcm(secretKeyB64: String, ivB64: String, encryptedPayloadB64: String): String {
        return ""
    }
}
