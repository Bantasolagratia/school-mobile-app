package com.sch.sekolah_mobile_app.data.security

expect object PlatformCrypto {
    fun verifyRsaSignature(publicKeyB64: String, data: ByteArray, signatureB64: String): Boolean
    fun decryptAesGcm(secretKeyB64: String, ivB64: String, encryptedPayloadB64: String): String
    fun encryptAesGcm(secretKeyB64: String, plaintext: String): Pair<String, String>
    fun hmacSha256B64(secret: String, data: String): String
}
